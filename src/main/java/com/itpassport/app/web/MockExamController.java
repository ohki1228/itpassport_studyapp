package com.itpassport.app.web;

import com.itpassport.app.auth.CurrentUserService;
import com.itpassport.app.entity.Question;
import com.itpassport.app.entity.StudyMode;
import com.itpassport.app.entity.User;
import com.itpassport.app.quiz.QuestionSelectionService;
import com.itpassport.app.quiz.QuizFlowService;
import com.itpassport.app.quiz.QuizSessionState;
import com.itpassport.app.quiz.SessionResult;
import com.itpassport.app.quiz.SessionResultService;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 実践演習(要件6.4): 100問/制限時間120分、回答中は正誤非表示、終了後にまとめて採点。
 * 出題〜1問ごとの回答記録の仕組みはQuizFlowServiceを演習・苦手克服と共用する。
 */
@Controller
@RequiredArgsConstructor
public class MockExamController {

    private static final String SESSION_KEY = "mockExamState";
    private static final int TIME_LIMIT_MINUTES = 120;

    private final CurrentUserService currentUserService;
    private final QuizFlowService quizFlowService;
    private final QuestionSelectionService questionSelectionService;
    private final SessionResultService sessionResultService;

    @GetMapping("/mock-exam/start")
    public String startForm() {
        return "mock-exam/start";
    }

    @PostMapping("/mock-exam/start")
    public String start(HttpSession session) {
        User user = currentUserService.getCurrentUser();
        List<Question> questions = questionSelectionService.selectForMockExam();

        QuizSessionState state = quizFlowService.startWithQuestions(
                user, StudyMode.MOCK_EXAM, questions, TIME_LIMIT_MINUTES);
        session.setAttribute(SESSION_KEY, state);
        return "redirect:/mock-exam/question";
    }

    @GetMapping("/mock-exam/question")
    public String question(HttpSession session, Model model) {
        QuizSessionState state = requireState(session);
        if (state.isFinished()) {
            quizFlowService.finish(state);
            return "redirect:/mock-exam/result";
        }

        Question question = quizFlowService.getCurrentQuestion(state);
        model.addAttribute("question", question);
        model.addAttribute("progressCurrent", state.getAnsweredCount() + 1);
        model.addAttribute("progressTotal", state.getQuestionIds().size());
        model.addAttribute("remainingSeconds", remainingSeconds(state));
        return "mock-exam/question";
    }

    @PostMapping("/mock-exam/answer")
    public String answer(@RequestParam(value = "choices", required = false) List<Integer> choices,
                          HttpSession session) {
        QuizSessionState state = requireState(session);
        List<Integer> selected = choices == null ? List.of() : choices;
        User user = currentUserService.getCurrentUser();

        quizFlowService.submitAnswer(user, state, selected);
        if (state.isFinished()) {
            quizFlowService.finish(state);
            return "redirect:/mock-exam/result";
        }
        return "redirect:/mock-exam/question";
    }

    @GetMapping("/mock-exam/timeout")
    public String timeout(HttpSession session) {
        QuizSessionState state = requireState(session);
        quizFlowService.finish(state);
        return "redirect:/mock-exam/result";
    }

    @GetMapping("/mock-exam/result")
    public String result(HttpSession session, Model model) {
        QuizSessionState state = requireState(session);
        SessionResult result = sessionResultService.build(state.getStudySessionId());
        model.addAttribute("result", result);
        return "mock-exam/result";
    }

    private long remainingSeconds(QuizSessionState state) {
        long limitSeconds = (long) state.getTimeLimitMinutes() * 60;
        long elapsed = Duration.between(state.getStartedAt(), LocalDateTime.now()).getSeconds();
        return Math.max(0, limitSeconds - elapsed);
    }

    private QuizSessionState requireState(HttpSession session) {
        Object attr = session.getAttribute(SESSION_KEY);
        if (!(attr instanceof QuizSessionState state) || state.getMode() != StudyMode.MOCK_EXAM) {
            throw new MockExamStateMissingException();
        }
        return state;
    }

    @ExceptionHandler(MockExamStateMissingException.class)
    public String handleMissingState() {
        return "redirect:/mock-exam/start";
    }

    private static class MockExamStateMissingException extends RuntimeException {
    }
}
