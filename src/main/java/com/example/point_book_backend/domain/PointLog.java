package com.example.point_book_backend.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "point_log")
public class PointLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId; // 제보/신고한 사용자 ID (FK)

    @Column(name = "target_book_id")
    private Long targetBookId; // 관련 도서 ID (FK)

    @Column(name = "report_type")
    private String reportType; // 유형 ('파손신고', '분실물제보', '조기반납리워드')

    @Column(name = "image_url")
    private String imageUrl; // 인증 사진 경로 (S3)

    @Column(name = "point_changed")
    private Integer pointChanged; // 증감된 포인트 양

    @Column(name = "created_at")
    private LocalDateTime createdAt; // 제보/신고 시각

    public PointLog() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTargetBookId() { return targetBookId; }
    public void setTargetBookId(Long targetBookId) { this.targetBookId = targetBookId; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getPointChanged() { return pointChanged; }
    public void setPointChanged(Integer pointChanged) { this.pointChanged = pointChanged; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}