package com.mohey.fcmpushservice.document;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "fcm_notifications_log")
public class FcmLog {
    private Date timestamp;
    private boolean success;
    private int statusCode;
    private String errorMessage;
    private String fcmMessageId;
    private String type;
}
