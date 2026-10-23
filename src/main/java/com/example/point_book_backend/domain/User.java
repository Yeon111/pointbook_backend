package com.example.point_book_backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "student_id", unique = true)
    private String studentId; // 학번

    private String name; // 이름

    private String pin; // 고유 비밀번호 (PIN)

    @Column(name = "point_balance")
    private int pointBalance = 1000; // 가입 시 기본 1000포인트

    @Column(name = "penalty_status")
    private boolean penaltyStatus;

    public User() {}

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public int getPointBalance() { return pointBalance; }
    public void setPointBalance(int pointBalance) { this.pointBalance = pointBalance; }
    public boolean isPenaltyStatus() { return penaltyStatus; }
    public void setPenaltyStatus(boolean penaltyStatus) { this.penaltyStatus = penaltyStatus; }

    // 🚨 [추가] BookController에서 에러가 나지 않도록 연결해 주는 메서드
    public boolean getPenaltyStatus() { return penaltyStatus; }
}