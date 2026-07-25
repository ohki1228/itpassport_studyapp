package com.itpassport.app.material.dto;

import java.util.List;

public record ParsedMaterials(
        List<GenreRow> genres,
        List<KnowledgeRow> knowledge,
        List<QuestionRow> questions,
        List<TermRow> terms
) {
}
