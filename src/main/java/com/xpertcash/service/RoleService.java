package com.xpertcash.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.xpertcash.entity.Permission;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.RoleType;
import com.xpertcash.repository.PermissionRepository;
import com.xpertcash.repository.RoleRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @PostConstruct
    public void initRoles() {
        // Ajouter les rôles uniquement si le rôle ADMIN n'existe pas déjà
        if (!roleRepository.existsByName(RoleType.ADMIN)) {
            List<Role> roles = Arrays.asList(
                new Role(null, RoleType.SUPER_ADMIN, null),
                new Role(null, RoleType.ADMIN, null),
                new Role(null, RoleType.VENDEUR, null),
                new Role(null, RoleType.COMPTABLE, null),
                new Role(null, RoleType.RH, null)
            );
            roleRepository.saveAll(roles);
            System.out.println("Rôles ajoutés dans la base de données.");
        }
    }

    // Ajouter une permission à un rôle
    // Ajouter une permission à un rôle
    @Transactional
    public Role addPermissionToRole(Long roleId, PermissionType permissionType) {
        // Trouver le rôle
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Rôle non trouvé"));

        // Vérifier si la permission existe
        Permission permission = permissionRepository.findByType(permissionType)
            .orElseGet(() -> {
                // Si la permission n'existe pas, la créer et la sauvegarder
                Permission newPermission = new Permission();
                newPermission.setType(permissionType);
                return permissionRepository.save(newPermission);
            });

        // Ajouter la permission au rôle si ce n'est pas déjà fait
        if (!role.getPermissions().contains(permission)) {
            role.getPermissions().add(permission);
            roleRepository.save(role);  // Sauvegarder les changements
        }

        return role;
    }

    // Récupérer les permissions d'un rôle
    public Set<Permission> getPermissionsForRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Rôle non trouvé"));
        return role.getPermissions();
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
    
}
