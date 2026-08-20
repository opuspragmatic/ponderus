package io.pragmatic.ponderus.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Levée quand une ressource n'existe pas OU n'appartient pas à
 * l'utilisateur authentifié : dans les deux cas on renvoie 404, pour
 * ne pas révéler l'existence de ressources d'autres comptes.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
