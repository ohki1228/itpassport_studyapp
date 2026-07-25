package com.itpassport.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
