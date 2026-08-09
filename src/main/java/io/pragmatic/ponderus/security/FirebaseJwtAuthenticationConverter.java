package io.pragmatic.ponderus.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.service.UserProvisioningService;

@Component
public class FirebaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    
    private final UserProvisioningService userProvisioningService;

    public FirebaseJwtAuthenticationConverter(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = userProvisioningService.findOrCreate(jwt);
        return new FirebaseAuthenticationToken(user, jwt);
    }
}
