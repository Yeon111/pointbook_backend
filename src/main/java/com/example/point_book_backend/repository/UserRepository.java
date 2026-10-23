package com.example.point_book_backend.repository;

import com.example.point_book_backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);
    // 👇 학번으로 DB에서 유저를 찾아오는 메서드 추가
    Optional<User> findByStudentId(String studentId);
}