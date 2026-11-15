package com.example.point_book_backend.repository;

import com.example.point_book_backend.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT b FROM Book b ORDER BY b.id DESC")
    List<Book> findAllByOrderByIdDesc();

    // 🚀 이것만 남겨두시면 됩니다! (Book.java에 storageNo가 있으므로 정상 작동함)
    Optional<Book> findTopByStorageNoOrderByIdDesc(String storageNo);
}