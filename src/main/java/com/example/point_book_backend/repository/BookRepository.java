package com.example.point_book_backend.repository;

import com.example.point_book_backend.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // 🚨 [핵심 해결] 캐시를 타지 않고 DB에서 가장 최신 상태의 책 목록을 실시간으로 가져옵니다!
    @Query("SELECT b FROM Book b ORDER BY b.id DESC")
    List<Book> findAllByOrderByIdDesc();
}