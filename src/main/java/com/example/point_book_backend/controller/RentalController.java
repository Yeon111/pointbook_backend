package com.example.point_book_backend.controller;

import com.example.point_book_backend.domain.Rental;
import com.example.point_book_backend.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class RentalController {

    @Autowired
    private RentalService rentalService;

    @PostMapping("/rentals")
    public ResponseEntity<?> rentBook(@RequestBody Map<String, Object> request) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long bookId = Long.valueOf(request.get("bookId").toString());

            String userNameStr = request.get("userName") != null ? request.get("userName").toString() : null;
            if (userNameStr == null || userNameStr.isEmpty()) {
                throw new IllegalArgumentException("대여자 이름(userName)이 전달되지 않았습니다.");
            }

            String paymentMethod = request.get("paymentMethod") != null ? request.get("paymentMethod").toString() : "points";
            Integer rentalHours = request.get("rentalHours") != null ? Integer.valueOf(request.get("rentalHours").toString()) : 24;
            Integer totalPoints = request.get("totalPoints") != null ? Integer.valueOf(request.get("totalPoints").toString()) : 100;

            String auth_code_1 = request.get("auth_code_1") != null ? request.get("auth_code_1").toString() : null;
            String auth_code_2 = request.get("auth_code_2") != null ? request.get("auth_code_2").toString() : null;

            Rental rental = rentalService.rentBook(userNameStr, bookId, paymentMethod, rentalHours, totalPoints, auth_code_1, auth_code_2);

            res.put("success", true);
            res.put("message", "대여 성공");
            res.put("rental", rental);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/returns")
    public ResponseEntity<?> returnBook(@RequestBody Map<String, Object> request) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long bookId = Long.valueOf(request.get("bookId").toString());
            rentalService.returnBook(bookId);

            res.put("success", true);
            res.put("message", "반납 성공");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/rentals/auth-code")
    public ResponseEntity<?> saveAuthCode(@RequestBody Map<String, Object> request) {
        Map<String, Object> res = new HashMap<>();
        try {
            Integer cabinetSlot = Integer.valueOf(request.get("cabinetSlot").toString());
            String auth_code_1 = request.get("auth_code_1").toString();
            String auth_code_2 = request.get("auth_code_2").toString();

            rentalService.saveSequentialAuthCodes(cabinetSlot, auth_code_1, auth_code_2);

            res.put("success", true);
            res.put("message", "인증번호 저장 완료");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/verify-step1")
    public ResponseEntity<?> verifyStep1(@RequestBody Map<String, Object> request) {
        Map<String, Boolean> res = new HashMap<>();
        try {
            Integer cabinetSlot = request.get("cabinetSlot") != null ? Integer.valueOf(request.get("cabinetSlot").toString()) : 1;
            String inputCode = request.get("authCode") != null ? request.get("authCode").toString() :
                    (request.get("auth_code_1") != null ? request.get("auth_code_1").toString() : "");

            boolean isMatch = rentalService.verifyStep1Code(cabinetSlot, inputCode);
            res.put("open", isMatch);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("open", false);
            return ResponseEntity.ok(res);
        }
    }

    @PostMapping("/verify-step2")
    public ResponseEntity<?> verifyStep2(@RequestBody Map<String, Object> request) {
        Map<String, Boolean> res = new HashMap<>();
        try {
            Integer cabinetSlot = request.get("cabinetSlot") != null ? Integer.valueOf(request.get("cabinetSlot").toString()) : 1;
            String inputCode = request.get("authCode") != null ? request.get("authCode").toString() :
                    (request.get("auth_code_2") != null ? request.get("auth_code_2").toString() : "");

            boolean isMatch = rentalService.verifyStep2Code(cabinetSlot, inputCode);
            res.put("open", isMatch);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("open", false);
            return ResponseEntity.ok(res);
        }
    }
}