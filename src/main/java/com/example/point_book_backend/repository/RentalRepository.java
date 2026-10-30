package com.example.point_book_backend.repository;

import com.example.point_book_backend.domain.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    // 🚀 [신규 1단계용] 수납장 번호 + 사용자가 누른 1단계 번호가 정확히 일치하는 진행 중인 대여건 딱 1개 찾기
    @Query("SELECT r FROM Rental r WHERE r.cabinetSlot = :cabinetSlot AND r.auth_code_1 = :inputCode AND r.status != '반납/회수완료'")
    Optional<Rental> findValidRentalForStep1(@Param("cabinetSlot") Integer cabinetSlot, @Param("inputCode") String inputCode);

    // 🚀 [신규 2단계용] 수납장 번호 + 사용자가 누른 2단계 번호가 정확히 일치하는 진행 중인 대여건 딱 1개 찾기
    @Query("SELECT r FROM Rental r WHERE r.cabinetSlot = :cabinetSlot AND r.auth_code_2 = :inputCode AND r.status != '반납/회수완료'")
    Optional<Rental> findValidRentalForStep2(@Param("cabinetSlot") Integer cabinetSlot, @Param("inputCode") String inputCode);

    Optional<Rental> findTopByOrderByIdDesc(); // 가장 최근 전체 내역 조회
}