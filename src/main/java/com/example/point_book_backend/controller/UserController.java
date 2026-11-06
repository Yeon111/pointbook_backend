package com.example.point_book_backend.controller;

import com.example.point_book_backend.domain.User;
import com.example.point_book_backend.dto.ReferralRequestDto;
import com.example.point_book_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users")
    public List<User> getUsers() { return userRepository.findAll(); }

    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        // 🚨 [수정된 부분] UUID 지우고 학번 기반 생성 "USER_학번" 고정
        user.setReferralCode("USER_" + user.getStudentId());
        return userRepository.save(user);
    }

    @PostMapping("/users/{id}/charge")
    public User chargePoint(@PathVariable Long id, @RequestParam int amount) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.setPointBalance(user.getPointBalance() + amount);
        return userRepository.save(user);
    }

    @PostMapping("/referral")
    public ResponseEntity<?> applyReferralCode(@RequestBody ReferralRequestDto requestDto) {
        try {
            User me = userRepository.findByStudentId(requestDto.getStudentId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
            User friend = userRepository.findByReferralCode(requestDto.getReferralCode())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 잘못된 추천 코드입니다."));

            if (me.getId().equals(friend.getId())) { throw new IllegalArgumentException("자신의 추천 코드는 입력할 수 없습니다."); }

            me.setPointBalance(me.getPointBalance() + 100);
            friend.setPointBalance(friend.getPointBalance() + 100);

            userRepository.save(me);
            userRepository.save(friend);

            return ResponseEntity.ok(Map.of("success", true, "message", "추천 코드가 등록되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "서버 처리 중 문제가 발생했습니다."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        try {
            String studentId = loginData.get("studentId");
            String pin = loginData.get("pin");

            User user = userRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 학번입니다."));

            if (user.getPin() != null && !user.getPin().equals(pin)) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }

            // 🚨 [수정된 부분] 코드가 비어있거나 기존 방식(난수 등)으로 잘못 저장된 유저까지 싹 다 "USER_학번"으로 강제 교정
            if (user.getReferralCode() == null || !user.getReferralCode().equals("USER_" + user.getStudentId())) {
                user.setReferralCode("USER_" + user.getStudentId());
                userRepository.save(user); // DB에 'USER_학번' 형태로 저장 고정!
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("studentId", user.getStudentId());
            response.put("name", user.getName());
            response.put("pointBalance", user.getPointBalance());
            response.put("referralCode", user.getReferralCode());
            response.put("stamps", user.getStamps());
            response.put("rentalCoupons", user.getRentalCoupons());

            // 👇 [신규 추가] 프론트엔드 요청대로 로그인 시 기프티콘 목록 전송
            response.put("exchangedGifticons", user.getExchangedGifticons());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}