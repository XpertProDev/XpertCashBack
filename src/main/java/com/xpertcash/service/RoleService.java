package com.xpertcash.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.xpertcash.entity.Permission;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.RoleType;
import com.xpertcash.repository.PermissionRepository;
import com.xpertcash.repository.RoleRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @PostConstruct
    public void initRoles() {
        if (roleRepository.count() == 0) {
            System.out.println("nitialisation des rôles et permissions...");

            // 1️⃣ Ajouter toutes les permissions en base si elles n'existent pas
            for (PermissionType type : PermissionType.values()) {
                if (!permissionRepository.existsByType(type)) {
                    Permission permission = new Permission();
                    permission.setType(type);
                    permissionRepository.save(permission);
                }
            }

            // Récupérer toutes les permissions depuis la base
            List<Permission> allPermissions = permissionRepository.findAll();
            Map<PermissionType, Permission> permissionMap = allPermissions.stream()
                    .collect(Collectors.toMap(Permission::getType, p -> p));

            // 3️Créer les rôles et leur attribuer les permissions
            Role adminRole = new Role();
            adminRole.setName(RoleType.ADMIN);
            adminRole.setPermissions(Arrays.asList(
                permissionMap.get(PermissionType.GERER_PRODUITS),
                permissionMap.get(PermissionType.VENDRE_PRODUITS),
                permissionMap.get(PermissionType.VOIR_FLUX_COMPTABLE),
                permissionMap.get(PermissionType.APPROVISIONNER_STOCK),
                    permissionMap.get(PermissionType.GERER_MAGASINS)
            ));

            Role venteRole = new Role();
            venteRole.setName(RoleType.VENDEUR);
            venteRole.setPermissions(Collections.singletonList(
                permissionMap.get(PermissionType.VENDRE_PRODUITS)
            ));

            Role comptableRole = new Role();
            comptableRole.setName(RoleType.COMPTABLE);
            comptableRole.setPermissions(Arrays.asList(
                permissionMap.get(PermissionType.VOIR_FLUX_COMPTABLE),
                permissionMap.get(PermissionType.APPROVISIONNER_STOCK)
            ));

            // 4️⃣ Sauvegarder les rôles avec les permissions associées
            roleRepository.saveAll(Arrays.asList(adminRole, venteRole, comptableRole));

            System.out.println("✅ Rôles et permissions initialisés avec succès !");
        }
    }
}
