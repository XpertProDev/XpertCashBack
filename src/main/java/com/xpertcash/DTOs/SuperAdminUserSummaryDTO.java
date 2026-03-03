package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuperAdminUserSummaryDTO {

    private Long id;
    private String uuid;
    private String nomComplet;
    private String roleName;
    private boolean locked;
    private boolean lockedByQuota;
    private LocalDateTime lastActivity;
}
