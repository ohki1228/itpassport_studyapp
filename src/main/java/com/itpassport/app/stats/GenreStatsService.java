package com.itpassport.app.stats;

import com.itpassport.app.entity.Genre;
import com.itpassport.app.repository.AnswerHistoryRepository;
import com.itpassport.app.repository.GenreRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenreStatsService {

    /** 苦手判定の対象とする最低回答数(要件6.3)。 */
    private static final int MIN_ANSWERS_FOR_JUDGEMENT = 5;

    private final GenreRepository genreRepository;
    private final AnswerHistoryRepository answerHistoryRepository;

    public List<GenreStat> getGenreStats(Long userId) {
        Map<Long, GenreAnswerAggregate> aggregates = new HashMap<>();
        answerHistoryRepository.aggregateByGenre(userId)
                .forEach(a -> aggregates.put(a.genreId(), a));

        return genreRepository.findAll().stream()
                .map(genre -> toGenreStat(genre, aggregates.get(genre.getId())))
                .sorted(Comparator.comparing(s -> s.genre().getDisplayOrder()))
                .toList();
    }

    public List<GenreStat> getWeakGenres(Long userId) {
        return getGenreStats(userId).stream()
                .filter(s -> s.judgement() == GenreJudgement.WEAK || s.judgement() == GenreJudgement.VERY_WEAK)
                .sorted(Comparator.comparingDouble(GenreStat::accuracy))
                .toList();
    }

    public double getOverallAccuracy(Long userId) {
        List<GenreAnswerAggregate> aggregates = answerHistoryRepository.aggregateByGenre(userId);
        long total = aggregates.stream().mapToLong(GenreAnswerAggregate::totalCount).sum();
        long correct = aggregates.stream().mapToLong(GenreAnswerAggregate::correctCount).sum();
        return total == 0 ? 0.0 : (double) correct / total * 100;
    }

    private GenreStat toGenreStat(Genre genre, GenreAnswerAggregate aggregate) {
        long total = aggregate == null ? 0 : aggregate.totalCount();
        long correct = aggregate == null ? 0 : aggregate.correctCount();
        double accuracy = total == 0 ? 0.0 : (double) correct / total * 100;
        GenreJudgement judgement = classify(total, accuracy);
        return new GenreStat(genre, total, correct, accuracy, judgement);
    }

    private GenreJudgement classify(long total, double accuracy) {
        if (total < MIN_ANSWERS_FOR_JUDGEMENT) {
            return GenreJudgement.INSUFFICIENT_DATA;
        }
        if (accuracy >= 80) {
            return GenreJudgement.EXCELLENT;
        }
        if (accuracy >= 60) {
            return GenreJudgement.NORMAL;
        }
        if (accuracy >= 40) {
            return GenreJudgement.WEAK;
        }
        return GenreJudgement.VERY_WEAK;
    }
}
