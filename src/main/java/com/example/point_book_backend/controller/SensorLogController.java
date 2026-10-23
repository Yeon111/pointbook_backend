package com.example.point_book_backend.controller;

import com.example.point_book_backend.domain.SensorLog;
import com.example.point_book_backend.repository.SensorLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
public class SensorLogController {

    @Autowired
    private SensorLogRepository sensorLogRepository;

    // 1. 센서 로그 전체 조회 (GET)
    @GetMapping("/logs")
    public List<SensorLog> getSensorLogs() {
        return sensorLogRepository.findAll();
    }

    // 2. 센서 데이터 수집 및 등록 (POST)
    @PostMapping("/logs")
    public SensorLog createSensorLog(@RequestBody SensorLog sensorLog) {
        return sensorLogRepository.save(sensorLog);
    }
}