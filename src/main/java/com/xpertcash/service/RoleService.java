package com.xpertcash.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Permission;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.repository.PermissionRepository;
import com.xpertcash.repository.RoleRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final JwtUtil jwtUtil;
    private final UsersRepository usersRepository;

    @PostConstruct
    public void initRoles() {
        if (roleRepository.count() == 0) {
            System.out.println("nitialisation des rôles et permissions...");

            // Ajouter toutes les permissions en base si elles n'existent pas
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
                permissionMap.get(PermissionType.GERER_CLIENTS),
                permissionMap.get(PermissionType.GERER_BOUTIQUE),
                permissionMap.get(PermissionType.Gestion_Facture),
                permissionMap.get(PermissionType.GERER_UTILISATEURS),
                permissionMap.get(PermissionType.APPROVISIONNER_STOCK)
            ));

            Role managerRole = new Role();
            managerRole.setName(RoleType.MANAGER);
            managerRole.setPermissions(Collections.singletonList(
                permissionMap.get(PermissionType.GERER_UTILISATEURS)

            ));

            Role venteRole = new Role();
            venteRole.setName(RoleType.VENDEUR);
            venteRole.setPermissions(Collections.singletonList(
                permissionMap.get(PermissionType.VENDRE_PRODUITS)
            ));

            Role gestionClient = new Role();
            gestionClient.setName(RoleType.Clientel);
            gestionClient.setPermissions(Collections.singletonList(
                 permissionMap.get(PermissionType.GERER_CLIENTS)
            ));

            Role rhRole = new Role();
            rhRole.setName(RoleType.RH);
            rhRole.setPermissions(Collections.singletonList(
                permissionMap.get(PermissionType.GERER_UTILISATEURS)
            ));

            /*  

            Role comptableRole = new Role();
            comptableRole.setName(RoleType.COMPTABLE);
            comptableRole.setPermissions(Arrays.asList(
                permissionMap.get(PermissionType.VOIR_FLUX_COMPTABLE),
                permissionMap.get(PermissionType.APPROVISIONNER_STOCK)
            ));
            */

            // 4️⃣ Sauvegarder les rôles avec les permissions associées
            roleRepository.saveAll(Arrays.asList(adminRole, venteRole, managerRole));

            System.out.println("✅ Rôles et permissions initialisés avec succès !");
        }
    }


    @Transactional
    public User updateUserRole(String token, Long userId, String newRoleName) {
        // Vérifier la présence du token JWT
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
    
        // Extraire le token sans le "Bearer "
        token = token.replace("Bearer ", "");
    
        Long adminId;
        try {
            // Décrypter le token pour obtenir l'ID de l'admin
            adminId = jwtUtil.extractUserId(token);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'admin depuis le token", e);
        }
    
        // Récupérer l'admin par l'ID extrait du token
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));
    
        // Vérifier que l'Admin est bien un ADMIN
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un ADMIN peut modifier des rôles !");
        }
    
        // Vérifier que l'Admin possède une entreprise
        if (admin.getEntreprise() == null) {
            throw new RuntimeException("L'Admin n'a pas d'entreprise associée.");
        }
    
        // Récupérer l'utilisateur à modifier
        User userToUpdate = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    
        // Vérifier que l'utilisateur appartient à la même entreprise que l'admin
        if (!userToUpdate.getEntreprise().equals(admin.getEntreprise())) {
            throw new RuntimeException("Cet utilisateur n'appartient pas à votre entreprise.");
        }
    
        // Convertir la String en RoleType
        RoleType roleType;
        try {
            roleType = RoleType.valueOf(newRoleName); 
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rôle invalide : " + newRoleName);
        }
    
        // Vérifier que le rôle existe dans la base de données
        Role newRole = roleRepository.findByName(roleType)
                .orElseThrow(() -> new RuntimeException("Rôle non trouvé : " + newRoleName));
    
        // Mettre à jour le rôle de l'utilisateur
        userToUpdate.setRole(newRole);
    
        // Sauvegarder l'utilisateur avec son nouveau rôle
        return usersRepository.save(userToUpdate);
    }
    
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}
