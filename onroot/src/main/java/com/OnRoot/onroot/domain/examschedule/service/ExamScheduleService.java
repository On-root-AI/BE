package com.OnRoot.onroot.domain.examschedule.service;

import com.OnRoot.onroot.domain.examschedule.dto.ExamScheduleResponse;
import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;
import com.OnRoot.onroot.domain.examschedule.repository.ExamScheduleRepository;
import com.OnRoot.onroot.domain.plan.repository.PlanRepository;
import com.OnRoot.onroot.global.client.QNetApiClient;
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
    private final PlanRepository planRepository;
    private final QNetApiClient qNetApiClient;
    private final ExamCodeParser examCodeParser;

    @Transactional
    public void sync() {
        Map<String, List<ExamCodeParser.ExamInfo>> examsBySeries = examCodeParser.loadBySeriesName();
        List<ExamSchedule> schedules = qNetApiClient.fetchAllWithExamNames(examsBySeries);

        planRepository.clearExamScheduleReferences();
        examScheduleRepository.deleteAll();
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
