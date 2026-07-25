package com.itpassport.app.quiz;

import com.itpassport.app.entity.Genre;

public record GenreBreakdown(
        Genre genre,
        long totalCount,
        long correctCount,
        double accuracy
) {
}
