-- Migration V1_17: Ajout du champ destinataire_id à note_facture_pro_forma
-- Permet d'assigner une note à un utilisateur spécifique de l'entreprise

ALTER TABLE note_facture_pro_forma ADD COLUMN destinataire_id BIGINT NULL;

ALTER TABLE note_facture_pro_forma ADD CONSTRAINT fk_note_destinataire 
    FOREIGN KEY (destinataire_id) REFERENCES user(id);

CREATE INDEX IF NOT EXISTS idx_note_facture_destinataire_id ON note_facture_pro_forma(destinataire_id);
