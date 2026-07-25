package com.itpassport.app.material.dto;

public record KnowledgeRow(
        long knowledgeId,
        long genreId,
        String title,
        String body,
        String keywords,
        String point
) {
}
