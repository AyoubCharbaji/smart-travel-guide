package com.pi.smart_travel_guide.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.jena.query.QueryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Gestion centralisée des erreurs : toutes les erreurs de l'API ont le même format JSON. */
@RestControllerAdvice
public class ApiExceptionHandler {

    // 404 : ressource introuvable
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 400 : paramètre invalide (ex. minStars=9 ou identifiant avec caractères interdits)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // 500 : erreur dans la requête SPARQL (on ne révèle pas les détails au client)
    @ExceptionHandler(QueryException.class)
    public ResponseEntity<Map<String, Object>> sparqlError(QueryException e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erreur lors de l'exécution de la requête SPARQL");
    }

    // Construit le corps JSON commun à toutes les erreurs
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}