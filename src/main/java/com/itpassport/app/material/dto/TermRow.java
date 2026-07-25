package com.itpassport.app.material.dto;

public record TermRow(
        long termId,
        long genreId,
        long knowledgeId,
        String term,
        String reading,
        String definition,
        String breakdown,
        String relatedTerms
) {
}
