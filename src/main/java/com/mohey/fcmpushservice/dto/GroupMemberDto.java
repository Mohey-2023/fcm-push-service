package com.mohey.fcmpushservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupMemberDto {
    private String memberUuid;
    private List<String> deviceTokenList;
}
