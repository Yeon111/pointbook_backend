package com.example.point_book_backend.repository;

import com.example.point_book_backend.domain.SensorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorLogRepository extends JpaRepository<SensorLog, Long> {
}