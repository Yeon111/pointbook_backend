package com.example.point_book_backend.domain;

import jakarta.persistence.*;
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

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt; // 감지 시각

    public SensorLog() {}

    // Getters and Setters
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