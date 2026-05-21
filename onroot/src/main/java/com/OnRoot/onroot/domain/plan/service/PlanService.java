package com.OnRoot.onroot.domain.plan.service;

import com.OnRoot.onroot.domain.plan.dto.PlanDetailResponse;
import com.OnRoot.onroot.domain.plan.dto.PlanResponse;
import com.OnRoot.onroot.domain.plan.dto.PlanUpdateRequest;
import com.OnRoot.onroot.domain.plan.entity.Plan;
import com.OnRoot.onroot.domain.plan.repository.PlanRepository;
import com.OnRoot.onroot.domain.task.dto.TaskResponse;
import com.OnRoot.onroot.domain.task.repository.TaskRepository;
import com.OnRoot.onroot.domain.user.entity.User;
import com.OnRoot.onroot.global.exception.NotFoundException;
import com.OnRoot.onroot.global.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final TaskRepository taskRepository;

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

    private Plan getPlanAndCheckOwnership(Long planId, User user) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("계획을 찾을 수 없습니다."));
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("해당 리소스에 접근 권한이 없습니다.");
        }
        return plan;
    }
}
