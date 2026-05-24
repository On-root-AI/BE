package com.OnRoot.onroot.global.config;

import com.OnRoot.onroot.domain.examschedule.service.ExamScheduleService;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.domain.user.entity.UserRole;
import com.OnRoot.onroot.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ExamScheduleService examScheduleService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdminUser();
        seedDummyUser();
        log.info("Q-Net 시험 일정 동기화 시작");
        examScheduleService.sync();
        log.info("Q-Net 시험 일정 동기화 완료");
    }

    private void seedAdminUser() {
        String adminEmail = "admin@onroot.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            userRepository.save(User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("onroot-admin"))
                    .nickname("관리자")
                    .provider("local")
                    .role(UserRole.ADMIN)
                    .createdAt(LocalDateTime.now())
                    .build());
            log.info("어드민 계정 생성 완료 - email: {}", adminEmail);
        }
    }

    private void seedDummyUser() {
        String testEmail = "test@onroot.com";
        if (userRepository.findByEmail(testEmail).isEmpty()) {
            userRepository.save(User.builder()
                    .email(testEmail)
                    .passwordHash(passwordEncoder.encode("onroot1234"))
                    .nickname("테스트관리자")
                    .provider("local")
                    .createdAt(LocalDateTime.now())
                    .build());
            log.info("테스트 계정 생성 완료 - email: {}", testEmail);
        }
    }
}
