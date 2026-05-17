package com.OnRoot.onroot.domain.examschedule.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "EXAM_SCHEDULE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_name", nullable = false, length = 100)
    private String examName;

    @Column(name = "subject", nullable = false, length = 100)
    private String subject;

    @Column(name = "application_start")
    private LocalDate applicationStart;

    @Column(name = "application_end")
    private LocalDate applicationEnd;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "result_date")
    private LocalDate resultDate;

    @Builder
    public ExamSchedule(String examName, String subject, LocalDate applicationStart,
                        LocalDate applicationEnd, LocalDate examDate, LocalDate resultDate) {
        this.examName = examName;
        this.subject = subject;
        this.applicationStart = applicationStart;
        this.applicationEnd = applicationEnd;
        this.examDate = examDate;
        this.resultDate = resultDate;
    }
}
