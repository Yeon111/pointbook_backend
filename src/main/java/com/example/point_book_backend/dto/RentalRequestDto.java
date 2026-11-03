package com.example.point_book_backend.dto;

public class RentalRequestDto {
    private String studentId;     // 학번
    private String paymentMethod; // 결제 수단 ('포인트' 또는 '쿠폰')
    private int totalCost;        // 대여 비용 (포인트 결제 시 사용)

    public RentalRequestDto() {}

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public int getTotalCost() { return totalCost; }
    public void setTotalCost(int totalCost) { this.totalCost = totalCost; }
}