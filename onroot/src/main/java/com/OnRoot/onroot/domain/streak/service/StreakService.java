package com.OnRoot.onroot.domain.streak.service;

import com.OnRoot.onroot.domain.streak.dto.StreakResponse;
import com.OnRoot.onroot.domain.streak.entity.Streak;
import com.OnRoot.onroot.domain.streak.repository.StreakRepository;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final StreakRepository streakRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public StreakResponse getStreak() {
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("더미 유저가 없습니다."));
        Streak streak = streakRepository.findByUser(user)
                .orElse(Streak.builder().user(user).currentStreak(0).lastActivityDate(null).build());
        return StreakResponse.from(streak);
    }
}
