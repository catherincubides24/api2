package com.petshop.repository;

import com.petshop.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// ProductRepository.java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();
    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String name); // parametrizada por Hibernate
}