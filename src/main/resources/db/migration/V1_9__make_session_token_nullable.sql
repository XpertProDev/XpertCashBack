-- Migration pour rendre session_token nullable
-- Permet de créer la session sans token, puis de le mettre à jour après avoir obtenu l'ID

ALTER TABLE user_sessions 
    MODIFY COLUMN session_token VARCHAR(500) NULL;

-- Note: L'index unique reste, mais NULL est autorisé (plusieurs NULL sont autorisés dans MySQL avec UNIQUE)

