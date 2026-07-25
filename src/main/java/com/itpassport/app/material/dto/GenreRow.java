package com.itpassport.app.material.dto;

public record GenreRow(
        long genreId,
        long categoryId,
        String categoryName,
        String genreName,
        int displayOrder
) {
}
