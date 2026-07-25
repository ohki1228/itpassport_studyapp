package com.itpassport.app.repository;

import com.itpassport.app.entity.Question;
import com.itpassport.app.entity.QuestionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByStatus(QuestionStatus status);

    List<Question> findByStatusAndCategoryId(QuestionStatus status, Long categoryId);

    /** open-in-view=falseのため、画面表示前にchoicesをフェッチ結合で取得しておく。 */
    @Query("SELECT q FROM Question q JOIN FETCH q.choices WHERE q.id = :id")
    Optional<Question> findByIdWithChoices(@Param("id") Long id);
}
