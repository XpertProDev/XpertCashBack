ALTER TABLE ligne_facture_proforma 
    MODIFY COLUMN ligne_description TEXT;

ALTER TABLE ligne_facture_reelle 
    MODIFY COLUMN ligne_description TEXT;
 
 DESCRIBE ligne_facture_proforma;