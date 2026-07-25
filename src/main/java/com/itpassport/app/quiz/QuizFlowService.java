package com.itpassport.app.quiz;

import com.itpassport.app.entity.AnswerHistory;
import com.itpassport.app.entity.Question;
import com.itpassport.app.entity.QuestionChoice;
import com.itpassport.app.entity.StudyMode;
import com.itpassport.app.entity.StudySession;
import com.itpassport.app.entity.User;
import com.itpassport.app.repository.AnswerHistoryRepository;
import com.itpassport.app.repository.QuestionRepository;
import com.itpassport.app.repository.StudySessionRepository;
import com.itpassport.app.stats.GenreStatsService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演習(25/50/100/エンドレス)・苦手克服の共通フロー。ページ遷移のたびにQuizSessionStateを
 * HttpSessionから受け取り、進行状態の更新結果を呼び出し側(コントローラ)がセッションへ書き戻す。
 */
@Service
@RequiredArgsConstructor
public class QuizFlowService {

    private static final int ENDLESS_BATCH_SIZE = 20;

    private final QuestionSelectionService questionSelectionService;
    private final QuestionRepository questionRepository;
    private final StudySessionRepository studySessionRepository;
    private final AnswerHistoryRepository answerHistoryRepository;
    private final GenreStatsService genreStatsService;

    @Transactional
    public QuizSessionState start(User user, StudyMode mode, Integer count) {
        QuizSessionState state = newState(user, mode, count, null);

        List<Question> initial;
        if (count == null) {
            state.setEndless(true);
            initial = questionSelectionService.selectRandom(ENDLESS_BATCH_SIZE);
        } else {
            state.setEndless(false);
            if (mode == StudyMode.WEAKNESS) {
                initial = questionSelectionService.selectWeighted(
                        user.getId(), genreStatsService.getGenreStats(user.getId()), count);
            } else {
                initial = questionSelectionService.selectRandom(count);
            }
        }
        state.setQuestionIds(new ArrayList<>(initial.stream().map(Question::getId).toList()));
        return state;
    }

    /** 実践演習用: あらかじめ選んだ問題リストと制限時間を指定してセッションを開始する。 */
    @Transactional
    public QuizSessionState startWithQuestions(User user, StudyMode mode, List<Question> questions,
                                                Integer timeLimitMinutes) {
        QuizSessionState state = newState(user, mode, questions.size(), timeLimitMinutes);
        state.setEndless(false);
        state.setQuestionIds(new ArrayList<>(questions.stream().map(Question::getId).toList()));
        return state;
    }

    private QuizSessionState newState(User user, StudyMode mode, Integer count, Integer timeLimitMinutes) {
        StudySession session = new StudySession();
        session.setUser(user);
        session.setMode(mode);
        session.setQuestionCount(count);
        session.setTimeLimitMinutes(timeLimitMinutes);
        session.setStartedAt(LocalDateTime.now());
        studySessionRepository.save(session);

        QuizSessionState state = new QuizSessionState();
        state.setStudySessionId(session.getId());
        state.setMode(mode);
        state.setCurrentIndex(0);
        state.setAnsweredCount(0);
        state.setCorrectCount(0);
        state.setStartedAt(session.getStartedAt());
        state.setTimeLimitMinutes(timeLimitMinutes);
        state.setWrongQuestionIds(new ArrayList<>());
        return state;
    }

    public Question getCurrentQuestion(QuizSessionState state) {
        if (state.isEndless() && state.getCurrentIndex() >= state.getQuestionIds().size()) {
            state.getQuestionIds().addAll(
                    questionSelectionService.selectRandom(ENDLESS_BATCH_SIZE).stream()
                            .map(Question::getId)
                            .toList());
        }
        Long id = state.getQuestionIds().get(state.getCurrentIndex());
        Question question = questionRepository.findByIdWithChoices(id)
                .orElseThrow(() -> new IllegalStateException("問題が見つかりません: id=" + id));
        applyChoiceOrder(state, question);
        return question;
    }

    /**
     * 選択肢の表示順を問題ごとにシャッフルする(位置で正解を覚えられないようにするため)。
     * 同じ問題を表示している間は順序を保持し、次の問題に進むとリセットされる。
     */
    private void applyChoiceOrder(QuizSessionState state, Question question) {
        if (!question.getId().equals(state.getShuffledForQuestionId())) {
            List<Integer> order = new ArrayList<>(question.getChoices().stream()
                    .map(QuestionChoice::getChoiceNumber)
                    .toList());
            Collections.shuffle(order);
            state.setShuffledForQuestionId(question.getId());
            state.setShuffledChoiceOrder(order);
        }
        Map<Integer, QuestionChoice> byNumber = question.getChoices().stream()
                .collect(Collectors.toMap(QuestionChoice::getChoiceNumber, c -> c));
        List<QuestionChoice> reordered = state.getShuffledChoiceOrder().stream()
                .map(byNumber::get)
                .toList();
        question.getChoices().clear();
        question.getChoices().addAll(reordered);
    }

    @Transactional
    public AnswerResult submitAnswer(User user, QuizSessionState state, List<Integer> selectedChoiceNumbers) {
        Question question = getCurrentQuestion(state);
        Set<Integer> correctNumbers = question.getChoices().stream()
                .filter(QuestionChoice::isCorrect)
                .map(QuestionChoice::getChoiceNumber)
                .collect(Collectors.toSet());
        boolean correct = new HashSet<>(selectedChoiceNumbers).equals(correctNumbers);

        AnswerHistory history = new AnswerHistory();
        history.setSession(studySessionRepository.getReferenceById(state.getStudySessionId()));
        history.setUser(user);
        history.setQuestion(question);
        history.setGenre(question.getGenre());
        history.setCorrect(correct);
        history.setMode(state.getMode());
        history.setAnsweredAt(LocalDateTime.now());
        history.setSelectedChoiceNumbers(new ArrayList<>(selectedChoiceNumbers));
        answerHistoryRepository.save(history);

        state.setAnsweredCount(state.getAnsweredCount() + 1);
        if (correct) {
            state.setCorrectCount(state.getCorrectCount() + 1);
        } else {
            state.getWrongQuestionIds().add(question.getId());
        }
        state.setCurrentIndex(state.getCurrentIndex() + 1);

        return new AnswerResult(question, correct, correctNumbers);
    }

    @Transactional
    public void finish(QuizSessionState state) {
        StudySession session = studySessionRepository.findById(state.getStudySessionId())
                .orElseThrow(() -> new IllegalStateException("演習セッションが見つかりません。"));
        session.setFinishedAt(LocalDateTime.now());
        if (state.isEndless()) {
            session.setQuestionCount(state.getAnsweredCount());
        }
        studySessionRepository.save(session);
    }
}
