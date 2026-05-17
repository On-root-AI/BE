package com.OnRoot.onroot.domain.ai.entity;

import com.OnRoot.onroot.domain.plan.entity.Plan;
import com.OnRoot.onroot.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AI_GENERATION_LOG")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiGenerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(name = "user_input", nullable = false, columnDefinition = "TEXT")
    private String userInput;

    @Column(name = "generated_json", nullable = false, columnDefinition = "JSON")
    private String generatedJson;

    @Column(name = "llm_model", nullable = false, length = 50)
    private String llmModel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
