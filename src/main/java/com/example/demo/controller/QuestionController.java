package com.example.demo.controller;

import com.example.demo.dto.AnswerDto;
import com.example.demo.model.Answer;
import com.example.demo.model.ApiResponse;
import com.example.demo.model.Question;
import com.example.demo.model.QuestionAssignment;
import com.example.demo.repository.QuestionAssignmentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.QuestionService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuestionAssignmentRepository questionAssignmentRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Question>> createQuestion(@RequestBody Question question) {
        // Implementation for creating a question will go here
        Question savedQuestion=questionService.createQuestion(question);
//        return ResponseEntity.ok(savedQuestion);
        return ResponseEntity.ok(new ApiResponse<>(true, "Question created successfully", savedQuestion));
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @GetMapping
    public ResponseEntity<List<Question>> getQuestions() {
        // Implementation for retrieving questions will go here
        List<Question> questions=questionService.getAllQuestions();
        return ResponseEntity.ok(questions);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign-question/{userId}/{questionId}")
    public ResponseEntity<ApiResponse<QuestionAssignment>> assignQuestionToUser(@PathVariable Long userId, @PathVariable Long questionId) {
        // Implementation for assigning a question to a user will go here
        try {
            QuestionAssignment questionAssignment = questionService.assignQuestionToUser(userId, questionId);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Question " + questionId + " assigned to User " + userId, questionAssignment)
            );

        } catch (BadRequestException e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(e.getMessage()));
        }

    }
//get questions assigned to auditor
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    @GetMapping("/assigned-questions/{userId}")
    public ResponseEntity<List<Question>> getAssignedQuestions(@PathVariable Long userId) {
        // Implementation for retrieving assigned questions for a user will go here
        try {
            List<Question> assignedQuestions = questionService.getAssignedQuestionsForUser(userId);
            return ResponseEntity.ok().body(assignedQuestions);
        }catch (BadRequestException e){
            return ResponseEntity.badRequest().build();
        }

    }

    //submit the answers for the questions assigned to auditor
    @PostMapping("/submit-answers/{userId}" )
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<ApiResponse<Answer>> submitAnswers(@PathVariable Long userId, @RequestBody List<Map<Long,String>> questionAnswers) {
        // Implementation for submitting answers will go here
        try {
            questionService.submitAnswers(userId, questionAnswers);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Answers submitted for User " + userId, null)
            );
//            return ResponseEntity.ok("Answers submitted for User " + userId);
        }catch (BadRequestException e){
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(e.getMessage()));
        }
        // For now, just return a placeholder response

    }
    @PostMapping("/submit-answers/single/{userId}" )
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<ApiResponse<Answer>> submitAnswersSingle(@PathVariable Long userId, @RequestBody AnswerDto answer) {
        // Implementation for submitting answers will go here
        try {
            answer.setUserId(userId);

         Answer ans=   questionService.submitSingleAnswer(userId, mapAnswerDtoToEntity(answer));
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Answers submitted for User " + userId, ans)
            );
//            return ResponseEntity.ok("Answers submitted for User " + userId);
        }catch (BadRequestException e){
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(e.getMessage()));
        }
        // For now, just return a placeholder response

    }

    private Answer mapAnswerDtoToEntity(AnswerDto answer) throws BadRequestException{
        Answer ans = new Answer();
        ans.setAuditor(userRepository.findById(answer.getUserId()).orElseThrow(() -> new BadRequestException("User not found")));
       ans.setAnswerText(answer.getAnswerText());
        QuestionAssignment byAuditorUserIdAndQuestionQuestionId = questionAssignmentRepository.findByAuditor_UserIdAndQuestion_QuestionId(answer.getUserId(), answer.getQuestionId());
       if(byAuditorUserIdAndQuestionQuestionId!=null){
              ans.setAssignment(byAuditorUserIdAndQuestionQuestionId);
       }
         else{
              throw new BadRequestException("Question not assigned to user");
            }


        return ans;
    }

    @GetMapping("/review-answers/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<List<Answer>> reviewAnswers(@PathVariable Long userId) {
        // Implementation for reviewing answers will go here
        try {
            List<Answer> result = questionService.reviewAnswers(userId);
            return ResponseEntity.ok().body(result);
        }catch (BadRequestException e){
            return ResponseEntity.badRequest().build();
        }
        // For now, just return a placeholder response

    }
    @PutMapping("/answer/{answerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<ApiResponse<Answer>> updateAnswer(@PathVariable Long answerId,@RequestBody Answer answer) {
        // Implementation for updating answer status will go here
        try {
            Answer ans = questionService.updateAnswer(answerId, answer);
//            return ResponseEntity.ok(ans);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Answer " + answerId + " updated successfully", ans)
            );
        }catch (BadRequestException e){
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(e.getMessage()));
        }


        // For now, just return a placeholder response

    }
    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/assignments")
    public ResponseEntity<List<QuestionAssignment>> getAllQuestionAssignments() {
        List<QuestionAssignment> assignments = questionAssignmentRepository.findAll();
        return ResponseEntity.ok(assignments);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Long id) {
        try {
            // Assuming you have a method in questionService to delete a question
            questionService.deleteQuestion(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Question deleted successfully", null));
        } catch (BadRequestException e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(e.getMessage()));
        }

    }
    @PutMapping("/answer/status/{answerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<ApiResponse<Answer>> updateAnswerStatus(@PathVariable Long answerId,@RequestBody Answer answer) {
        // Implementation for updating answer status will go here
        try {
            Answer ans = questionService.updateAnswerStatus(answerId, answer);
//            return ResponseEntity.ok(ans);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Answer " + answerId + " updated successfully", ans)
            );
        }catch (BadRequestException e){
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(e.getMessage()));
        }


        // For now, just return a placeholder response

    }
}
