package com.itpassport.app.quiz;

import com.itpassport.app.entity.AnswerHistory;
import java.util.List;

public record SessionResult(
        int answeredCount,
        int correctCount,
        double accuracy,
        List<GenreBreakdown> genreBreakdown,
        List<AnswerHistory> wrongAnswers
) {
}
