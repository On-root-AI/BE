package com.OnRoot.onroot.domain.examschedule.service;

import com.OnRoot.onroot.domain.examschedule.dto.ExamScheduleResponse;
import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;
import com.OnRoot.onroot.domain.examschedule.repository.ExamScheduleRepository;
import com.OnRoot.onroot.global.client.QNetApiClient;
import com.OnRoot.onroot.global.client.QualExamSchdApiClient;
import com.OnRoot.onroot.global.parser.ExamCodeParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final QualExamSchdApiClient qualExamSchdApiClient;
    private final QNetApiClient qNetApiClient;
    private final ExamCodeParser examCodeParser;

    @Transactional
    public void sync() {
        examScheduleRepository.deleteAll();

        List<ExamSchedule> schedules;
        try {
            Map<String, List<ExamCodeParser.ExamInfo>> examsBySeries = examCodeParser.loadBySeriesName();
            schedules = qNetApiClient.fetchAllWithExamNames(examsBySeries);
        } catch (Exception e) {
            log.error("Q-Net 종목별 일정 로드 실패", e);
            schedules = List.of();
        }

        if (schedules.isEmpty()) {
            log.warn("종목별 일정 없음 → data.go.kr 카테고리 레벨 폴백");
            schedules = qualExamSchdApiClient.fetchAll();
        }
        examScheduleRepository.saveAll(schedules);
        log.info("시험 일정 동기화 완료: {}건 저장", schedules.size());
    }

    @Transactional(readOnly = true)
    public List<ExamScheduleResponse> getAll() {
        return examScheduleRepository.findAll()
                .stream()
                .map(ExamScheduleResponse::from)
                .toList();
    }
}
