package com.OnRoot.onroot.global.config;

import com.OnRoot.onroot.domain.examschedule.service.ExamScheduleService;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ExamScheduleService examScheduleService;
    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedDummyUser();
        log.info("Q-Net 시험 일정 동기화 시작");
        examScheduleService.sync();
        log.info("Q-Net 시험 일정 동기화 완료");
    }

    private void seedDummyUser() {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder()
                    .email("dummy@onroot.com")
                    .passwordHash("dummy")
                    .nickname("테스트유저")
                    .provider("local")
                    .createdAt(LocalDateTime.now())
                    .build());
            log.info("더미 유저 생성 완료");
        }
    }
}
