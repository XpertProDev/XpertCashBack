package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.NoteFactureProForma;

@Repository
public interface NoteFactureProFormaRepository extends JpaRepository<NoteFactureProForma, Long>{
     List<NoteFactureProForma> findByFacture(FactureProForma facture);


}
