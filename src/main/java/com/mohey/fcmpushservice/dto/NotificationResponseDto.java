package com.mohey.fcmpushservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationResponseDto {
    private String fcmToken;
    private String type;
    private String title;
    private String body;
}
