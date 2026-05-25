package com.OnRoot.onroot.domain.plan.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OnRoot.onroot.domain.plan.dto.PlanCreateRequest;
import com.OnRoot.onroot.domain.plan.dto.PlanDetailResponse;
import com.OnRoot.onroot.domain.plan.dto.PlanResponse;
import com.OnRoot.onroot.domain.plan.dto.PlanUpdateRequest;
import com.OnRoot.onroot.domain.plan.entity.Plan;
import com.OnRoot.onroot.domain.plan.entity.PlanStatus;
import com.OnRoot.onroot.domain.plan.repository.PlanRepository;
import com.OnRoot.onroot.domain.task.dto.TaskResponse;
import com.OnRoot.onroot.domain.task.repository.TaskRepository;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.global.exception.NotFoundException;
import com.OnRoot.onroot.global.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final TaskRepository taskRepository;
    private final com.OnRoot.onroot.domain.examschedule.repository.ExamScheduleRepository examScheduleRepository;

    @Transactional(readOnly = true)
    public List<PlanResponse> getPlans(User user) {
        return planRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(PlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanDetailResponse getPlan(Long planId, User user) {
        Plan plan = getPlanAndCheckOwnership(planId, user);
        List<TaskResponse> tasks = taskRepository.findByPlanOrderByOrderIndexAsc(plan).stream()
                .map(TaskResponse::from)
                .toList();
        return PlanDetailResponse.from(plan, tasks);
    }

    @Transactional
    public PlanResponse updatePlan(Long planId, PlanUpdateRequest request, User user) {
        Plan plan = getPlanAndCheckOwnership(planId, user);
        plan.update(request.title(), request.category(), request.targetDate(), request.status());
        return PlanResponse.from(plan);
    }

    @Transactional
    public void deletePlan(Long planId, User user) {
        Plan plan = getPlanAndCheckOwnership(planId, user);
        planRepository.delete(plan);
    }

    @Transactional
    public PlanResponse createPlan(PlanCreateRequest request, User user) {
        // Manual plan creation (no AI delegation)
        Plan.PlanBuilder builder = Plan.builder()
            .user(user)
            .title(request.title())
            .category(request.category())
            .targetDate(request.targetDate())
            .status(PlanStatus.IN_PROGRESS)
            .createdAt(java.time.LocalDateTime.now());

        if (request.examScheduleId() != null) {
            examScheduleRepository.findById(request.examScheduleId()).ifPresent(es -> builder.writtenExamSchedule(es));
        }

        Plan saved = planRepository.save(builder.build());
        return PlanResponse.from(saved);
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
