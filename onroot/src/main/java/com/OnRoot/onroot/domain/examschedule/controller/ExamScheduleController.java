package com.OnRoot.onroot.domain.examschedule.controller;

import com.OnRoot.onroot.domain.examschedule.dto.ExamScheduleResponse;
import com.OnRoot.onroot.domain.examschedule.service.ExamScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exam-schedules")
@RequiredArgsConstructor
public class ExamScheduleController {

    private final ExamScheduleService examScheduleService;

    @GetMapping
    public List<ExamScheduleResponse> getAll() {
        return examScheduleService.getAll();
    }
}
