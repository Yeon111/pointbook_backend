package com.example.point_book_backend.controller;

import com.example.point_book_backend.domain.Rental;
import com.example.point_book_backend.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*") // 프론트엔드 통신 에러 방지
public class RentalController {

    @Autowired
    private RentalService rentalService;

    // 🚨 [대여 API] POST /api/rentals
    @PostMapping("/rentals")
    public ResponseEntity<?> rentBook(@RequestBody Map<String, Object> request) {
        try {
            Long bookId = Long.valueOf(request.get("bookId").toString());

            String userNameStr = request.get("userName") != null ? request.get("userName").toString() : null;
            if (userNameStr == null || userNameStr.isEmpty()) {
                throw new IllegalArgumentException("대여자 이름(userName)이 전달되지 않았습니다.");
            }

            String paymentMethod = request.get("paymentMethod") != null ? request.get("paymentMethod").toString() : "points";
            Integer rentalHours = request.get("rentalHours") != null ? Integer.valueOf(request.get("rentalHours").toString()) : 24;
            Integer totalPoints = request.get("totalPoints") != null ? Integer.valueOf(request.get("totalPoints").toString()) : 100;

            Rental rental = rentalService.rentBook(userNameStr, bookId, paymentMethod, rentalHours, totalPoints);

            return ResponseEntity.ok(Map.of("success", true, "message", "대여 성공", "rental", rental));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 🚨 [반납 API] 프론트엔드가 요청하는 POST /api/returns 주소로 완벽하게 맞춤!
    @PostMapping("/returns")
    public ResponseEntity<?> returnBook(@RequestBody Map<String, Object> request) {
        try {
            Long bookId = Long.valueOf(request.get("bookId").toString());

            // RentalService 내부에서 isAvailable = true, borrower = "" 로 복구 및 DB 반영
            rentalService.returnBook(bookId);

            return ResponseEntity.ok(Map.of("success", true, "message", "반납 성공 (대여 가능 상태로 복구 완료)"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}