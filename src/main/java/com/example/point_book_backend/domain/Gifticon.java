package com.example.point_book_backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Gifticon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 예: "조선대 주차권 1시간 변경권"
    private String exchangeDate; // 교환 날짜

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore // 무한 참조 방지
    private User user;

    public Gifticon() {}

    public Gifticon(String name, String exchangeDate, User user) {
        this.name = name;
        this.exchangeDate = exchangeDate;
        this.user = user;
    }

    public String getName() { return name; }
    public String getExchangeDate() { return exchangeDate; }
}