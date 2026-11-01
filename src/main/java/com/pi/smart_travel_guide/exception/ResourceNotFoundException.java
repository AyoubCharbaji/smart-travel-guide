package com.pi.smart_travel_guide.exception;

/** Lancée quand une ressource demandée n'existe pas : elle sera convertie en HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);   // le message sera affiché dans la réponse JSON
    }
}