package com.mohey.fcmpushservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.mohey.fcmpushservice.config.FirebaseProperties;
import com.mohey.fcmpushservice.document.FcmLog;
import com.mohey.fcmpushservice.dto.*;
import com.mohey.fcmpushservice.repository.FcmLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private FirebaseProperties firebaseProperties;
    private final FcmLogRepository fcmLogRepository;
    private final ObjectMapper mapper;

    @Autowired
    public FcmService(FirebaseProperties firebaseProperties, FcmLogRepository fcmLogRepository, ObjectMapper mapper) {
        this.firebaseProperties = firebaseProperties;
        this.fcmLogRepository = fcmLogRepository;
        this.mapper = mapper;
    }

    public void sendMessageTo(String kafkaMessage) throws IOException{
        String message = makeMessageTo(kafkaMessage);
        JsonNode jsonNode = mapper.readTree(kafkaMessage);
        String type = jsonNode.path("type").asText();
        log.info("type : " + type);
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
        saveLog(response,type);
    }
    public void sendMessageTopic(String kafkaMessage) throws IOException{
        String message = makeMessageTopic(kafkaMessage);
        String API_URL = "https://fcm.googleapis.com/v1/projects/" + firebaseProperties.getProject_id() +"/messages:send";
        log.info("message = " + message);
        JsonNode jsonNode = mapper.readTree(kafkaMessage);
        String type = jsonNode.path("topic").asText();
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
        saveLog(response,type);
    }

    public void sendChat(String kafkaMessage) throws IOException{
        String API_URL = "https://fcm.googleapis.com/v1/projects/" + firebaseProperties.getProject_id() +"/messages:send";
        OkHttpClient client = new OkHttpClient();
        ChatResponseDto chatResponseDto = mapper.readValue(kafkaMessage, ChatResponseDto.class);
        String senderUuid = chatResponseDto.getSenderUuid();
        String type = "chat";
        for(GroupMemberDto groupMemberDto: chatResponseDto.getGroupMembers()) {
            if (senderUuid.equals(groupMemberDto.getMemberUuid())) {
                continue;
            }
            for (String fcmToken : groupMemberDto.getDeviceTokenList()) {
                String message = makeChatMessage(fcmToken, chatResponseDto);
                RequestBody requestBody = RequestBody.create(message, MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(requestBody)
                        .addHeader(HttpHeaders.AUTHORIZATION,"Bearer " + getAccessToken())
                        .addHeader(HttpHeaders.CONTENT_TYPE, "application/json; UTF-8")
                        .build();
                Response response = client.newCall(request)
                        .execute();
                saveLog(response,type);
            }
        }
    }
    private String makeMessageTo(String kafkaMessage) throws JsonProcessingException{
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
    private String makeMessageTopic(String kafkaMessage) throws JsonProcessingException{
        try{
            NoticeResponseDto noticeResponseDto = mapper.readValue(kafkaMessage, NoticeResponseDto.class);
            FcmTopicMessageDto fcmTopicMessageDto = FcmTopicMessageDto.builder()
                    .message(FcmTopicMessageDto.Message.builder()
                            .topic(noticeResponseDto.getTopic())
                            .notification(FcmTopicMessageDto.Notification.builder()
                                    .title(noticeResponseDto.getTitle())
                                    .body(noticeResponseDto.getBody())
                                    .build())
                            .build())
                    .validate_only(false)
                    .build();
            return mapper.writeValueAsString(fcmTopicMessageDto);
        }catch (JsonProcessingException ex){
            ex.printStackTrace();
        }
        return null;
    }

    private String makeChatMessage(String fcmToken,ChatResponseDto chatResponseDto) {
        try {
            String bodyMessage = chatResponseDto.getSenderName();
            String imageUrl = "";
            String messageType = chatResponseDto.getMessageType();
            if("message".equals(messageType)){
                bodyMessage = bodyMessage + ": " + chatResponseDto.getMessage();
            }else if("image".equals(messageType)){
                imageUrl = chatResponseDto.getImageUrl();
            }else if("location".equals(messageType)){
                bodyMessage += ": 위치 공유";
            }
            FcmMessageDto fcmMessageDto = FcmMessageDto.builder()
                    .message(FcmMessageDto.Message.builder()
                            .token(fcmToken)
                            .notification(FcmMessageDto.Notification.builder()
                                    .title(chatResponseDto.getGroupName())
                                    .body(bodyMessage)
                                    .image(imageUrl)
                                    .build())
                            .build())
                    .validate_only(false)
                    .build();
            log.info("fcmMessage : " + fcmMessageDto.toString());
            return mapper.writeValueAsString(fcmMessageDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getAccessToken() throws IOException {
//        log.info("properties :" + firebaseProperties);
        String firebaseCredentials = mapper.writeValueAsString(firebaseProperties);
//        log.info("firebaseCredientials = " + firebaseCredentials);
        ByteArrayInputStream credentialsAsStream = new ByteArrayInputStream(firebaseCredentials.getBytes());
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(credentialsAsStream)
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        googleCredentials.refreshIfExpired();
//        log.info("token : " + googleCredentials.getAccessToken().getTokenValue());
        return googleCredentials.getAccessToken().getTokenValue();
    }

    private void saveLog(Response response,String type) throws IOException{
        String responseBody = response.body().string();

        FcmLog fcmLog = new FcmLog();
        fcmLog.setTimestamp(new Date());
        fcmLog.setSuccess(response.isSuccessful());
        fcmLog.setStatusCode(response.code());

        if(response.isSuccessful()){
            // 200 전송 성공
            JsonNode rootNode = mapper.readTree(responseBody);
            String messageId = rootNode.path("name").asText();
            fcmLog.setFcmMessageId(messageId);
        } else{
            // 400 에러
            JsonNode rootNode = mapper.readTree(responseBody);
            String status = rootNode.path("error").path("status").asText();
            fcmLog.setErrorMessage(status);
        }

        fcmLog.setType(type);
        log.info("fcmLog : " + fcmLog.toString());
        fcmLogRepository.save(fcmLog);
    }
}