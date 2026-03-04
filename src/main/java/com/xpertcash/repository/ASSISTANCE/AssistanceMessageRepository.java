package com.xpertcash.repository.ASSISTANCE;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.ASSISTANCE.AssistanceMessage;

@Repository
public interface AssistanceMessageRepository extends JpaRepository<AssistanceMessage, Long> {

    List<AssistanceMessage> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}

