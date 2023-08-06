package com.mohey.fcmpushservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.mohey.fcmpushservice.config.FirebaseProperties;
import com.mohey.fcmpushservice.dto.FcmMessageDto;
import com.mohey.fcmpushservice.dto.NotificationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.http.HttpHeaders;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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

    public void sendMessageTo(String kafkaMessage) throws IOException{
        String message = makeMessage(kafkaMessage);
        String API_URL = "https://fcm.googleapis.com/v1/projects/" + firebaseProperties.getProject_id() +"/messages:send";
        log.info("message = " + message);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(message, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader(HttpHeaders.AUTHORIZATION,"Bearer " + getAccessToken())
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json; UTF-8")
                .build();
        Response response = client.newCall(request)
                .execute();
        System.out.println(response.body().string());
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