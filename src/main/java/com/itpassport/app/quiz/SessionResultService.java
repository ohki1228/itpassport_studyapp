package com.itpassport.app.quiz;

import com.itpassport.app.entity.AnswerHistory;
import com.itpassport.app.entity.Genre;
import com.itpassport.app.repository.AnswerHistoryRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 演習・苦手克服・実践演習の結果画面で共通して使う集計(全体正答率・ジャンル別内訳・誤答一覧)。
 */
@Service
@RequiredArgsConstructor
public class SessionResultService {

    private final AnswerHistoryRepository answerHistoryRepository;

    public SessionResult build(Long sessionId) {
        List<AnswerHistory> all = answerHistoryRepository.findBySessionIdWithDetails(sessionId);

        int answeredCount = all.size();
        int correctCount = (int) all.stream().filter(AnswerHistory::isCorrect).count();
        double accuracy = answeredCount == 0 ? 0.0 : (double) correctCount / answeredCount * 100;

        Map<Long, List<AnswerHistory>> byGenre = new LinkedHashMap<>();
        for (AnswerHistory history : all) {
            byGenre.computeIfAbsent(history.getGenre().getId(), id -> new java.util.ArrayList<>()).add(history);
        }
        List<GenreBreakdown> genreBreakdown = byGenre.values().stream()
                .map(list -> {
                    Genre genre = list.get(0).getGenre();
                    long total = list.size();
                    long correct = list.stream().filter(AnswerHistory::isCorrect).count();
                    double genreAccuracy = total == 0 ? 0.0 : (double) correct / total * 100;
                    return new GenreBreakdown(genre, total, correct, genreAccuracy);
                })
                .sorted(Comparator.comparing(b -> b.genre().getDisplayOrder()))
                .toList();

        List<AnswerHistory> wrongAnswers = all.stream().filter(h -> !h.isCorrect()).toList();

        return new SessionResult(answeredCount, correctCount, accuracy, genreBreakdown, wrongAnswers);
    }
}
