package com.itpassport.app.stats;

import com.itpassport.app.entity.Genre;

public record GenreStat(
        Genre genre,
        long totalCount,
        long correctCount,
        double accuracy,
        GenreJudgement judgement
) {
}
