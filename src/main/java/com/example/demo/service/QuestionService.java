package com.example.demo.service;

import com.example.demo.model.Answer;
import com.example.demo.model.Question;
import com.example.demo.model.QuestionAssignment;
import org.apache.coyote.BadRequestException;

import java.util.List;
import java.util.Map;

public interface QuestionService {
    Question createQuestion(Question question);

    List<Question> getAllQuestions();

    QuestionAssignment assignQuestionToUser(Long userId, Long questionId) throws BadRequestException;

    List<Question> getAssignedQuestionsForUser(Long userId) throws BadRequestException;

    void submitAnswers(Long userId, List<Map<Long, String>> questionAnswers) throws BadRequestException;

    List<Answer> reviewAnswers(Long userId) throws BadRequestException;

    Answer updateAnswerStatus(Long answerId,Answer updatedAnswer) throws BadRequestException;

    Answer submitSingleAnswer(Long userId, Answer answer) throws BadRequestException;

    Answer updateAnswer(Long answerId, Answer answer) throws BadRequestException;

    void deleteQuestion(Long id) throws BadRequestException;
}
