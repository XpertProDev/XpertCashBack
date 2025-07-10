package com.xpertcash.configuration;

import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;

public class CentralAccess {

    public static boolean isAdminOrManagerOfEntreprise(User user, Long targetEntrepriseId) {
        if (user == null || user.getEntreprise() == null || targetEntrepriseId == null) {
            return false;
        }

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;

        return isAdminOrManager && user.getEntreprise().getId().equals(targetEntrepriseId);
    }

    public static boolean isSelfOrAdminOrManager(User user, Long targetUserId) {
        if (user == null || targetUserId == null) {
            return false;
        }

        boolean isSelf = user.getId().equals(targetUserId);
        return isSelf || isAdminOrManagerOfEntreprise(user, user.getEntreprise().getId());
    }
}
