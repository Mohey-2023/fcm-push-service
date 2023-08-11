package com.mohey.fcmpushservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatResponseDto {
    private String groupUuid;
    private String groupName;
    private String senderUuid;
    private String senderName;
    private String message;
    private String messageType;
    private String imageUrl;
    private List<GroupMemberDto> groupMembers;
}

