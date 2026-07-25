package com.itpassport.app.quiz;

import com.itpassport.app.entity.Question;
import com.itpassport.app.entity.QuestionStatus;
import com.itpassport.app.repository.QuestionRepository;
import com.itpassport.app.stats.GenreJudgement;
import com.itpassport.app.stats.GenreStat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionSelectionService {

    /** 実践演習の大分類ごとの出題数(要件6.4: ストラテジ35/マネジメント20/テクノロジ45)。大分類IDは categories のシード値と対応。 */
    private static final Map<Long, Integer> MOCK_EXAM_RATIO = Map.of(1L, 35, 2L, 20, 3L, 45);

    private final QuestionRepository questionRepository;

    public List<Question> selectRandom(int count) {
        List<Question> pool = new ArrayList<>(questionRepository.findByStatus(QuestionStatus.PUBLIC));
        java.util.Collections.shuffle(pool);
        return pool.stream().limit(count).toList();
    }

    /**
     * 苦手ジャンルほど選ばれやすい重み付き抽出(復元なし)。Efraimidis-Spirakis法により
     * key = random^(1/weight) の降順で並べ、上位count件を採用する。
     */
    public List<Question> selectWeighted(Long userId, List<GenreStat> genreStats, int count) {
        Map<Long, Integer> genreWeights = genreStats.stream()
                .collect(Collectors.toMap(s -> s.genre().getId(), this::weightOf));

        List<Question> pool = questionRepository.findByStatus(QuestionStatus.PUBLIC);
        Random random = new Random();

        return pool.stream()
                .map(q -> {
                    int weight = genreWeights.getOrDefault(q.getGenre().getId(), 1);
                    double key = Math.pow(random.nextDouble(), 1.0 / weight);
                    return Map.entry(key, q);
                })
                .sorted(Comparator.comparingDouble((Map.Entry<Double, Question> e) -> e.getKey()).reversed())
                .limit(count)
                .map(Map.Entry::getValue)
                .toList();
    }

    private int weightOf(GenreStat stat) {
        return switch (stat.judgement()) {
            case VERY_WEAK -> 6;
            case WEAK -> 4;
            case INSUFFICIENT_DATA -> 2;
            case NORMAL -> 2;
            case EXCELLENT -> 1;
        };
    }

    /** 実践演習用: 大分類ごとの出題比率(35:20:45)に沿って抽出する。データ不足分野は available 分だけ採用する。 */
    public List<Question> selectForMockExam() {
        List<Question> selected = new ArrayList<>();
        MOCK_EXAM_RATIO.forEach((categoryId, target) -> {
            List<Question> categoryPool = new ArrayList<>(
                    questionRepository.findByStatusAndCategoryId(QuestionStatus.PUBLIC, categoryId));
            java.util.Collections.shuffle(categoryPool);
            selected.addAll(categoryPool.stream().limit(target).toList());
        });
        java.util.Collections.shuffle(selected);
        return selected;
    }
}
