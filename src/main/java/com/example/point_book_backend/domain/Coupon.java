package com.example.point_book_backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 쿠폰 이름
    private String issueDate; // 발급 날짜

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore // 무한 참조 방지
    private User user;

    public Coupon() {}

    public Coupon(String name, String issueDate, User user) {
        this.name = name;
        this.issueDate = issueDate;
        this.user = user;
    }
}