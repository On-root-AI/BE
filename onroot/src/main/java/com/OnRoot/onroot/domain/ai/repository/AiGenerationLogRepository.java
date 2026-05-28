package com.OnRoot.onroot.domain.ai.repository;

import com.OnRoot.onroot.domain.ai.entity.AiGenerationLog;
import com.OnRoot.onroot.domain.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, Long> {

    void deleteByPlan(Plan plan);
}
