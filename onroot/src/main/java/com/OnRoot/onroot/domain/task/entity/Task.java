package com.OnRoot.onroot.domain.task.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.OnRoot.onroot.domain.plan.entity.Plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TASK")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "memo", length = 2000)
    private String memo;

    public void update(String title, LocalDate scheduledDate, Integer orderIndex) {
        if (title != null) this.title = title;
        if (scheduledDate != null) this.scheduledDate = scheduledDate;
        if (orderIndex != null) this.orderIndex = orderIndex;
    }

    public void update(String title, LocalDate scheduledDate, Integer orderIndex, String memo) {
        if (title != null) this.title = title;
        if (scheduledDate != null) this.scheduledDate = scheduledDate;
        if (orderIndex != null) this.orderIndex = orderIndex;
        if (memo != null) this.memo = memo;
    }

    public void complete(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getMemo() {
        return memo;
    }
}
