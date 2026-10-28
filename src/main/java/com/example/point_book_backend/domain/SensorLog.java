package com.example.point_book_backend.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp; // 1. 이 줄을 새로 추가합니다.
import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "sensor_log")
public class SensorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Column(name = "cabinet_slot")
    private Integer cabinetSlot; // 수납장 칸 번호 (1단 또는 2단)

    @Column(name = "limit_switch_status")
    private Boolean limitSwitchStatus; // 리미트 스위치 눌림 여부

    @Column(name = "door_status")
    private Boolean doorStatus; // 문 닫힘 여부

    @CreationTimestamp // 2. 여기에 어노테이션을 추가합니다.
    @Column(name = "recorded_at", updatable = false) // 3. 괄호 안에 updatable = false를 추가합니다.
    private LocalDateTime recordedAt; // 감지 시각

    public SensorLog() {}

    // Getters and Setters (아래 부분은 기존 코드 그대로 두시면 됩니다)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCabinetSlot() { return cabinetSlot; }
    public void setCabinetSlot(Integer cabinetSlot) { this.cabinetSlot = cabinetSlot; }

    public Boolean getLimitSwitchStatus() { return limitSwitchStatus; }
    public void setLimitSwitchStatus(Boolean limitSwitchStatus) { this.limitSwitchStatus = limitSwitchStatus; }

    public Boolean getDoorStatus() { return doorStatus; }
    public void setDoorStatus(Boolean doorStatus) { this.doorStatus = doorStatus; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}