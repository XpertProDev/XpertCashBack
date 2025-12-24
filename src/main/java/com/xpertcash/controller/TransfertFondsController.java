package com.xpertcash.controller;

import com.xpertcash.service.TransfertFondsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/auth")
public class TransfertFondsController {

    @Autowired
    private TransfertFondsService transfertFondsService;

    @PostMapping(value = "/transfert-fonds", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> effectuerTransfert(
            @RequestParam("transfert") String transfertJson,
            @RequestParam(value = "pieceJointe", required = false) MultipartFile pieceJointeFile,
            HttpServletRequest httpRequest) {
        return handleRequest(() ->
                transfertFondsService.effectuerTransfertMultipart(transfertJson, pieceJointeFile, httpRequest)
        );
    }

    @GetMapping("/transfert-fonds")
    public ResponseEntity<?> listerTransferts(HttpServletRequest httpRequest) {
        return handleRequest(() -> transfertFondsService.listerTransferts(httpRequest));
    }

    private ResponseEntity<?> handleRequest(Supplier<Object> supplier) {
        try {
            return ResponseEntity.ok(supplier.get());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Erreur interne du serveur : " + e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}

