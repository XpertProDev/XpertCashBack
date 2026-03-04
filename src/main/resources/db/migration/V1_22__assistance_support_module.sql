-- Module d'assistance / support : tickets + messages

CREATE TABLE IF NOT EXISTS assistance_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_ticket VARCHAR(50) NOT NULL,
    sujet VARCHAR(255) NOT NULL,
    statut VARCHAR(20) NOT NULL,
    created_by_id BIGINT NOT NULL,
    entreprise_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    closed_at DATETIME NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT uk_assistance_ticket_numero UNIQUE (numero_ticket),
    CONSTRAINT fk_assistance_ticket_user FOREIGN KEY (created_by_id) REFERENCES `user`(id),
    CONSTRAINT fk_assistance_ticket_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprise(id),
    INDEX idx_assistance_ticket_entreprise (entreprise_id),
    INDEX idx_assistance_ticket_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS assistance_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    auteur_id BIGINT NOT NULL,
    contenu TEXT NOT NULL,
    piece_jointe_path VARCHAR(500) NULL,
    is_support TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_assistance_message_ticket FOREIGN KEY (ticket_id) REFERENCES assistance_ticket(id) ON DELETE CASCADE,
    CONSTRAINT fk_assistance_message_user FOREIGN KEY (auteur_id) REFERENCES `user`(id),
    INDEX idx_assistance_message_ticket (ticket_id),
    INDEX idx_assistance_message_auteur (auteur_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

