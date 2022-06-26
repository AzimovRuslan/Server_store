package com.example.springtask.repos;

import com.example.springtask.domain.store.Price;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRepository extends JpaRepository<Price, Long> {
}
