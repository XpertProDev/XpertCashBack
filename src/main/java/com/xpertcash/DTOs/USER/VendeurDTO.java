package com.xpertcash.DTOs.USER;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendeurDTO {
    private Long id;
    private String nom;
    private String type;
    private List<BoutiqueSimpleDTO> boutiques;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoutiqueSimpleDTO {
        private Long id;
        private String nom;
    }
}
