package com.OnRoot.onroot.domain.task.repository;

import java.time.LocalDateTime;
import com.OnRoot.onroot.domain.plan.entity.Plan;
import com.OnRoot.onroot.domain.task.entity.Task;
import com.OnRoot.onroot.domain.user.entity.User;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByPlanOrderByOrderIndexAsc(Plan plan);

    Optional<Task> findByIdAndPlan(Long id, Plan plan);

    @Query("select t.completedAt from Task t where t.plan.user = :user and t.completedAt is not null order by t.completedAt desc")
    List<LocalDateTime> findCompletedAtByUserOrderByCompletedAtDesc(@Param("user") User user);
}
