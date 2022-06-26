package com.example.springtask.repos;

import com.example.springtask.domain.store.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
