package com.OnRoot.onroot.domain.examschedule.service;

import com.OnRoot.onroot.domain.examschedule.dto.ExamScheduleResponse;
import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;
import com.OnRoot.onroot.domain.examschedule.repository.ExamScheduleRepository;
import com.OnRoot.onroot.domain.plan.repository.PlanRepository;
import com.OnRoot.onroot.global.client.QNetApiClient;
import com.OnRoot.onroot.global.parser.ExamCodeParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final PlanRepository planRepository;
    private final QNetApiClient qNetApiClient;
    private final ExamCodeParser examCodeParser;

    @Async("syncExecutor")
    @Transactional
    public void sync() {
        if (examScheduleRepository.count() > 0) {
            log.info("exam_schedule 데이터 존재 — Q-Net 동기화 skip");
            return;
        }
        Map<String, List<ExamCodeParser.ExamInfo>> examsBySeries = examCodeParser.loadBySeriesName();

        List<ExamSchedule> engineers = qNetApiClient.fetchEngineers(examsBySeries);
        List<ExamSchedule> professionals = qNetApiClient.fetchProfessionals(examsBySeries);
        List<ExamSchedule> functional = qNetApiClient.fetchFunctional(examsBySeries);
        List<ExamSchedule> industrial = qNetApiClient.fetchIndustrialEngineers(examsBySeries);

        List<ExamSchedule> all = new ArrayList<>();
        all.addAll(engineers);
        all.addAll(professionals);
        all.addAll(functional);
        all.addAll(industrial);

        if (all.isEmpty()) {
            log.warn("시험 일정 fetch 결과 없음, 기존 데이터 유지");
            return;
        }

        planRepository.clearExamScheduleReferences();
        examScheduleRepository.deleteAll();
        examScheduleRepository.saveAll(all);
        log.info("시험 일정 동기화 완료: 기사 {}건, 기술사 {}건, 기능사 {}건, 산업기사 {}건 저장",
                engineers.size(), professionals.size(), functional.size(), industrial.size());
    }

    @Async("syncExecutor")
    @Transactional
    public void syncEngineers() {
        Map<String, List<ExamCodeParser.ExamInfo>> examsBySeries = examCodeParser.loadBySeriesName();
        List<ExamSchedule> engineers = qNetApiClient.fetchEngineers(examsBySeries);
        if (engineers.isEmpty()) {
            log.warn("기사 시험 일정 fetch 결과 없음");
            return;
        }
        Set<String> subjects = engineers.stream()
                .map(ExamSchedule::getSubject)
                .collect(Collectors.toSet());
        examScheduleRepository.deleteBySubjectIn(subjects);
        examScheduleRepository.saveAll(engineers);
        log.info("기사 시험 일정 재동기화 완료: {}건", engineers.size());
    }

    @Transactional(readOnly = true)
    public List<ExamScheduleResponse> getAll() {
        return examScheduleRepository.findAll()
                .stream()
                .map(ExamScheduleResponse::from)
                .toList();
    }
}
