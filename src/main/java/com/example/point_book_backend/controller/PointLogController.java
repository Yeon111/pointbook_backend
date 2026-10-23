package com.example.point_book_backend.controller;

import com.example.point_book_backend.domain.PointLog;
import com.example.point_book_backend.repository.PointLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/points")
public class PointLogController {

    @Autowired
    private PointLogRepository pointLogRepository;

    // 1. 포인트 제보/신고 내역 전체 조회 (GET)
    @GetMapping("/reports")
    public List<PointLog> getPointReports() {
        return pointLogRepository.findAll();
    }

    // 2. 신규 제보/신고 및 포인트 변동 등록 (POST)
    @PostMapping("/reports")
    public PointLog createPointReport(@RequestBody PointLog pointLog) {
        return pointLogRepository.save(pointLog);
    }
}