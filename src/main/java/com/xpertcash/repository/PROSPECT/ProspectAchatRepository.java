package com.xpertcash.repository.PROSPECT;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.PROSPECT.ProspectAchat;

@Repository
public interface ProspectAchatRepository extends JpaRepository<ProspectAchat, Long> {
    List<ProspectAchat> findByProspectId(Long prospectId);
    List<ProspectAchat> findByProspectIdOrderByDateAchatDesc(Long prospectId);
}
