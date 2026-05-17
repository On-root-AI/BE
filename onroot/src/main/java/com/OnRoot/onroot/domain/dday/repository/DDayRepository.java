package com.OnRoot.onroot.domain.dday.repository;

import com.OnRoot.onroot.domain.dday.entity.DDay;
import com.OnRoot.onroot.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DDayRepository extends JpaRepository<DDay, Long> {
    List<DDay> findByUserOrderByTargetDateAsc(User user);
}
