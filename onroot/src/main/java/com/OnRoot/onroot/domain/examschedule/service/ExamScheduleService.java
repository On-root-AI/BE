package com.OnRoot.onroot.domain.examschedule.service;

import com.OnRoot.onroot.domain.examschedule.dto.ExamScheduleResponse;
import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;
import com.OnRoot.onroot.domain.examschedule.repository.ExamScheduleRepository;
import com.OnRoot.onroot.global.client.QNetApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final QNetApiClient qNetApiClient;

    @Transactional
    public void sync() {
        List<ExamSchedule> fetched = qNetApiClient.fetchAll();
        for (ExamSchedule schedule : fetched) {
            if (!examScheduleRepository.existsByExamNameAndExamDate(schedule.getExamName(), schedule.getExamDate())) {
                examScheduleRepository.save(schedule);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ExamScheduleResponse> getAll() {
        return examScheduleRepository.findAll()
                .stream()
                .map(ExamScheduleResponse::from)
                .toList();
    }
}
