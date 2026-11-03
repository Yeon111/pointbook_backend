package com.example.point_book_backend.dto;

public class GifticonExchangeRequestDto {
    private String studentId;
    private String giftName;
    private int cost;

    public GifticonExchangeRequestDto() {}

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getGiftName() { return giftName; }
    public void setGiftName(String giftName) { this.giftName = giftName; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }
}