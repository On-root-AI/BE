package com.OnRoot.onroot.domain.task.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.OnRoot.onroot.domain.plan.entity.Plan;
import com.OnRoot.onroot.domain.plan.entity.PlanStatus;
import com.OnRoot.onroot.domain.plan.repository.PlanRepository;
import com.OnRoot.onroot.domain.streak.service.StreakService;
import com.OnRoot.onroot.domain.task.dto.TaskResponse;
import com.OnRoot.onroot.domain.task.entity.Task;
import com.OnRoot.onroot.domain.task.repository.TaskRepository;
import com.OnRoot.onroot.domain.user.entity.User;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PlanRepository planRepository;

        @Mock
        private StreakService streakService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void completeTask_togglesCompletedAtBetweenNowAndNull() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("hash")
                .nickname("tester")
                .provider("local")
                .createdAt(LocalDateTime.now())
                .build();

        Plan plan = Plan.builder()
                .id(10L)
                .user(user)
                .title("plan")
                .category("category")
                .targetDate(LocalDate.of(2026, 5, 27))
                .status(PlanStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();

        Task task = Task.builder()
                .id(100L)
                .plan(plan)
                .title("task")
                .scheduledDate(LocalDate.of(2026, 5, 27))
                .orderIndex(0)
                .build();

        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(taskRepository.findByIdAndPlan(100L, plan)).thenReturn(Optional.of(task));

        TaskResponse first = taskService.completeTask(10L, 100L, user);
        TaskResponse second = taskService.completeTask(10L, 100L, user);

        assertThat(first.completedAt()).isNotNull();
        assertThat(second.completedAt()).isNull();
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    void getTasks_preservesCompletedAtValues() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("hash")
                .nickname("tester")
                .provider("local")
                .createdAt(LocalDateTime.now())
                .build();

        Plan plan = Plan.builder()
                .id(10L)
                .user(user)
                .title("plan")
                .category("category")
                .targetDate(LocalDate.of(2026, 5, 27))
                .status(PlanStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();

        Task pending = Task.builder()
                .id(100L)
                .plan(plan)
                .title("pending")
                .scheduledDate(LocalDate.of(2026, 5, 27))
                .orderIndex(0)
                .build();

        Task done = Task.builder()
                .id(101L)
                .plan(plan)
                .title("done")
                .scheduledDate(LocalDate.of(2026, 5, 27))
                .orderIndex(1)
                .build();
        done.complete(LocalDateTime.of(2026, 5, 27, 10, 0));

        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(taskRepository.findByPlanOrderByOrderIndexAsc(plan)).thenReturn(List.of(pending, done));

        List<TaskResponse> responses = taskService.getTasks(10L, user);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).completedAt()).isNull();
        assertThat(responses.get(1).completedAt()).isEqualTo(LocalDateTime.of(2026, 5, 27, 10, 0));
    }
}