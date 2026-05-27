package com.OnRoot.onroot.domain.streak.entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.OnRoot.onroot.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "STREAK")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Streak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    public void sync(int currentStreak, LocalDate lastActivityDate) {
        this.currentStreak = currentStreak;
        this.lastActivityDate = lastActivityDate;
    }

    public void recordActivity(LocalDate activityDate) {
        if (activityDate == null) {
            return;
        }

        if (lastActivityDate == null) {
            currentStreak = 1;
            lastActivityDate = activityDate;
            return;
        }

        long daysBetween = ChronoUnit.DAYS.between(lastActivityDate, activityDate);

        if (daysBetween < 0) {
            return;
        }

        if (daysBetween == 0) {
            return;
        }

        if (daysBetween == 1) {
            currentStreak += 1;
        } else {
            currentStreak = 1;
        }

        lastActivityDate = activityDate;
    }
}
