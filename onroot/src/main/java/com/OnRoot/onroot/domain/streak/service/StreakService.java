package com.OnRoot.onroot.domain.streak.service;

import com.OnRoot.onroot.domain.streak.dto.StreakResponse;
import com.OnRoot.onroot.domain.streak.entity.Streak;
import com.OnRoot.onroot.domain.streak.repository.StreakRepository;
import com.OnRoot.onroot.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final StreakRepository streakRepository;

    @Transactional(readOnly = true)
    public StreakResponse getStreak(User user) {
        Streak streak = streakRepository.findByUser(user)
                .orElse(Streak.builder().user(user).currentStreak(0).lastActivityDate(null).build());
        return StreakResponse.from(streak);
    }
}
