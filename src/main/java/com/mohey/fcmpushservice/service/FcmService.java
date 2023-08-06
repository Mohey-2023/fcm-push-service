package com.mohey.fcmpushservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.mohey.fcmpushservice.config.FirebaseProperties;
import com.mohey.fcmpushservice.dto.FcmMessageDto;
import com.mohey.fcmpushservice.dto.NotificationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private FirebaseProperties firebaseProperties;
    private final ObjectMapper mapper;

    @Autowired
    public FcmService(FirebaseProperties firebaseProperties, ObjectMapper mapper) {
        this.firebaseProperties = firebaseProperties;
        this.mapper = mapper;
    }

    private String makeMessage(String kafkaMessage) throws JsonProcessingException{
        try{
            NotificationResponseDto notificationResponseDto = mapper.readValue(kafkaMessage, NotificationResponseDto.class);
            FcmMessageDto fcmMessageDto = FcmMessageDto.builder()
                    .message(FcmMessageDto.Message.builder()
                            .token(notificationResponseDto.getFcmToken())
                            .notification(FcmMessageDto.Notification.builder()
                                    .title(notificationResponseDto.getTitle())
                                    .body(notificationResponseDto.getBody())
                                    .build())
                            .build())
                    .validate_only(false)
                    .build();
            return mapper.writeValueAsString(fcmMessageDto);
        }catch (JsonProcessingException ex){
        ex.printStackTrace();
        }
        return null;
    }

    private String getAccessToken() throws IOException {
        log.info("properties :" + firebaseProperties);
        String firebaseCredentials = mapper.writeValueAsString(firebaseProperties);
        log.info("firebaseCredientials = " + firebaseCredentials);
        ByteArrayInputStream credentialsAsStream = new ByteArrayInputStream(firebaseCredentials.getBytes());
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(credentialsAsStream)
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        googleCredentials.refreshIfExpired();
        log.info("token : " + googleCredentials.getAccessToken().getTokenValue());
        return googleCredentials.getAccessToken().getTokenValue();
    }
}