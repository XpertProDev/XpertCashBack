package com.xpertcash.controller;

import org.springframework.web.bind.annotation.*;

import com.xpertcash.DTOs.RoleRequest;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.User;
import com.xpertcash.service.RoleService;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

public class RoleController {

    private final RoleService roleService;


        @PutMapping("/updateRole/{userId}")
    public ResponseEntity<User> updateUserRole(
            HttpServletRequest request, 
            @PathVariable Long userId, 
            @RequestBody RoleRequest roleRequest) {
        String token = request.getHeader("Authorization");
        User updatedUser = roleService.updateUserRole(token, userId, roleRequest.getName());
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/getAllRoles")
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }


}
