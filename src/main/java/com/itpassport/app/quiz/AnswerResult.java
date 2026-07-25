package com.itpassport.app.quiz;

import com.itpassport.app.entity.Question;
import java.util.Set;

public record AnswerResult(
        Question question,
        boolean correct,
        Set<Integer> correctChoiceNumbers
) {
}
