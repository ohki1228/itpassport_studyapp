package com.itpassport.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "terms")
@Getter
@Setter
@NoArgsConstructor
public class Term {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_id")
    private Knowledge knowledge;

    @Column(nullable = false, length = 100)
    private String term;

    @Column(nullable = false, length = 100)
    private String reading;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(columnDefinition = "TEXT")
    private String breakdown;

    @Column(name = "related_terms", columnDefinition = "TEXT")
    private String relatedTerms;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
