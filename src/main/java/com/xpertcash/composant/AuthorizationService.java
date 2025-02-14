package com.xpertcash.composant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.User;
import com.xpertcash.repository.RoleRepository;
import com.xpertcash.repository.UsersRepository;


@Service
public class AuthorizationService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RoleRepository roleRepository;

    public void checkPermission(User user, PermissionType requiredPermission) {
        if (user.getRole() == null) {
            throw new RuntimeException("Utilisateur sans rôle");
        }

        Role role = user.getRole();
        
        // Vérifier si le rôle contient la permission requise
        boolean hasPermission = role.getPermissions()
                .stream()
                .anyMatch(permission -> permission.getType().equals(requiredPermission));  // Comparer directement les types PermissionType

        if (!hasPermission) {
            throw new RuntimeException("Vous n'avez pas la permission pour cette action.");
        }
    }
}
