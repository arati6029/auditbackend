package com.example.demo.model;



import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long answerId;

    @ManyToOne()
    @JoinColumn(name = "assignment_id")
    private QuestionAssignment assignment;

    @ManyToOne()
    @JoinColumn(name = "auditor_id")
    private User auditor;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    public Long getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Long answerId) {
        this.answerId = answerId;
    }

    public QuestionAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(QuestionAssignment assignment) {
        this.assignment = assignment;
    }

    public User getAuditor() {
        return auditor;
    }

    public void setAuditor(User auditor) {
        this.auditor = auditor;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
}

