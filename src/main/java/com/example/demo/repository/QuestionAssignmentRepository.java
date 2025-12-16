package com.example.demo.repository;

import com.example.demo.model.QuestionAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionAssignmentRepository extends JpaRepository<QuestionAssignment, Long> {
    List<QuestionAssignment> findByAuditor_UserId(Long userId);

    QuestionAssignment findByAuditor_UserIdAndQuestion_QuestionId(Long userId, Long questionId);
}
