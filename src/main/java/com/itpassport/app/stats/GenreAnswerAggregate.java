package com.itpassport.app.stats;

public record GenreAnswerAggregate(
        Long genreId,
        long totalCount,
        long correctCount
) {
}
