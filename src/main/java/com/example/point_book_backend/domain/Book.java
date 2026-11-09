package com.example.point_book_backend.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat; // 날짜 포맷용 추가
import java.time.LocalDateTime; // 날짜 타입 추가
import java.util.ArrayList;
import java.util.List;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private String publisher;

    @Column(name = "book_condition")
    private String condition;

    private String storageNo;
    private String lockerNo;

    @JsonProperty("isAvailable")
    private Boolean isAvailable;

    private String owner;
    private String ownerPin;
    private String borrower;

    @Column(columnDefinition = "TEXT")
    private String image;

    @JsonProperty("rentalTimeLeft")
    private Long rentalTimeLeft;

    @JsonProperty("rentalDueDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime rentalDueDate;

    // 💡 1단계 인증 코드 필드만 유지 (2단계 제거)
    @Column(name = "auth_code_1")
    private String authCode1;

    // 💡 앱-하드웨어 API 분리를 위한 1단계 인증 통과 상태 필드
    @Column(name = "step1_passed")
    private Boolean step1Passed = false;

    // 프론트엔드가 요구하는 reports 배열 필드 (DB 1:N 관계 설정)
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Report> reports = new ArrayList<>();

    // --- Getter & Setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getStorageNo() { return storageNo; }
    public void setStorageNo(String storageNo) { this.storageNo = storageNo; }

    public String getLockerNo() { return lockerNo; }
    public void setLockerNo(String lockerNo) { this.lockerNo = lockerNo; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getOwnerPin() { return ownerPin; }
    public void setOwnerPin(String ownerPin) { this.ownerPin = ownerPin; }

    public String getBorrower() { return borrower; }
    public void setBorrower(String borrower) { this.borrower = borrower; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Long getRentalTimeLeft() { return rentalTimeLeft; }
    public void setRentalTimeLeft(Long rentalTimeLeft) { this.rentalTimeLeft = rentalTimeLeft; }

    public LocalDateTime getRentalDueDate() { return rentalDueDate; }
    public void setRentalDueDate(LocalDateTime rentalDueDate) { this.rentalDueDate = rentalDueDate; }

    // 💡 1단계 인증 코드 Getter & Setter만 유지
    public String getAuthCode1() { return authCode1; }
    public void setAuthCode1(String authCode1) { this.authCode1 = authCode1; }

    public Boolean getStep1Passed() { return step1Passed; }
    public void setStep1Passed(Boolean step1Passed) { this.step1Passed = step1Passed; }

    // reports Getter & Setter
    public List<Report> getReports() { return reports; }
    public void setReports(List<Report> reports) { this.reports = reports; }
}