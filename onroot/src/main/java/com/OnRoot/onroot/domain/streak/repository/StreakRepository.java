package com.OnRoot.onroot.domain.streak.repository;

import com.OnRoot.onroot.domain.streak.entity.Streak;
import com.OnRoot.onroot.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StreakRepository extends JpaRepository<Streak, Long> {
    Optional<Streak> findByUser(User user);
}
