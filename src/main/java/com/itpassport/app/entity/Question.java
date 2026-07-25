package com.itpassport.app.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_type", nullable = false, length = 10)
    private AnswerType answerType;

    @Column(name = "required_answer_count", nullable = false)
    private Integer requiredAnswerCount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuestionStatus status;

    @Column(length = 255)
    private String source;

    @Column(name = "table_data", columnDefinition = "TEXT")
    private String tableData;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("choiceNumber ASC")
    private List<QuestionChoice> choices = new ArrayList<>();

    /**
     * table_dataを行×セルの表に分解する(1行目は見出し行)。テンプレート側で{@code question.tableRows}として参照する。
     */
    @Transient
    public List<List<String>> getTableRows() {
        if (tableData == null || tableData.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tableData.split("\n"))
                .map(row -> Arrays.stream(row.split("\\|", -1)).map(String::trim).toList())
                .toList();
    }
}
