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

    // [대여 API] POST /api/rentals (인증번호까지 함께 받도록 수정)
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

            // 👈 프론트엔드가 보낸 1단계, 2단계 인증번호 추출
            String auth_code_1 = request.get("auth_code_1") != null ? request.get("auth_code_1").toString() : null;
            String auth_code_2 = request.get("auth_code_2") != null ? request.get("auth_code_2").toString() : null;

            // 서비스로 인증번호까지 함께 전달
            Rental rental = rentalService.rentBook(userNameStr, bookId, paymentMethod, rentalHours, totalPoints, auth_code_1, auth_code_2);

            return ResponseEntity.ok(Map.of("success", true, "message", "대여 성공", "rental", rental));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // [반납 API] POST /api/returns
    @PostMapping("/returns")
    public ResponseEntity<?> returnBook(@RequestBody Map<String, Object> request) {
        try {
            Long bookId = Long.valueOf(request.get("bookId").toString());

            rentalService.returnBook(bookId);

            return ResponseEntity.ok(Map.of("success", true, "message", "반납 성공 (대여 가능 상태로 복구 완료)"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // [프론트엔드 -> 백엔드] 개별 인증번호 저장용 (기존 유지)
    @PostMapping("/rentals/auth-code")
    public ResponseEntity<?> saveAuthCode(@RequestBody Map<String, Object> request) {
        try {
            Integer cabinetSlot = Integer.valueOf(request.get("cabinetSlot").toString());
            String auth_code_1 = request.get("auth_code_1").toString();
            String auth_code_2 = request.get("auth_code_2").toString();

            rentalService.saveSequentialAuthCodes(cabinetSlot, auth_code_1, auth_code_2);

            return ResponseEntity.ok(Map.of("success", true, "message", "2단계 보안 인증번호 DB 저장 완료"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 🚨 [하드웨어 -> 백엔드] 1단계 키패드 입력 인증번호 검증
    @PostMapping("/rentals/verify-step1")
    public ResponseEntity<Map<String, Boolean>> verifyStep1(@RequestBody Map<String, Object> request) {
        try {
            Integer cabinetSlot = Integer.valueOf(request.get("cabinetSlot").toString());
            String inputCode = request.get("authCode").toString();

            boolean isMatch = rentalService.verifyStep1Code(cabinetSlot, inputCode);

            return ResponseEntity.ok(Map.of("open", isMatch)); // true면 1단계 성공
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("open", false));
        }
    }

    // 🚨 [하드웨어 -> 백엔드] 2단계 키패드 입력 인증번호 최종 검증
    @PostMapping("/rentals/verify-step2")
    public ResponseEntity<Map<String, Boolean>> verifyStep2(@RequestBody Map<String, Object> request) {
        try {
            Integer cabinetSlot = Integer.valueOf(request.get("cabinetSlot").toString());
            String inputCode = request.get("authCode").toString();

            boolean isMatch = rentalService.verifyStep2Code(cabinetSlot, inputCode);

            return ResponseEntity.ok(Map.of("open", isMatch)); // true면 최종 문 개방
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("open", false));
        }
    }
}