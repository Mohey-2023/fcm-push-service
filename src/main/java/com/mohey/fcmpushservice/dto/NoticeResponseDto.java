package com.mohey.fcmpushservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoticeResponseDto {
    private String topic;
    private String title;
    private String body;
}
