package com.xpertcash.controller;

import org.springframework.web.bind.annotation.*;

import com.xpertcash.entity.Permission;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Role;
import com.xpertcash.service.RoleService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

public class RoleController {

    private final RoleService roleService;

    // Ajouter une permission à un rôle
    @PostMapping("/{roleId}/addPermission")
    public ResponseEntity<?> addPermissionToRole(@PathVariable Long roleId, @RequestParam PermissionType permissionType) {
        try {
            Role updatedRole = roleService.addPermissionToRole(roleId, permissionType);
            return ResponseEntity.ok(updatedRole);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Récupérer les permissions d'un rôle
    @GetMapping("/{roleId}/permissions")
    public ResponseEntity<Set<Permission>> getPermissionsForRole(@PathVariable Long roleId) {
        Set<Permission> permissions = roleService.getPermissionsForRole(roleId);
        return ResponseEntity.ok(permissions);
    }


    @GetMapping("/all")
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

}
