package com.example.demo.service;

import com.example.demo.model.Question;
import com.example.demo.model.QuestionAssignment;
import com.example.demo.repository.AnswerRepository;
import com.example.demo.repository.QuestionAssignmentRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.UserRepository;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.demo.model.Answer;
@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private QuestionAssignmentRepository questionAssignmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AnswerRepository answerRepository;

    @Override
    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }

    @Override
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    @Override
    public QuestionAssignment assignQuestionToUser(Long userId, Long questionId) throws BadRequestException {
        QuestionAssignment questionAssignment = new QuestionAssignment();
        QuestionAssignment existingAssignment = questionAssignmentRepository.findByAuditor_UserIdAndQuestion_QuestionId(userId, questionId);
        if (existingAssignment != null) {
            throw new BadRequestException("Question already assigned to this user");
        }
        questionAssignment.setAuditor(userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found")));
        questionAssignment.setQuestion(questionRepository.findById(questionId).orElseThrow(() -> new BadRequestException("Question not found")));
        return questionAssignmentRepository.save(questionAssignment);
    }

    @Override
    public List<Question> getAssignedQuestionsForUser(Long userId) throws BadRequestException {
        userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        List<QuestionAssignment> byAuditorUserId = questionAssignmentRepository.findByAuditor_UserId(userId);

        return byAuditorUserId.stream().map(QuestionAssignment::getQuestion).collect(Collectors.toList());
    }

    @Override
    public void submitAnswers(Long userId, List<Map<Long, String>> questionAnswers) throws BadRequestException {
        // Implementation for submitting answers will go here
        // This is a placeholder implementation
        userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        for (Map<Long, String> answerMap : questionAnswers) {
            for (Map.Entry<Long, String> entry : answerMap.entrySet()) {
                Long questionId = entry.getKey();
                String answer = entry.getValue();
                Answer ans = new Answer();
                ans.setAuditor(userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")));
                ans.setAssignment(questionAssignmentRepository.findByAuditor_UserIdAndQuestion_QuestionId(userId,questionId));
                ans.setAnswerText(answer);

           answerRepository.save(ans);

            }
        }
    }

    @Override
    public List<Answer> reviewAnswers(Long userId) throws BadRequestException {
        userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        return  answerRepository.findByAuditor_UserId(userId);
    }

    @Override
    public Answer updateAnswerStatus(Long answerId, Answer updatedAnswer) throws BadRequestException {
        Answer answer = answerRepository.findById(answerId).orElseThrow(() -> new BadRequestException("Answer not found"));
        QuestionAssignment.Status status = updatedAnswer.getAssignment().getStatus();
        answer.getAssignment().setStatus(status);
        questionAssignmentRepository.save(answer.getAssignment());
        updatedAnswer.setAnswerId(answerId);
        return answerRepository.save(updatedAnswer);
    }

    @Override
    public Answer submitSingleAnswer(Long userId, Answer answer) throws BadRequestException {
        userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));
        answer.setAuditor(userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found")));
        questionAssignmentRepository.findByAuditor_UserIdAndQuestion_QuestionId(userId, answer.getAssignment().getQuestion().getQuestionId());
        return answerRepository.save(answer);
    }

    @Override
    public Answer updateAnswer(Long answerId, Answer answer) throws BadRequestException {
        Answer existingAns = answerRepository.findById(answerId).orElseThrow(() -> new BadRequestException("Answer not found"));
      existingAns.setAnswerText(answer.getAnswerText());
//      existingAns.setAssignment(answer.getAssignment());
//        existingAns.setAuditor(answer.getAuditor());
        return answerRepository.save(existingAns);

    }

    @Override
    public void deleteQuestion(Long id) throws BadRequestException {
        questionRepository.findById(id).orElseThrow(() -> new BadRequestException("Question not found"));
        questionRepository.deleteById(id);
    }

}
