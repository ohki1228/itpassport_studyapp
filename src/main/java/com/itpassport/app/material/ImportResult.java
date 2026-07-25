package com.itpassport.app.material;

import java.util.List;

public record ImportResult(
        int genresImported,
        int knowledgeImported,
        int questionsImported,
        int termsImported,
        List<String> errors
) {
}
