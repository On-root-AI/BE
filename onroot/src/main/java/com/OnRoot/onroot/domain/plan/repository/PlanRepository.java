package com.OnRoot.onroot.domain.plan.repository;

import com.OnRoot.onroot.domain.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    @Modifying
    @Query("UPDATE Plan p SET p.writtenExamSchedule = null, p.practicalExamSchedule = null WHERE p.writtenExamSchedule IS NOT NULL OR p.practicalExamSchedule IS NOT NULL")
    void clearExamScheduleReferences();

}
