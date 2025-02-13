package com.xpertcash.controller;

import org.springframework.web.bind.annotation.*;

import com.xpertcash.entity.Role;
import com.xpertcash.service.RoleService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

public class RoleController {

    private final RoleService roleService;

    @GetMapping("/all")
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

}
