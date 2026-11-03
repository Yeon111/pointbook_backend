package com.example.point_book_backend.dto;

public class ReferralRequestDto {

    // 프론트엔드가 보내는 JSON의 키값과 정확히 일치해야 합니다.
    private String studentId;
    private String referralCode;

    // 기본 생성자 (Spring Boot에서 JSON을 객체로 변환할 때 필요함)
    public ReferralRequestDto() {
    }

    // Getter
    public String getStudentId() {
        return studentId;
    }

    public String getReferralCode() {
        return referralCode;
    }

    // Setter
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }
}