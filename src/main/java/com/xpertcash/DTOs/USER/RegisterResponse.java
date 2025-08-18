package com.xpertcash.DTOs.USER;

import com.xpertcash.entity.User;

import lombok.Data;

@Data
public class RegisterResponse {
    private boolean success;
    private String message;
    private User user;

}

