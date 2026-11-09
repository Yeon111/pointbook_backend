package com.example.point_book_backend.repository;

import com.example.point_book_backend.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // 👈 새로 추가된 메서드를 위해 Optional 임포트 추가

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // 🚨 [핵심 해결] 캐시를 타지 않고 DB에서 가장 최신 상태의 책 목록을 실시간으로 가져옵니다!
    @Query("SELECT b FROM Book b ORDER BY b.id DESC")
    List<Book> findAllByOrderByIdDesc();

    // 🚀 [새로 추가] 사물함 번호로 가장 최근에 등록/보관 진행 중인 도서 1건 조회
    Optional<Book> findTopByStorageNoOrderByIdDesc(String storageNo);
}