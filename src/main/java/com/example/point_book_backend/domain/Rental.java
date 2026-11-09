package com.example.point_book_backend.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "rental")
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "rental_type")
    private String rentalType; // '시간권', '복습권', '종일권'

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "due_time")
    private LocalDateTime dueTime;

    @Column(name = "return_time")
    private LocalDateTime returnTime;

    private String status; // '대여중', '반납완료', '연체'

    @Column(name = "cabinet_slot")
    private Integer cabinetSlot;

    // 💡 1단계 인증 번호만 남김 (2단계 제거)
    @Column(name = "auth_code_1")
    private String auth_code_1;

    // 💡 앱-하드웨어 API 분리를 위한 1단계 인증 통과 상태 필드
    @Column(name = "step1_passed")
    private Boolean step1Passed = false;

    public Rental() {}

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getRentalType() { return rentalType; }
    public void setRentalType(String rentalType) { this.rentalType = rentalType; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getDueTime() { return dueTime; }
    public void setDueTime(LocalDateTime dueTime) { this.dueTime = dueTime; }

    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getCabinetSlot() { return cabinetSlot; }
    public void setCabinetSlot(Integer cabinetSlot) { this.cabinetSlot = cabinetSlot; }

    // 💡 1단계 인증용 Getter & Setter만 유지
    public String getAuth_code_1() { return auth_code_1; }
    public void setAuth_code_1(String auth_code_1) { this.auth_code_1 = auth_code_1; }

    public Boolean getStep1Passed() { return step1Passed; }
    public void setStep1Passed(Boolean step1Passed) { this.step1Passed = step1Passed; }
}