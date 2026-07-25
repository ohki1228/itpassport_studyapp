package com.itpassport.app.web;

import com.itpassport.app.entity.Category;
import com.itpassport.app.entity.Genre;
import java.util.List;

public record CategoryGenres(
        Category category,
        List<Genre> genres
) {
}
