package com.example.point_book_backend.controller;

import com.example.point_book_backend.domain.Gifticon;
import com.example.point_book_backend.domain.User;
import com.example.point_book_backend.dto.GifticonExchangeRequestDto;
import com.example.point_book_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/gifticons")
public class GifticonController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeGifticon(@RequestBody GifticonExchangeRequestDto requestDto) {
        try {
            // 1. 유저 조회
            User user = userRepository.findByStudentId(requestDto.getStudentId())
                    .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

            // 2. 포인트 검증 및 차감 로직
            int cost = requestDto.getCost();
            if (user.getPointBalance() < cost) {
                return ResponseEntity.ok().body(Map.of("success", false, "message", "포인트가 부족합니다."));
            }
            user.setPointBalance(user.getPointBalance() - cost);

            // 3. 교환된 기프티콘을 DB에 영구 저장
            Gifticon newGifticon = new Gifticon(
                    requestDto.getGiftName(),
                    java.time.LocalDate.now().toString(),
                    user
            );
            user.getExchangedGifticons().add(newGifticon);

            userRepository.save(user);

            // 4. 프론트엔드가 요구한 응답 데이터 포맷 (성공 여부, 갱신된 포인트, 최신 쿠폰함 목록)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("updatedPoints", user.getPointBalance());
            response.put("exchangedGifticons", user.getExchangedGifticons());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}