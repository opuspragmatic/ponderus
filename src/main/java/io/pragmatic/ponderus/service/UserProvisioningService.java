package io.pragmatic.ponderus.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.pragmatic.ponderus.domain.Plan;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.repository.UserRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserProvisioningService {

    private final UserRepository userRepository;
    
    /**
     * Retrouve l'utilisateur interne correspondant au token Firebase,
     * ou le crée à la volée (JIT provisioning) si c'est sa première requête.
     */
    @Transactional
    public User findOrCreate(Jwt jwt) {
        String firebaseUid = jwt.getSubject();

        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> createUser(jwt, firebaseUid));
    }

    private User createUser(Jwt jwt, String firebaseUid) {
        User user = new User();
        user.setFirebaseUid(firebaseUid);
        user.setEmail(jwt.getClaimAsString("email"));
        user.setPlan(Plan.FREE);

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Deux requêtes concurrentes ont déclenché la création en même temps :
            // la contrainte unique sur firebase_uid a bloqué la seconde.
            // On récupère simplement celui créé entre-temps par la première.
            return userRepository.findByFirebaseUid(firebaseUid)
                    .orElseThrow();
        }
    }
}