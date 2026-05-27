package com.OnRoot.onroot.domain.streak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.OnRoot.onroot.domain.streak.entity.Streak;
import com.OnRoot.onroot.domain.streak.repository.StreakRepository;
import com.OnRoot.onroot.domain.task.repository.TaskRepository;
import com.OnRoot.onroot.domain.user.entity.User;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private StreakRepository streakRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private StreakService streakService;

    private User createUser() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("hash")
                .nickname("tester")
                .provider("local")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void syncStreak_resetsToOneWhenLatestDayIsIsolatedAfterGap() {
        User user = createUser();
        Streak streak = Streak.builder()
                .user(user)
                .currentStreak(5)
                .lastActivityDate(LocalDate.of(2026, 5, 20))
                .build();

        when(streakRepository.findByUser(user)).thenReturn(Optional.of(streak));
        when(taskRepository.findCompletedAtByUserOrderByCompletedAtDesc(user)).thenReturn(List.of(
                LocalDateTime.of(2026, 5, 27, 10, 0),
                LocalDateTime.of(2026, 5, 25, 9, 0),
                LocalDateTime.of(2026, 5, 24, 8, 0)
        ));

        streakService.syncStreak(user);

        assertThat(streak.getCurrentStreak()).isEqualTo(1);
        assertThat(streak.getLastActivityDate()).isEqualTo(LocalDate.of(2026, 5, 27));
    }

    @Test
    void syncStreak_countsConsecutiveDatesAcrossMultipleTasksSameDay() {
        User user = createUser();
        Streak streak = Streak.builder()
                .user(user)
                .currentStreak(1)
                .lastActivityDate(LocalDate.of(2026, 5, 27))
                .build();

        when(streakRepository.findByUser(user)).thenReturn(Optional.of(streak));
        when(taskRepository.findCompletedAtByUserOrderByCompletedAtDesc(user)).thenReturn(List.of(
                LocalDateTime.of(2026, 5, 28, 18, 0),
                LocalDateTime.of(2026, 5, 28, 9, 0),
                LocalDateTime.of(2026, 5, 27, 10, 0),
                LocalDateTime.of(2026, 5, 26, 8, 0)
        ));

        streakService.syncStreak(user);

        assertThat(streak.getCurrentStreak()).isEqualTo(3);
        assertThat(streak.getLastActivityDate()).isEqualTo(LocalDate.of(2026, 5, 28));
    }
}