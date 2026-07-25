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
@Table(name = "knowledge")
@Getter
@Setter
@NoArgsConstructor
public class Knowledge {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(columnDefinition = "TEXT")
    private String point;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
