package com.OnRoot.onroot.domain.streak.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.OnRoot.onroot.domain.user.entity.User;

class StreakTest {

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
    void recordActivity_setsFirstStreakToOne() {
        Streak streak = Streak.builder()
                .user(createUser())
                .currentStreak(0)
                .lastActivityDate(null)
                .build();

        streak.recordActivity(LocalDate.of(2026, 5, 27));

        assertThat(streak.getCurrentStreak()).isEqualTo(1);
        assertThat(streak.getLastActivityDate()).isEqualTo(LocalDate.of(2026, 5, 27));
    }

    @Test
    void recordActivity_incrementsWhenDaysAreConsecutive() {
        Streak streak = Streak.builder()
                .user(createUser())
                .currentStreak(2)
                .lastActivityDate(LocalDate.of(2026, 5, 27))
                .build();

        streak.recordActivity(LocalDate.of(2026, 5, 28));

        assertThat(streak.getCurrentStreak()).isEqualTo(3);
        assertThat(streak.getLastActivityDate()).isEqualTo(LocalDate.of(2026, 5, 28));
    }

    @Test
    void recordActivity_resetsWhenGapIsLongerThanOneDay() {
        Streak streak = Streak.builder()
                .user(createUser())
                .currentStreak(4)
                .lastActivityDate(LocalDate.of(2026, 5, 27))
                .build();

        streak.recordActivity(LocalDate.of(2026, 5, 31));

        assertThat(streak.getCurrentStreak()).isEqualTo(1);
        assertThat(streak.getLastActivityDate()).isEqualTo(LocalDate.of(2026, 5, 31));
    }
}
