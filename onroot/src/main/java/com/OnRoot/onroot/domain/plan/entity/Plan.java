package com.OnRoot.onroot.domain.plan.entity;

import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;
import com.OnRoot.onroot.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PLAN")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "written_exam_schedule_id")
    private ExamSchedule writtenExamSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practical_exam_schedule_id")
    private ExamSchedule practicalExamSchedule;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "weekday_hours")
    private Integer weekdayHours;

    @Column(name = "weekend_hours")
    private Integer weekendHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public void update(String title, String category, LocalDate targetDate, PlanStatus status) {
        if (title != null) this.title = title;
        if (category != null) this.category = category;
        if (targetDate != null) this.targetDate = targetDate;
        if (status != null) this.status = status;
    }
}
