package com.OnRoot.onroot.domain.examschedule.repository;

import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    boolean existsByExamNameAndExamDate(String examName, LocalDate examDate);

    List<ExamSchedule> findByExamNameContainingAndExamDateGreaterThanEqualOrderByExamDateAsc(String keyword, LocalDate from);

    void deleteBySubjectIn(Set<String> subjects);
}
