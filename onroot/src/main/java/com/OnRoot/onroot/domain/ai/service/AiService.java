package com.OnRoot.onroot.domain.ai.service;

import com.OnRoot.onroot.domain.ai.dto.AiGenerateRequest;
import com.OnRoot.onroot.domain.ai.dto.AiGenerateResponse;
import com.OnRoot.onroot.domain.ai.entity.AiGenerationLog;
import com.OnRoot.onroot.domain.ai.repository.AiGenerationLogRepository;
import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;
import com.OnRoot.onroot.domain.examschedule.repository.ExamScheduleRepository;
import com.OnRoot.onroot.domain.plan.entity.Plan;
import com.OnRoot.onroot.domain.plan.entity.PlanStatus;
import com.OnRoot.onroot.domain.plan.repository.PlanRepository;
import com.OnRoot.onroot.domain.task.entity.Task;
import com.OnRoot.onroot.domain.task.repository.TaskRepository;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.global.client.GeminiApiClient;
import com.OnRoot.onroot.global.store.ExamSubjectStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final Map<String, String> EXAM_ALIAS = Map.of(
            "정처기", "정보처리기사",
            "정산기", "정보처리산업기사",
            "전기기사", "전기기사",
            "전기산기", "전기산업기사",
            "전기기능사", "전기기능사",
            "산안기", "산업안전기사",
            "소방설비기사", "소방설비기사",
            "건축기사", "건축기사",
            "토목기사", "토목기사"
    );

    private static final Set<String> GENERIC_TOKENS = Set.of(
            "필기", "실기", "시험", "합격", "공부", "준비", "학습",
            "평일", "주말", "하루", "시간", "개월", "안에", "이내", "이하"
    );

    private final ExamScheduleRepository examScheduleRepository;
    private final PlanRepository planRepository;
    private final TaskRepository taskRepository;
    private final AiGenerationLogRepository aiGenerationLogRepository;
    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;
    private final ExamSubjectStore examSubjectStore;

    @Value("${gemini.api.model}")
    private String llmModel;

    @Transactional
    public AiGenerateResponse generate(AiGenerateRequest request, User user) {
        LocalDate targetDate = extractTargetDate(request.userInput());
        int[] hours = extractAvailableHours(request.userInput());
        int weekdayHours = hours[0];
        int weekendHours = hours[1];

        ExamMatch match = resolveExamSchedule(request.userInput(), targetDate);
        ExamSchedule writtenExam = match.writtenExam();
        ExamSchedule practicalExam = match.practicalExam();
        String category = match.keyword();

        if (practicalExam != null) {
            targetDate = practicalExam.getExamDate();
        } else if (writtenExam != null) {
            targetDate = writtenExam.getExamDate();
        }

        log.info("파싱 결과 - 목표날짜: {}, 카테고리: {}, 필기: {}, 실기: {}, 평일: {}h, 주말: {}h",
                targetDate, category,
                writtenExam != null ? writtenExam.getExamName() : "없음",
                practicalExam != null ? practicalExam.getExamName() : "없음",
                weekdayHours, weekendHours);

        String prompt = buildPrompt(request, targetDate, category, writtenExam, practicalExam, weekdayHours, weekendHours);
        String generatedJson = geminiApiClient.generate(prompt);

        JsonNode json;
        try {
            json = objectMapper.readTree(generatedJson);
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패: {}", generatedJson);
            throw new com.OnRoot.onroot.global.exception.ExternalServiceException(
                    "AI 서비스 호출에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        String title = json.path("title").asText();
        JsonNode segmentsNode = json.path("segments");

        Plan plan = planRepository.save(Plan.builder()
                .user(user)
                .writtenExamSchedule(writtenExam)
                .practicalExamSchedule(practicalExam)
                .title(title)
                .category(category)
                .targetDate(targetDate)
                .weekdayHours(weekdayHours)
                .weekendHours(weekendHours)
                .status(PlanStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build());

        List<Task> tasks = expandSegments(plan, segmentsNode, writtenExam, practicalExam, weekdayHours, weekendHours);
        taskRepository.saveAll(tasks);
        log.info("task 생성 완료: {}개", tasks.size());

        AiGenerationLog aiLog = aiGenerationLogRepository.save(AiGenerationLog.builder()
                .user(user)
                .plan(plan)
                .userInput(request.userInput())
                .generatedJson(generatedJson)
                .llmModel(llmModel)
                .createdAt(LocalDateTime.now())
                .build());

        List<AiGenerateResponse.TaskDto> taskDtos = tasks.stream()
                .map(t -> new AiGenerateResponse.TaskDto(t.getId(), t.getTitle(), t.getScheduledDate(), t.getOrderIndex()))
                .toList();

        return new AiGenerateResponse(aiLog.getId(), plan.getId(), title, weekdayHours, weekendHours, taskDtos);
    }

    private record Segment(LocalDate startDate, LocalDate endDate, List<String> weekdayTopics, String weekendTopic) {
        boolean covers(LocalDate date) {
            return !date.isBefore(startDate) && !date.isAfter(endDate);
        }
    }

    private static final List<String> FALLBACK_TOPICS = List.of("이론 복습", "문제 풀이", "오답 노트 정리", "기출 분석", "핵심 개념 정리");

    private List<Task> expandSegments(
            Plan plan, JsonNode segmentsNode,
            ExamSchedule writtenExam, ExamSchedule practicalExam,
            int weekdayHours, int weekendHours) {

        // AI 응답 segments 파싱
        List<Segment> segments = new ArrayList<>();
        for (JsonNode seg : segmentsNode) {
            try {
                LocalDate segStart = LocalDate.parse(seg.path("startDate").asText());
                LocalDate segEnd = LocalDate.parse(seg.path("endDate").asText());
                List<String> weekdayTopics = new ArrayList<>();
                for (JsonNode t : seg.path("weekdayTopics")) weekdayTopics.add(t.asText());
                String weekendTopic = seg.path("weekendTopic").asText("종합 복습");
                if (!weekdayTopics.isEmpty()) {
                    segments.add(new Segment(segStart, segEnd, weekdayTopics, weekendTopic));
                }
            } catch (Exception e) {
                log.warn("세그먼트 파싱 실패, 스킵: {}", e.getMessage());
            }
        }

        // 전체 날짜 범위를 직접 순회 — 세그먼트 누락 방어
        List<Task> tasks = new ArrayList<>();
        int orderIndex = 1;
        LocalDate date = LocalDate.now();
        LocalDate end = plan.getTargetDate();

        while (!date.isAfter(end)) {
            final LocalDate d = date;
            Segment seg = segments.stream().filter(s -> s.covers(d)).findFirst().orElse(null);

            List<String> weekdayTopics = seg != null ? seg.weekdayTopics() : FALLBACK_TOPICS;
            String weekendTopic = seg != null ? seg.weekendTopic() : "전과목 종합 복습";

            String taskTitle = resolveTaskTitle(date, weekdayTopics, weekendTopic,
                    weekdayHours, weekendHours, writtenExam, practicalExam);

            tasks.add(Task.builder()
                    .plan(plan)
                    .title(taskTitle)
                    .scheduledDate(date)
                    .orderIndex(orderIndex++)
                    .build());

            date = date.plusDays(1);
        }

        log.info("총 {}개 task 생성 (세그먼트 {}개)", tasks.size(), segments.size());
        return tasks;
    }

    private String resolveTaskTitle(
            LocalDate date,
            List<String> weekdayTopics, String weekendTopic,
            int weekdayHours, int weekendHours,
            ExamSchedule writtenExam, ExamSchedule practicalExam) {

        // 시험일
        if (writtenExam != null && date.equals(writtenExam.getExamDate())) {
            return writtenExam.getExamName() + " 응시";
        }
        if (practicalExam != null && date.equals(practicalExam.getExamDate())) {
            return practicalExam.getExamName() + " 응시";
        }
        // 원서접수 기간
        if (writtenExam != null && writtenExam.getApplicationStart() != null
                && !date.isBefore(writtenExam.getApplicationStart())
                && !date.isAfter(writtenExam.getApplicationEnd())) {
            return "필기 원서접수";
        }
        if (practicalExam != null && practicalExam.getApplicationStart() != null
                && !date.isBefore(practicalExam.getApplicationStart())
                && !date.isAfter(practicalExam.getApplicationEnd())) {
            return "실기 원서접수";
        }
        // 주말
        if (isWeekend(date)) {
            return weekendTopic + " (" + weekendHours + "시간)";
        }
        // 평일: 요일 기반 순환
        int dayOfWeek = date.getDayOfWeek().getValue() - 1; // 월=0, 화=1, ..., 금=4
        String topic = weekdayTopics.get(dayOfWeek % weekdayTopics.size());
        return topic + " (" + weekdayHours + "시간)";
    }

    private boolean isSpecialDate(LocalDate date, ExamSchedule writtenExam, ExamSchedule practicalExam) {
        if (writtenExam != null && date.equals(writtenExam.getExamDate())) return true;
        if (practicalExam != null && date.equals(practicalExam.getExamDate())) return true;
        if (writtenExam != null && writtenExam.getApplicationStart() != null
                && !date.isBefore(writtenExam.getApplicationStart())
                && !date.isAfter(writtenExam.getApplicationEnd())) return true;
        if (practicalExam != null && practicalExam.getApplicationStart() != null
                && !date.isBefore(practicalExam.getApplicationStart())
                && !date.isAfter(practicalExam.getApplicationEnd())) return true;
        return false;
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private static final long EXAM_MATCH_TOLERANCE_DAYS = 14;

    private ExamMatch resolveExamSchedule(String userInput, LocalDate targetDate) {
        boolean wantsWrittenOnly  = userInput.contains("필기") && !userInput.contains("실기");
        boolean wantsPracticalOnly = userInput.contains("실기") && !userInput.contains("필기");

        String[] tokens = userInput.split("[\\s,./]+");
        for (String token : tokens) {
            if (token.length() < 2) continue;
            if (GENERIC_TOKENS.contains(token)) continue;
            String keyword = EXAM_ALIAS.getOrDefault(token, token);
            List<ExamSchedule> matches = examScheduleRepository
                    .findByExamNameContainingAndExamDateGreaterThanEqualOrderByExamDateAsc(keyword, LocalDate.now());
            if (matches.isEmpty()) continue;

            if (wantsWrittenOnly) {
                // "필기 합격하고싶어" — 필기 일정만
                ExamSchedule writtenExam = matches.stream()
                        .filter(e -> e.getExamName().contains("필기"))
                        .filter(e -> Math.abs(ChronoUnit.DAYS.between(e.getExamDate(), targetDate)) <= EXAM_MATCH_TOLERANCE_DAYS)
                        .min(Comparator.comparingLong(e -> Math.abs(ChronoUnit.DAYS.between(e.getExamDate(), targetDate))))
                        .orElse(null);
                return new ExamMatch(writtenExam, null, keyword);
            }

            if (wantsPracticalOnly) {
                // "실기 합격하고싶어" — 이미 필기 합격, 실기 일정만
                ExamSchedule practicalExam = matches.stream()
                        .filter(e -> e.getExamName().contains("실기"))
                        .filter(e -> Math.abs(ChronoUnit.DAYS.between(e.getExamDate(), targetDate)) <= EXAM_MATCH_TOLERANCE_DAYS)
                        .min(Comparator.comparingLong(e -> Math.abs(ChronoUnit.DAYS.between(e.getExamDate(), targetDate))))
                        .orElse(null);
                return new ExamMatch(null, practicalExam, keyword);
            }

            // 일반 케이스: targetDate ±60일 이내의 실기 → 그 앞 필기
            ExamSchedule practicalExam = matches.stream()
                    .filter(e -> e.getExamName().contains("실기"))
                    .filter(e -> Math.abs(ChronoUnit.DAYS.between(e.getExamDate(), targetDate)) <= EXAM_MATCH_TOLERANCE_DAYS)
                    .min(Comparator.comparingLong(e -> Math.abs(ChronoUnit.DAYS.between(e.getExamDate(), targetDate))))
                    .orElse(null);

            ExamSchedule writtenExam;
            if (practicalExam != null) {
                final LocalDate practicalDate = practicalExam.getExamDate();
                writtenExam = matches.stream()
                        .filter(e -> e.getExamName().contains("필기"))
                        .filter(e -> e.getExamDate().isBefore(practicalDate))
                        .max(Comparator.comparing(ExamSchedule::getExamDate))
                        .orElse(null);
                // 실기는 잡혔으나 앞선 필기가 없으면 → 필기를 봐야 실기가 가능하므로 일반학습계획
                if (writtenExam == null) {
                    return new ExamMatch(null, null, keyword);
                }
            } else {
                writtenExam = matches.stream()
                        .filter(e -> e.getExamName().contains("필기"))
                        .filter(e -> Math.abs(ChronoUnit.DAYS.between(e.getExamDate(), targetDate)) <= EXAM_MATCH_TOLERANCE_DAYS)
                        .min(Comparator.comparingLong(e -> Math.abs(ChronoUnit.DAYS.between(e.getExamDate(), targetDate))))
                        .orElse(null);
            }

            return new ExamMatch(writtenExam, practicalExam, keyword);
        }
        String fallbackCategory = tokens.length > 0
                ? EXAM_ALIAS.getOrDefault(tokens[0], tokens[0])
                : "기타";
        return new ExamMatch(null, null, fallbackCategory);
    }

    private int[] extractAvailableHours(String userInput) {
        int weekday = 2;
        int weekend = 2;

        Matcher m = Pattern.compile("평일[^\\d]*(\\d+)\\s*시간").matcher(userInput);
        if (m.find()) weekday = Integer.parseInt(m.group(1));

        m = Pattern.compile("주말[^\\d]*(\\d+)\\s*시간").matcher(userInput);
        if (m.find()) weekend = Integer.parseInt(m.group(1));

        if (!userInput.contains("평일") && !userInput.contains("주말")) {
            m = Pattern.compile("하루[^\\d]*(\\d+)\\s*시간").matcher(userInput);
            if (m.find()) {
                weekday = Integer.parseInt(m.group(1));
                weekend = weekday;
            }
        }

        return new int[]{weekday, weekend};
    }

    private LocalDate extractTargetDate(String userInput) {
        Matcher monthMatcher = Pattern.compile("(\\d+)\\s*(개월|달)").matcher(userInput);
        if (monthMatcher.find()) {
            return LocalDate.now().plusMonths(Long.parseLong(monthMatcher.group(1)));
        }
        Matcher yearMatcher = Pattern.compile("(\\d+)\\s*년").matcher(userInput);
        if (yearMatcher.find()) {
            return LocalDate.now().plusYears(Long.parseLong(yearMatcher.group(1)));
        }
        log.info("목표 기간 미입력 — 기본값 3개월 적용");
        return LocalDate.now().plusMonths(3);
    }

    private String buildPrompt(AiGenerateRequest request, LocalDate targetDate, String category,
                               ExamSchedule writtenExam, ExamSchedule practicalExam,
                               int weekdayHours, int weekendHours) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 학습 플래너입니다.\n");
        sb.append("오늘 날짜: ").append(LocalDate.now()).append("\n");
        sb.append("카테고리: ").append(category).append("\n");
        sb.append("사용자 요청: ").append(request.userInput()).append("\n");
        sb.append("가용 학습 시간: 평일 ").append(weekdayHours).append("시간, 주말 ").append(weekendHours).append("시간\n");
        sb.append("\n[학습 계획 기간]\n");
        sb.append("시작일: ").append(LocalDate.now()).append("\n");
        sb.append("종료일: ").append(targetDate).append("\n");

        boolean hasBothExams = writtenExam != null && practicalExam != null;
        boolean hasAnyExam = writtenExam != null || practicalExam != null;

        if (hasBothExams) {
            sb.append("\n[시험 일정]\n");
            sb.append("  [1단계: 필기]\n");
            sb.append("  - 시험명: ").append(writtenExam.getExamName()).append("\n");
            sb.append("  - 시험일: ").append(writtenExam.getExamDate()).append("\n");
            if (writtenExam.getApplicationStart() != null) {
                sb.append("  - 원서접수: ").append(writtenExam.getApplicationStart())
                        .append(" ~ ").append(writtenExam.getApplicationEnd()).append("\n");
            }
            sb.append("  [2단계: 실기]\n");
            sb.append("  - 시험명: ").append(practicalExam.getExamName()).append("\n");
            sb.append("  - 시험일: ").append(practicalExam.getExamDate()).append("\n");
            if (practicalExam.getApplicationStart() != null) {
                sb.append("  - 원서접수: ").append(practicalExam.getApplicationStart())
                        .append(" ~ ").append(practicalExam.getApplicationEnd()).append("\n");
            }
        } else if (writtenExam != null) {
            sb.append("\n[시험 일정]\n");
            sb.append("  - 시험명: ").append(writtenExam.getExamName()).append("\n");
            sb.append("  - 시험일: ").append(writtenExam.getExamDate()).append("\n");
            if (writtenExam.getApplicationStart() != null) {
                sb.append("  - 원서접수: ").append(writtenExam.getApplicationStart())
                        .append(" ~ ").append(writtenExam.getApplicationEnd()).append("\n");
            }
        } else if (practicalExam != null) {
            sb.append("\n[시험 일정]\n");
            sb.append("  - 시험명: ").append(practicalExam.getExamName()).append("\n");
            sb.append("  - 시험일: ").append(practicalExam.getExamDate()).append("\n");
            if (practicalExam.getApplicationStart() != null) {
                sb.append("  - 원서접수: ").append(practicalExam.getApplicationStart())
                        .append(" ~ ").append(practicalExam.getApplicationEnd()).append("\n");
            }
        }

        sb.append("\n[지시사항]\n");
        sb.append("- 학습 기간을 여러 세그먼트(구간)로 나눌 것. 각 세그먼트는 특정 주제에 집중하는 연속된 날짜 범위\n");
        sb.append("- 세그먼트들의 startDate~endDate가 시작일부터 종료일까지 빠짐없이 이어져야 함 (날짜 겹침/공백 없음)\n");
        sb.append("- 각 세그먼트에 weekdayTopics (평일 5개, 각 요일별 서로 다른 소주제)와 weekendTopic (주말 복습 주제) 제공\n");
        sb.append("- weekdayTopics 5개는 각각 월/화/수/목/금에 해당하는 서로 다른 소주제여야 함\n");
        sb.append("- 시험일, 원서접수 기간은 서버에서 자동 처리하므로 세그먼트에 자유롭게 포함해도 됨\n");

        ExamSubjectStore.ExamSubjects subjects = examSubjectStore.findByName(category).orElse(null);

        if (hasBothExams) {
            sb.append("- 필기 준비 구간(오늘~").append(writtenExam.getExamDate())
              .append("): ").append(category).append(" 필기 과목을 세그먼트별로 배분\n");
            if (subjects != null && !subjects.written().isEmpty()) {
                sb.append("  [필기 과목] ").append(String.join(" → ", subjects.written())).append("\n");
            }
            sb.append("- 실기 준비 구간(").append(writtenExam.getExamDate().plusDays(1)).append("~")
              .append(practicalExam.getExamDate()).append("): 실기 과목별 세그먼트 구성\n");
            if (subjects != null && !subjects.practical().isEmpty()) {
                sb.append("  [실기 과목] ").append(String.join(" → ", subjects.practical())).append("\n");
            }
        } else if (writtenExam != null) {
            sb.append("- 필기 준비 구간: ").append(category).append(" 필기 과목별 세그먼트 구성\n");
            if (subjects != null && !subjects.written().isEmpty()) {
                sb.append("  [필기 과목] ").append(String.join(" → ", subjects.written())).append("\n");
            }
        } else if (practicalExam != null) {
            sb.append("- 실기 준비 구간: 실기 과목별 세그먼트 구성\n");
            if (subjects != null && !subjects.practical().isEmpty()) {
                sb.append("  [실기 과목] ").append(String.join(" → ", subjects.practical())).append("\n");
            }
        }

        if (!hasAnyExam) {
            sb.append("- 시험 일정 정보가 없으므로 임의로 시험 날짜를 추측하지 말 것\n");
        }

        sb.append("- 반드시 다음 JSON 형식으로만 응답할 것:\n");
        sb.append("{\n");
        sb.append("  \"title\": \"계획 제목\",\n");
        sb.append("  \"segments\": [\n");
        sb.append("    {\n");
        sb.append("      \"startDate\": \"YYYY-MM-DD\",\n");
        sb.append("      \"endDate\": \"YYYY-MM-DD\",\n");
        sb.append("      \"weekdayTopics\": [\"월요일 소주제\", \"화요일 소주제\", \"수요일 소주제\", \"목요일 소주제\", \"금요일 소주제\"],\n");
        sb.append("      \"weekendTopic\": \"주말 복습 주제\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}");

        return sb.toString();
    }

    private record ExamMatch(ExamSchedule writtenExam, ExamSchedule practicalExam, String keyword) {}
}
