package com.xpertcash.composant;

import com.xpertcash.entity.User;
import com.xpertcash.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Migration pour ajouter des UUIDs aux utilisateurs existants qui n'en ont pas
 * Cette classe s'exécute au démarrage de l'application
 */
@Component
public class UserUuidMigration implements CommandLineRunner {

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔄 Début de la migration UUID pour les utilisateurs...");
        
        // Récupérer tous les utilisateurs qui n'ont pas d'UUID
        List<User> usersWithoutUuid = usersRepository.findAll()
                .stream()
                .filter(user -> user.getUuid() == null || user.getUuid().trim().isEmpty())
                .toList();
        
        if (usersWithoutUuid.isEmpty()) {
            System.out.println("✅ Aucun utilisateur sans UUID trouvé. Migration non nécessaire.");
            return;
        }
        
        System.out.println("📊 " + usersWithoutUuid.size() + " utilisateur(s) sans UUID trouvé(s).");
        
        int migratedCount = 0;
        for (User user : usersWithoutUuid) {
            try {
                // Générer un UUID unique
                String newUuid;
                do {
                    newUuid = UUID.randomUUID().toString();
                } while (usersRepository.findByUuid(newUuid).isPresent());
                
                user.setUuid(newUuid);
                usersRepository.save(user);
                migratedCount++;
                
                System.out.println("✅ UUID généré pour l'utilisateur: " + user.getEmail() + " -> " + newUuid);
                
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la migration de l'utilisateur " + user.getEmail() + ": " + e.getMessage());
            }
        }
        
        System.out.println("🎉 Migration terminée! " + migratedCount + "/" + usersWithoutUuid.size() + " utilisateur(s) migré(s).");
    }
}
