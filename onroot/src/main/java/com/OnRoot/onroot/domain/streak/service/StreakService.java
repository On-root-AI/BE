package com.OnRoot.onroot.domain.streak.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.OnRoot.onroot.domain.streak.dto.StreakResponse;
import com.OnRoot.onroot.domain.streak.entity.Streak;
import com.OnRoot.onroot.domain.streak.repository.StreakRepository;
import com.OnRoot.onroot.domain.task.repository.TaskRepository;
import com.OnRoot.onroot.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final StreakRepository streakRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public StreakResponse getStreak(User user) {
        syncStreak(user);
        Streak streak = streakRepository.findByUser(user)
                .orElse(Streak.builder().user(user).currentStreak(0).lastActivityDate(null).build());
        return StreakResponse.from(streak);
    }

    @Transactional
    public void syncStreak(User user) {
        List<LocalDateTime> completedAtList = taskRepository.findCompletedAtByUserOrderByCompletedAtDesc(user);
        Set<LocalDate> completedDates = new LinkedHashSet<>();
        for (LocalDateTime completedAt : completedAtList) {
            completedDates.add(completedAt.toLocalDate());
        }

        Streak streak = streakRepository.findByUser(user)
                .orElseGet(() -> streakRepository.save(
                        Streak.builder()
                                .user(user)
                                .currentStreak(0)
                                .lastActivityDate(null)
                                .build()
                ));

        if (completedDates.isEmpty()) {
            streak.sync(0, null);
            return;
        }

        int currentStreak = 1;
        LocalDate lastActivityDate = completedDates.iterator().next();
        LocalDate cursor = lastActivityDate;

        for (LocalDate completedDate : completedDates) {
            if (completedDate.equals(lastActivityDate)) {
                continue;
            }

            long daysBetween = ChronoUnit.DAYS.between(completedDate, cursor);
            if (daysBetween == 1) {
                currentStreak += 1;
                cursor = completedDate;
            } else {
                break;
            }
        }

        streak.sync(currentStreak, lastActivityDate);
    }
}
