package com.itpassport.app.repository;

import com.itpassport.app.entity.AnswerHistory;
import com.itpassport.app.stats.GenreAnswerAggregate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnswerHistoryRepository extends JpaRepository<AnswerHistory, Long> {

    @Query("""
            SELECT new com.itpassport.app.stats.GenreAnswerAggregate(
                ah.genre.id, COUNT(ah), SUM(CASE WHEN ah.correct = true THEN 1L ELSE 0L END))
            FROM AnswerHistory ah
            WHERE ah.user.id = :userId
            GROUP BY ah.genre.id
            """)
    List<GenreAnswerAggregate> aggregateByGenre(@Param("userId") Long userId);

    /** open-in-view=falseのため、結果画面で使うquestion/choices/genreを事前にフェッチ結合しておく。 */
    @Query("""
            SELECT DISTINCT ah FROM AnswerHistory ah
            JOIN FETCH ah.question q
            JOIN FETCH q.choices
            JOIN FETCH ah.genre
            WHERE ah.session.id = :sessionId
            ORDER BY ah.id
            """)
    List<AnswerHistory> findBySessionIdWithDetails(@Param("sessionId") Long sessionId);

    @Query("""
            SELECT new com.itpassport.app.stats.GenreAnswerAggregate(
                ah.genre.id, COUNT(ah), SUM(CASE WHEN ah.correct = true THEN 1L ELSE 0L END))
            FROM AnswerHistory ah
            WHERE ah.session.id = :sessionId
            GROUP BY ah.genre.id
            """)
    List<GenreAnswerAggregate> aggregateBySession(@Param("sessionId") Long sessionId);
}
