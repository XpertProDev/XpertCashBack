package com.xpertcash.configuration;

import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import java.util.function.Function;

@Component
public class JwtUtil {

    private final JwtConfig jwtConfig;

    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    // Méthode pour extraire l'UUID de l'utilisateur depuis le JWT
    public String extractUserUuid(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (ExpiredJwtException e) {
            System.out.println(" Token expiré : impossible d'extraire l'UUID utilisateur.");
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println(" Token invalide ou mal formé : " + e.getMessage());
            return null;
        }
    }

    // Méthode LEGACY pour extraire l'ID de l'utilisateur depuis le JWT (pour compatibilité)
    @Deprecated
    public Long extractUserId(String token) {
        try {
            return Long.parseLong(extractClaim(token, Claims::getSubject));
        } catch (ExpiredJwtException e) {
            System.out.println(" Token expiré");
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println(" Token invalide : " + e.getMessage());
            return null;
        }
    }

    public Claims extractAllClaimsSafe(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(jwtConfig.getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println(" Erreur lors de l'extraction des claims : " + e.getMessage());
            return null;
        }
    }


    // Méthode générique pour extraire une information spécifique du token
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Méthode pour extraire toutes les informations du token (claims)
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtConfig.getSecretKey()) 
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
