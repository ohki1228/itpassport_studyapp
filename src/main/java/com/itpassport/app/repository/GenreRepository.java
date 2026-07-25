package com.itpassport.app.repository;

import com.itpassport.app.entity.Genre;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    List<Genre> findAllByCategoryIdOrderByDisplayOrderAsc(Long categoryId);
}
