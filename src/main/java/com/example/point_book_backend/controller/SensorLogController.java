package com.example.point_book_backend.controller;

import com.example.point_book_backend.domain.SensorLog;
import com.example.point_book_backend.repository.SensorLogRepository;
import com.example.point_book_backend.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensors")
@CrossOrigin("*")
public class SensorLogController {

    @Autowired
    private SensorLogRepository sensorLogRepository;

    @Autowired
    private RentalService rentalService;

    // 1. 센서 로그 전체 조회 (GET)
    @GetMapping("/logs")
    public List<SensorLog> getSensorLogs() {
        return sensorLogRepository.findAll();
    }

    // 2. 🚀 하드웨어의 배열/통합 JSON 포맷 완벽 대응 POST 메서드
    @PostMapping("/logs")
    public ResponseEntity<?> createSensorLog(@RequestBody Map<String, Object> request) {
        try {
            // 하드웨어/앱에서 넘어오는 값들 추출
            // (하드웨어가 보내는 키값에 맞춰서 꺼내오기)
            Object cabinetSlotObj = request.get("cabinetSlot");
            Object lockerNoObj = request.get("lockerNo");
            Object doorStatusObj = request.get("doorStatus");
            List<Boolean> bookSensors = (List<Boolean>) request.get("bookSensors");

            SensorLog sensorLog = new SensorLog();

            // 1) 캐비넷 슬롯 (1보관함, 2보관함 번호) 매핑
            if (cabinetSlotObj != null) {
                sensorLog.setCabinetSlot(Integer.valueOf(cabinetSlotObj.toString()));
            }

            // 2) 라커 넘버 (구역 정보) 매핑 -> 이제 null 안 뜨고 데이터가 들어오면 쏙 박힘!
            if (lockerNoObj != null) {
                sensorLog.setLockerNo(lockerNoObj.toString());
            }

            // 3) 문 상태 매핑
            if (doorStatusObj != null) {
                sensorLog.setDoorStatus(Boolean.valueOf(doorStatusObj.toString()));
            }

            // 4) 리미트 스위치 상태 (배열의 첫 번째 값 혹은 필요한 값) 매핑
            if (bookSensors != null && !bookSensors.isEmpty()) {
                sensorLog.setLimitSwitchStatus(bookSensors.get(0));
            }

            // DB에 저장
            sensorLogRepository.save(sensorLog);

            // 실시간 상태 동기화 필요 시 호출
            if (lockerNoObj != null && bookSensors != null) {
                rentalService.updateBookStatusBySensors(lockerNoObj.toString(), bookSensors);
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "센서 데이터 맵핑 및 저장 성공"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "파싱 에러: " + e.getMessage()));
        }
    }
}