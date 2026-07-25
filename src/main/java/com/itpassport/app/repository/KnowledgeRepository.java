package com.itpassport.app.repository;

import com.itpassport.app.entity.Knowledge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {

    List<Knowledge> findAllByGenreIdOrderByIdAsc(Long genreId);
}
