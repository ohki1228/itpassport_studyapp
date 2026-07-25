package com.itpassport.app.repository;

import com.itpassport.app.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByDisplayOrderAsc();
}
