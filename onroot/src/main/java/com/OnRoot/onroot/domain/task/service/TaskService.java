package com.OnRoot.onroot.domain.task.service;

import com.OnRoot.onroot.domain.plan.entity.Plan;
import com.OnRoot.onroot.domain.plan.repository.PlanRepository;
import com.OnRoot.onroot.domain.task.dto.TaskCreateRequest;
import com.OnRoot.onroot.domain.task.dto.TaskResponse;
import com.OnRoot.onroot.domain.task.dto.TaskUpdateRequest;
import com.OnRoot.onroot.domain.task.entity.Task;
import com.OnRoot.onroot.domain.task.repository.TaskRepository;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.global.exception.NotFoundException;
import com.OnRoot.onroot.global.exception.UnauthorizedException;
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
    public TaskResponse createTask(Long planId, TaskCreateRequest request, User user) {
        Plan plan = getPlanAndCheckOwnership(planId, user);
        Task task = Task.builder()
                .plan(plan)
                .title(request.title())
                .scheduledDate(request.scheduledDate())
                .orderIndex(request.orderIndex())
                .build();
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(Long planId, User user) {
        Plan plan = getPlanAndCheckOwnership(planId, user);
        return taskRepository.findByPlanOrderByOrderIndexAsc(plan).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public TaskResponse updateTask(Long planId, Long taskId, TaskUpdateRequest request, User user) {
        Plan plan = getPlanAndCheckOwnership(planId, user);
        Task task = taskRepository.findByIdAndPlan(taskId, plan)
                .orElseThrow(() -> new NotFoundException("할일을 찾을 수 없습니다."));
        task.update(request.title(), request.scheduledDate(), request.orderIndex());
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse completeTask(Long planId, Long taskId, User user) {
        Plan plan = getPlanAndCheckOwnership(planId, user);
        Task task = taskRepository.findByIdAndPlan(taskId, plan)
                .orElseThrow(() -> new NotFoundException("할일을 찾을 수 없습니다."));
        task.complete(LocalDateTime.now());
        return TaskResponse.from(task);
    }

    @Transactional
    public void deleteTask(Long planId, Long taskId, User user) {
        Plan plan = getPlanAndCheckOwnership(planId, user);
        Task task = taskRepository.findByIdAndPlan(taskId, plan)
                .orElseThrow(() -> new NotFoundException("할일을 찾을 수 없습니다."));
        taskRepository.delete(task);
    }

    private Plan getPlanAndCheckOwnership(Long planId, User user) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("계획을 찾을 수 없습니다."));
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("해당 리소스에 접근 권한이 없습니다.");
        }
        return plan;
    }
}
