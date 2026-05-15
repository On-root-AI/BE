package com.OnRoot.onroot.global.config;

import com.OnRoot.onroot.domain.examschedule.service.ExamScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ExamScheduleService examScheduleService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Q-Net 시험 일정 동기화 시작");
        examScheduleService.sync();
        log.info("Q-Net 시험 일정 동기화 완료");
    }
}
