package com.OnRoot.onroot.domain.task.repository;

import com.OnRoot.onroot.domain.plan.entity.Plan;
import com.OnRoot.onroot.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByPlanOrderByOrderIndexAsc(Plan plan);

    Optional<Task> findByIdAndPlan(Long id, Plan plan);

    void deleteByPlan(Plan plan);
}
