package com.example.point_book_backend.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "student_id", unique = true)
    private String studentId;

    private String name;
    private String pin;

    @Column(name = "point_balance")
    private int pointBalance = 1000;

    @Column(name = "penalty_status")
    private boolean penaltyStatus;

    @Column(unique = true)
    private String referralCode;

    private int stamps = 0;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Coupon> rentalCoupons = new ArrayList<>();

    // 👇 [신규 추가] 기프티콘 쿠폰함
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Gifticon> exchangedGifticons = new ArrayList<>();

    public User() {}

    // 기존 Getter & Setter
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
    public boolean getPenaltyStatus() { return penaltyStatus; }
    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    public int getStamps() { return stamps; }
    public void setStamps(int stamps) { this.stamps = stamps; }
    public List<Coupon> getRentalCoupons() { return rentalCoupons; }
    public void setRentalCoupons(List<Coupon> rentalCoupons) { this.rentalCoupons = rentalCoupons; }

    // 👇 [신규 추가] 기프티콘 Getter & Setter
    public List<Gifticon> getExchangedGifticons() { return exchangedGifticons; }
    public void setExchangedGifticons(List<Gifticon> exchangedGifticons) { this.exchangedGifticons = exchangedGifticons; }
}