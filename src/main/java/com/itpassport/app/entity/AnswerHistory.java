package com.itpassport.app.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "answer_history")
@Getter
@Setter
@NoArgsConstructor
public class AnswerHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudyMode mode;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    // 常に少数(最大4件)かつ表示のたびに参照するため、EAGERにしてLazyInitializationExceptionを避ける
    // (choicesと同時にJOIN FETCHするとMultipleBagFetchExceptionになるため個別にEAGER取得する)。
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "answer_history_selections", joinColumns = @JoinColumn(name = "answer_history_id"))
    @Column(name = "choice_number")
    private List<Integer> selectedChoiceNumbers = new ArrayList<>();
}
