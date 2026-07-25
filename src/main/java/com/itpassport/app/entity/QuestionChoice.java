package com.itpassport.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "question_choices")
@Getter
@Setter
@NoArgsConstructor
public class QuestionChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "choice_number", nullable = false)
    private Integer choiceNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;
}
