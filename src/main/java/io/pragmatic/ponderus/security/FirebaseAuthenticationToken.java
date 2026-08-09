package io.pragmatic.ponderus.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;

import io.pragmatic.ponderus.domain.User;

/**
 * Authentication dont le principal est directement notre User interne
 * (pas les claims bruts du Jwt), pour que les controllers récupèrent
 * l'entité métier sans requête supplémentaire.
 */
public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {
    private final User user;
    private final Jwt jwt;

    public FirebaseAuthenticationToken(User user, Jwt jwt) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.user = user;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt;
    }

    @Override
    public Object getPrincipal() {
        return user;
    }
}
