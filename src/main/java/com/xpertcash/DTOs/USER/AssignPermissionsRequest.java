package com.xpertcash.DTOs.USER;

import com.xpertcash.entity.PermissionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignPermissionsRequest {
 
    private Map<PermissionType, Boolean> permissions;
    private List<Long> boutiqueIdsForStockManagement;
    private List<Long> boutiqueIdsForProductManagement;
}
