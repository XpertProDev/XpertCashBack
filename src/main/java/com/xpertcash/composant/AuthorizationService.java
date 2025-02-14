package com.xpertcash.composant;

import org.springframework.stereotype.Service;

import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.User;

@Service
public class AuthorizationService {

    public void checkPermission(User user, PermissionType permission) {
        if (!user.getRole().getPermissions().contains(permission)) {
            throw new RuntimeException("Vous n'avez pas la permission pour cette action.");
        }
    }
}
