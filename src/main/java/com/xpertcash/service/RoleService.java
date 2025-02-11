package com.xpertcash.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.xpertcash.entity.Role;
import com.xpertcash.entity.RoleType;
import com.xpertcash.repository.RoleRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void initRoles() {
        if (roleRepository.count() == 0) {
            List<Role> roles = Arrays.asList(
                new Role(null, RoleType.SUPER_ADMIN),
                new Role(null, RoleType.ADMIN),
                new Role(null, RoleType.VENDEUR),
                new Role(null, RoleType.COMPTABLE),
                new Role(null, RoleType.RH)
            );
            roleRepository.saveAll(roles);
            System.out.println("Rôles ajoutés dans la base de données.");
        } 
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
    
}
