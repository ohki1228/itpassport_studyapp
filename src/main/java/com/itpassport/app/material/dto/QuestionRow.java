package com.itpassport.app.material.dto;

import java.util.List;

public record QuestionRow(
        long questionId,
        long categoryId,
        long genreId,
        String questionText,
        String tableData,
        List<String> choices,
        String answerType,
        List<Integer> correctAnswers,
        int requiredAnswerCount,
        String explanation,
        List<String> choiceExplanations,
        String status,
        String source
) {
}
