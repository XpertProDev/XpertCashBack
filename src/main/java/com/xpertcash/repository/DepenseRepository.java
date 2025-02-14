package com.xpertcash.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Depense;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;

@Repository
public interface DepenseRepository extends JpaRepository<Depense, Long> {
    List<Depense> findByUser(User user);
    List<Depense> findByDateBetween(Date startDate, Date endDate);
    List<Depense> findByEntreprise(Entreprise entreprise);
}

