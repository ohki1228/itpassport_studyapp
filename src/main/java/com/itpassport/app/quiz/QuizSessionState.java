package com.itpassport.app.quiz;

import com.itpassport.app.entity.StudyMode;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出題中の1回分の演習状態。HttpSessionに保持し、画面遷移をまたいで参照する
 * (このアプリはページ遷移型のためサーバー側で進行状態を持つ必要がある)。
 */
public class QuizSessionState implements Serializable {

    private Long studySessionId;
    private StudyMode mode;
    private List<Long> questionIds;
    private int currentIndex;
    private boolean endless;
    private int answeredCount;
    private int correctCount;
    private LocalDateTime startedAt;
    private Integer timeLimitMinutes;
    private List<Long> wrongQuestionIds;
    private Long shuffledForQuestionId;
    private List<Integer> shuffledChoiceOrder;

    public Long getStudySessionId() {
        return studySessionId;
    }

    public void setStudySessionId(Long studySessionId) {
        this.studySessionId = studySessionId;
    }

    public StudyMode getMode() {
        return mode;
    }

    public void setMode(StudyMode mode) {
        this.mode = mode;
    }

    public List<Long> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<Long> questionIds) {
        this.questionIds = questionIds;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public boolean isEndless() {
        return endless;
    }

    public void setEndless(boolean endless) {
        this.endless = endless;
    }

    public int getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(int answeredCount) {
        this.answeredCount = answeredCount;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public List<Long> getWrongQuestionIds() {
        return wrongQuestionIds;
    }

    public void setWrongQuestionIds(List<Long> wrongQuestionIds) {
        this.wrongQuestionIds = wrongQuestionIds;
    }

    public Long getShuffledForQuestionId() {
        return shuffledForQuestionId;
    }

    public void setShuffledForQuestionId(Long shuffledForQuestionId) {
        this.shuffledForQuestionId = shuffledForQuestionId;
    }

    public List<Integer> getShuffledChoiceOrder() {
        return shuffledChoiceOrder;
    }

    public void setShuffledChoiceOrder(List<Integer> shuffledChoiceOrder) {
        this.shuffledChoiceOrder = shuffledChoiceOrder;
    }

    /** エンドレスモードでは尽きたら補充するため、常にfalse(終了は利用者の明示操作による)。 */
    public boolean isFinished() {
        return !endless && currentIndex >= questionIds.size();
    }
}
