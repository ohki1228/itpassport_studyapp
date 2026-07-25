package com.itpassport.app.repository;

import com.itpassport.app.entity.Term;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TermRepository extends JpaRepository<Term, Long> {

    List<Term> findAllByOrderByGenreIdAscTermAsc();

    List<Term> findAllByOrderByReadingAsc();

    List<Term> findByTermIn(Collection<String> terms);

    @Query("SELECT t FROM Term t JOIN FETCH t.genre g JOIN FETCH g.category LEFT JOIN FETCH t.knowledge WHERE t.id = :id")
    Optional<Term> findByIdWithGenreAndKnowledge(Long id);
}
