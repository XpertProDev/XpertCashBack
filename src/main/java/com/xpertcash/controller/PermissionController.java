package com.xpertcash.controller;

import com.xpertcash.entity.Permission;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.service.PermissionService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    // Ajouter une permission
    @PostMapping("/add")
    public ResponseEntity<?> addPermission(@RequestParam PermissionType permissionType) {
        try {
            Permission newPermission = permissionService.addPermission(permissionType);
            return ResponseEntity.ok(newPermission);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Récupérer toutes les permissions
    @GetMapping("/all")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }
}

