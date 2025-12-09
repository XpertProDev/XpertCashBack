package com.xpertcash.composant;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.xpertcash.configuration.QRCodeGenerator;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.RoleRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;

/**
 * Initialise automatiquement un compte SUPER_ADMIN au démarrage de l'application.
 *
 * Si un utilisateur avec l'email configuré existe déjà, rien n'est fait.
 */
@Component
public class SuperAdminInitializer implements CommandLineRunner {

    private static final String SUPER_ADMIN_EMAIL = "carterhedy5700@gmail.com";
    private static final String SUPER_ADMIN_PASSWORD = "password123";

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private ImageStorageService imageStorageService;

    @Override
    public void run(String... args) {
        // Ne rien faire si le super admin existe déjà
        if (usersRepository.findByEmail(SUPER_ADMIN_EMAIL).isPresent()) {
            System.out.println("✅ SUPER_ADMIN déjà présent, aucune initialisation nécessaire.");
            return;
        }

        System.out.println("🚀 Initialisation du compte SUPER_ADMIN...");

        // Rôle SUPER_ADMIN : on le crée s'il n'existe pas encore
        Role superAdminRole = roleRepository.findFirstByName(RoleType.SUPER_ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleType.SUPER_ADMIN);
                    // Pas de configuration de permissions ici : tu peux les gérer via ton système de rôles
                    return roleRepository.save(role);
                });

        // Créer ou récupérer une entreprise spéciale pour le super admin
        String nomEntreprise = "Tchakeda Super Admin";
        Entreprise superAdminEntreprise = entrepriseRepository.findByNomEntreprise(nomEntreprise)
                .orElseGet(() -> {
                    Entreprise e = new Entreprise();
                    e.setNomEntreprise(nomEntreprise);
                    // Générer un identifiant unique
                    String identifiantUnique;
                    do {
                        identifiantUnique = Entreprise.generateIdentifiantEntreprise();
                    } while (entrepriseRepository.existsByIdentifiantEntreprise(identifiantUnique));
                    e.setIdentifiantEntreprise(identifiantUnique);
                    e.setCreatedAt(LocalDateTime.now());
                    e.setAdresse("");
                    e.setTelephone("");
                    e.setPays("");
                    e.setSecteur("");
                    e.setEmail("");
                    e.setLogo("");
                    e.setSiege("Ville");
                    e.setActive(true);
                    //codeqr
                    return entrepriseRepository.save(e);
                });

        // Générer un code PIN personnel unique (comme dans l'inscription)
        String personalCode;
        boolean isUnique;
        Random random = new Random();
        do {
            personalCode = String.format("%04d", random.nextInt(10000));
            isUnique = !usersRepository.existsByPersonalCode(personalCode);
        } while (!isUnique);

        // Créer l'utilisateur SUPER_ADMIN
        User superAdmin = new User();
        superAdmin.setUuid(UUID.randomUUID().toString());
        superAdmin.setPersonalCode(personalCode);
        superAdmin.setNomComplet("Super Admin");
        superAdmin.setEmail(SUPER_ADMIN_EMAIL);
        superAdmin.setPassword(passwordEncoder.encode(SUPER_ADMIN_PASSWORD));
        // Numéro de téléphone technique, juste pour respecter la contrainte de non-null/unique
        superAdmin.setPhone("0000000000");
        superAdmin.setPays("ML");
        superAdmin.setCreatedAt(LocalDateTime.now());
        superAdmin.setActivatedLien(true);
        superAdmin.setEnabledLien(true);
        superAdmin.setLocked(false);
        superAdmin.setEntreprise(superAdminEntreprise);
        superAdmin.setRole(superAdminRole);

        // Générer le QR code pour le SUPER_ADMIN (même logique que pour registerUsers)
        try {
            String qrContent = personalCode;
            byte[] qrCodeBytes = QRCodeGenerator.generateQRCode(qrContent, 200, 200);

            String fileName = UUID.randomUUID().toString();
            String qrCodeUrl = imageStorageService.saveQrCodeImage(qrCodeBytes, fileName);

            superAdmin.setQrCodeUrl(qrCodeUrl);
        } catch (Exception e) {
            System.err.println("Erreur génération QR Code SUPER_ADMIN: " + e.getMessage());
        }

        // Sauvegarder le super admin
        superAdmin = usersRepository.save(superAdmin);

        // S'assurer que l'entreprise a bien un admin (pour éviter les NullPointer plus tard)
        if (superAdminEntreprise.getAdmin() == null) {
            superAdminEntreprise.setAdmin(superAdmin);
            entrepriseRepository.save(superAdminEntreprise);
        }

        System.out.println("✅ Compte SUPER_ADMIN initialisé avec succès : " + SUPER_ADMIN_EMAIL);
    }
}


