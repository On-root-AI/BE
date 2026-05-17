package com.OnRoot.onroot.domain.task.service;

import com.OnRoot.onroot.domain.plan.entity.Plan;
import com.OnRoot.onroot.domain.plan.repository.PlanRepository;
import com.OnRoot.onroot.domain.task.dto.TaskCreateRequest;
import com.OnRoot.onroot.domain.task.dto.TaskResponse;
import com.OnRoot.onroot.domain.task.dto.TaskUpdateRequest;
import com.OnRoot.onroot.domain.task.entity.Task;
import com.OnRoot.onroot.domain.task.repository.TaskRepository;
import com.OnRoot.onroot.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final PlanRepository planRepository;

    @Transactional
    public TaskResponse createTask(Long planId, TaskCreateRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("계획을 찾을 수 없습니다."));
        Task task = Task.builder()
                .plan(plan)
                .title(request.title())
                .scheduledDate(request.scheduledDate())
                .orderIndex(request.orderIndex())
                .build();
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("계획을 찾을 수 없습니다."));
        return taskRepository.findByPlanOrderByOrderIndexAsc(plan).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public TaskResponse updateTask(Long planId, Long taskId, TaskUpdateRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("계획을 찾을 수 없습니다."));
        Task task = taskRepository.findByIdAndPlan(taskId, plan)
                .orElseThrow(() -> new NotFoundException("할일을 찾을 수 없습니다."));
        task.update(request.title(), request.scheduledDate(), request.orderIndex());
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse completeTask(Long planId, Long taskId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("계획을 찾을 수 없습니다."));
        Task task = taskRepository.findByIdAndPlan(taskId, plan)
                .orElseThrow(() -> new NotFoundException("할일을 찾을 수 없습니다."));
        task.complete(LocalDateTime.now());
        return TaskResponse.from(task);
    }

    @Transactional
    public void deleteTask(Long planId, Long taskId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("계획을 찾을 수 없습니다."));
        Task task = taskRepository.findByIdAndPlan(taskId, plan)
                .orElseThrow(() -> new NotFoundException("할일을 찾을 수 없습니다."));
        taskRepository.delete(task);
    }
}
