package com.OnRoot.onroot.domain.examschedule.repository;

import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    boolean existsByExamNameAndExamDate(String examName, java.time.LocalDate examDate);
}
