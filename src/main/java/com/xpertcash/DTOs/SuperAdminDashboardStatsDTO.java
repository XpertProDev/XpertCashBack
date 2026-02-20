package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminDashboardStatsDTO {
    private long totalEntreprises;
    private long totalActives;
    private long totalDesactivees;
    private long totalUsersAllEntreprises;
    private long nouvellesCetteSemaine;
    private long nouvellesCeMois;
}
