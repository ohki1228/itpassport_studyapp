package com.itpassport.app.web;

import com.itpassport.app.auth.CurrentUserService;
import com.itpassport.app.entity.Question;
import com.itpassport.app.entity.StudyMode;
import com.itpassport.app.entity.User;
import com.itpassport.app.quiz.AnswerResult;
import com.itpassport.app.quiz.QuizFlowService;
import com.itpassport.app.quiz.QuizSessionState;
import com.itpassport.app.quiz.SessionResult;
import com.itpassport.app.quiz.SessionResultService;
import com.itpassport.app.stats.GenreStatsService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * 演習問題(practice)と苦手克服(weakness)の共通フロー。両モードは「出題対象の選び方」だけが異なり、
 * 出題〜回答〜結果表示の流れは同じため1つのコントローラで扱う。
 */
@Controller
@RequiredArgsConstructor
public class QuizController {

    private static final String SESSION_KEY = "quizState";

    private final CurrentUserService currentUserService;
    private final QuizFlowService quizFlowService;
    private final GenreStatsService genreStatsService;
    private final SessionResultService sessionResultService;

    @GetMapping("/quiz/{mode}/start")
    public String startForm(@PathVariable String mode, Model model) {
        StudyMode studyMode = parseMode(mode);
        model.addAttribute("mode", mode);
        model.addAttribute("modeLabel", label(studyMode));
        if (studyMode == StudyMode.WEAKNESS) {
            User user = currentUserService.getCurrentUser();
            model.addAttribute("weakGenres", genreStatsService.getWeakGenres(user.getId()));
        }
        return "quiz/start";
    }

    @PostMapping("/quiz/{mode}/start")
    public String start(@PathVariable String mode, @RequestParam(required = false) String count,
                         HttpSession session) {
        StudyMode studyMode = parseMode(mode);
        Integer questionCount = ("endless".equals(count) || count == null || count.isBlank())
                ? null : Integer.valueOf(count);

        User user = currentUserService.getCurrentUser();
        QuizSessionState state = quizFlowService.start(user, studyMode, questionCount);
        session.setAttribute(SESSION_KEY, state);
        return "redirect:/quiz/" + mode + "/question";
    }

    @GetMapping("/quiz/{mode}/question")
    public String question(@PathVariable String mode, HttpSession session, Model model) {
        QuizSessionState state = requireState(session, mode);
        if (state.isFinished()) {
            quizFlowService.finish(state);
            return "redirect:/quiz/" + mode + "/result";
        }

        Question question = quizFlowService.getCurrentQuestion(state);
        model.addAttribute("mode", mode);
        model.addAttribute("modeLabel", label(state.getMode()));
        model.addAttribute("question", question);
        model.addAttribute("progressCurrent", state.getAnsweredCount() + 1);
        model.addAttribute("progressTotal", state.isEndless() ? null : state.getQuestionIds().size());
        return "quiz/question";
    }

    @PostMapping("/quiz/{mode}/answer")
    public String answer(@PathVariable String mode,
                          @RequestParam(value = "choices", required = false) List<Integer> choices,
                          HttpSession session, Model model) {
        QuizSessionState state = requireState(session, mode);
        List<Integer> selected = choices == null ? List.of() : choices;
        User user = currentUserService.getCurrentUser();

        AnswerResult result = quizFlowService.submitAnswer(user, state, selected);
        boolean finished = state.isFinished();
        if (finished) {
            quizFlowService.finish(state);
        }

        model.addAttribute("mode", mode);
        model.addAttribute("modeLabel", label(state.getMode()));
        model.addAttribute("question", result.question());
        model.addAttribute("correct", result.correct());
        model.addAttribute("correctChoiceNumbers", result.correctChoiceNumbers());
        model.addAttribute("selectedChoiceNumbers", selected);
        model.addAttribute("finished", finished);
        return "quiz/answer-result";
    }

    @GetMapping("/quiz/{mode}/finish")
    public String finishEarly(@PathVariable String mode, HttpSession session) {
        QuizSessionState state = requireState(session, mode);
        quizFlowService.finish(state);
        return "redirect:/quiz/" + mode + "/result";
    }

    @GetMapping("/quiz/{mode}/result")
    public String result(@PathVariable String mode, HttpSession session, Model model) {
        QuizSessionState state = requireState(session, mode);
        SessionResult result = sessionResultService.build(state.getStudySessionId());
        model.addAttribute("mode", mode);
        model.addAttribute("modeLabel", label(state.getMode()));
        model.addAttribute("result", result);
        return "quiz/result";
    }

    private QuizSessionState requireState(HttpSession session, String mode) {
        Object attr = session.getAttribute(SESSION_KEY);
        if (!(attr instanceof QuizSessionState state) || state.getMode() != parseMode(mode)) {
            throw new QuizStateMissingException("/quiz/" + mode + "/start");
        }
        return state;
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(QuizStateMissingException.class)
    public String handleMissingState(QuizStateMissingException ex) {
        return "redirect:" + ex.redirectPath;
    }

    private static class QuizStateMissingException extends RuntimeException {
        private final String redirectPath;

        QuizStateMissingException(String redirectPath) {
            this.redirectPath = redirectPath;
        }
    }

    private StudyMode parseMode(String mode) {
        return switch (mode) {
            case "practice" -> StudyMode.PRACTICE;
            case "weakness" -> StudyMode.WEAKNESS;
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "不明なモードです: " + mode);
        };
    }

    private String label(StudyMode mode) {
        return switch (mode) {
            case PRACTICE -> "演習問題";
            case WEAKNESS -> "苦手克服";
            case MOCK_EXAM -> "実践演習";
        };
    }
}
