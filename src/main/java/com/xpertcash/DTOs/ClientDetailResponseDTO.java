package com.xpertcash.DTOs;

import com.xpertcash.entity.Client;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Réponse du détail client : client + liste des dettes écart caisse à son compte. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientDetailResponseDTO {
    private Client client;
    private List<EcartCaisseItemDTO> ecarts;
}
