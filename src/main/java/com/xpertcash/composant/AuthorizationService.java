package com.xpertcash.composant;

import org.springframework.stereotype.Service;

import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.User;



@Service
public class AuthorizationService {


    public void checkPermission(User user, PermissionType requiredPermission) {
        if (user.getRole() == null) {
            throw new RuntimeException("Utilisateur sans rôle");
        }

        Role role = user.getRole();
        
        // Vérifier si le rôle contient la permission requise
        boolean hasPermission = role.getPermissions()
                .stream()
                .anyMatch(permission -> permission.getType().equals(requiredPermission));

        if (!hasPermission) {
            throw new RuntimeException("Vous n'avez pas la permission pour cette action.");
        }
    }
}
