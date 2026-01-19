package com.xpertcash.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.xpertcash.entity.Permission;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.repository.PermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class PermissionService {

    private final PermissionRepository permissionRepository;

    public Permission addPermission(PermissionType permissionType) {
        if (permissionRepository.existsByType(permissionType)) {
            throw new RuntimeException("Cette permission existe déjà !");
        }
        Permission newPermission = new Permission();
        newPermission.setType(permissionType);
        return permissionRepository.save(newPermission);
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

}
