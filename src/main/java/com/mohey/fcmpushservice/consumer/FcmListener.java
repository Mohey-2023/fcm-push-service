package com.mohey.fcmpushservice.consumer;

import com.mohey.fcmpushservice.service.FcmService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@AllArgsConstructor
@Slf4j
public class FcmListener {

    private FcmService fcmService;

    @KafkaListener(topics="personal-push")
    public void personalPush(String kafkaMessage) throws IOException {
        log.info("푸시 알림 메세지 : " + kafkaMessage);
        fcmService.sendMessageTo(kafkaMessage);
    }

    @KafkaListener(topics="all-push")
    public void allPush(String kafkaMessage) throws IOException{
        log.info("전체 푸시 알림 메시지 : " + kafkaMessage);
        fcmService.sendMessageTopic(kafkaMessage);
    }

}
