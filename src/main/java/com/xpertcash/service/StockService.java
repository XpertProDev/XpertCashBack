package com.xpertcash.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Stock;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.UsersRepository;

@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;

        @Autowired
    private UsersRepository usersRepository;


    
            //Methode pour augmenter la quantiter du stock
                public void ajouterQuantite(Long produitId, int quantiteAjoutee) {
                Stock stock = stockRepository.findByProduitId(produitId)
                        .orElseThrow(() -> new RuntimeException("Produit non trouvé dans le stock"));
                stock.setQuantite(stock.getQuantite() + quantiteAjoutee);
                stockRepository.save(stock);
        }


             //Methode pour definir la date dexpiration
                public void mettreAJourExpiration(Long produitId, LocalDate dateExpiration) {
                Stock stock = stockRepository.findByProduitId(produitId)
                        .orElseThrow(() -> new RuntimeException("Produit non trouvé dans le stock"));

                stock.setDateExpiration(dateExpiration);
                stockRepository.save(stock);
        }


        // Méthode pour récupérer tout le stock
        public List<Stock> recupererToutLeStock() {
            return stockRepository.findAll();
        }


    

}
