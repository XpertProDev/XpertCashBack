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




}
