package com.xpertcash.exceptions;

import org.eclipse.angus.mail.util.MailConnectException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

   // Gestion des erreurs de validation (MethodArgumentNotValidException)
   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
       Map<String, String> errors = new HashMap<>();
       
       // Récupérer tous les messages d'erreur de validation avec le nom du champ
       ex.getBindingResult().getFieldErrors().forEach(error -> {
           errors.put(error.getField(), error.getDefaultMessage());  // Ajoute le nom du champ et le message d'erreur
       });
       
       return ResponseEntity.badRequest().body(errors);  // Retourne un bad request avec les erreurs de validation
   }

   // Gestion des erreurs de connexion au serveur de messagerie
   @ExceptionHandler(MailConnectException.class)
   public ResponseEntity<Map<String, String>> handleMailConnectException(MailConnectException e) {
       // Retourne une réponse indiquant que le serveur de messagerie est inaccessible
       return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
               .body(Collections.singletonMap("error", "Erreur de connexion au serveur de messagerie. Veuillez réessayer plus tard."));
   }

   // Gestion des erreurs de type RuntimeException
   @ExceptionHandler(RuntimeException.class)
   public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
       // Retourne une réponse d'erreur détaillée pour RuntimeException
       Map<String, String> errorResponse = new HashMap<>();
       errorResponse.put("error", "Une erreur est survenue : " + e.getMessage());
       errorResponse.put("exception", e.getClass().getSimpleName());  // Ajoute le nom de l'exception pour plus de contexte
       return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
   }

   // Gestion des erreurs générales (toutes les autres exceptions non spécifiées)
   @ExceptionHandler(Exception.class)
   public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
       // Retourne une réponse générique d'erreur pour des exceptions inconnues
       Map<String, String> errorResponse = new HashMap<>();
       errorResponse.put("error", "Une erreur interne est survenue. Veuillez réessayer plus tard.");
       errorResponse.put("exception", e.getClass().getSimpleName());  // Fournit des détails sur l'exception
       return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
   }

   // Gestion des erreurs NoResourceFoundException (ressource non trouvée)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFoundException(NoResourceFoundException e) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Ressource non trouvée : " + e.getMessage());
        errorResponse.put("exception", e.getClass().getSimpleName());  // Détails sur l'exception
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
