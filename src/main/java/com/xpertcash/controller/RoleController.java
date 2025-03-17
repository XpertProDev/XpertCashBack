package com.xpertcash.controller;

import org.springframework.web.bind.annotation.*;

import com.xpertcash.DTOs.RoleRequest;
import com.xpertcash.entity.Permission;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.User;
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


        @PutMapping("/updateRole/{userId}")
    public ResponseEntity<User> updateUserRole(
            @RequestHeader("Authorization") String token, 
            @PathVariable Long userId, 
            @RequestBody RoleRequest roleRequest) {
        User updatedUser = roleService.updateUserRole(token, userId, roleRequest.getName());
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/getAllRoles")
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }


}
