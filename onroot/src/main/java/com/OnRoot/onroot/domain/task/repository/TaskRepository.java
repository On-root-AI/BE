package com.OnRoot.onroot.domain.task.repository;

import com.OnRoot.onroot.domain.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
