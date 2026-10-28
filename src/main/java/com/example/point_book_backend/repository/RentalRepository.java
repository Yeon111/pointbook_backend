package com.example.point_book_backend.repository;

import com.example.point_book_backend.domain.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    // 👇 새롭게 추가된 코드: 수납장 번호(cabinetSlot)로 대여 기록을 찾는 기능
    Optional<Rental> findByCabinetSlot(Integer cabinetSlot);
    Optional<Rental> findTopByOrderByIdDesc(); // 가장 최근 대여 내역 조회
}