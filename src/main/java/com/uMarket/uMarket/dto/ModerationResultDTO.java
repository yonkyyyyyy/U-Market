package com.uMarket.uMarket.dto;

import com.uMarket.uMarket.enums.ModerationStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ModerationResultDTO {
    private String imageId;
    private String originalUrl;
    private ModerationStatus status;
    private double confidenceScore;
    private LocalDateTime processedAt;

    public ModerationResultDTO() {
        this.processedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    public ModerationResultDTO(String imageId, String originalUrl, ModerationStatus status, double confidenceScore) {
        this.imageId = imageId;
        this.originalUrl = originalUrl;
        this.status = status;
        this.confidenceScore = confidenceScore;
        this.processedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    // Getters y Setters
    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public ModerationStatus getStatus() {
        return status;
    }

    public void setStatus(ModerationStatus status) {
        this.status = status;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}