package com.example.point_book_backend.controller;

import com.example.point_book_backend.domain.User;
import com.example.point_book_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // 1. 사용자 전체 조회
    @GetMapping("/users")
    public List getUsers() {
        return userRepository.findAll();
    }

    // 2. 신규 사용자 등록
    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    // 3. 사용자 포인트 충전
    @PostMapping("/users/{id}/charge")
    public User chargePoint(@PathVariable Long id, @RequestParam int amount) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // getPoint() 대신 DB 모델링에 맞춘 getPointBalance() 사용
        user.setPointBalance(user.getPointBalance() + amount);
        return userRepository.save(user);
    }
}