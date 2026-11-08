package com.boilerplate.infrastructure.security;

import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class ApiClientAuthentication implements Authentication {

    private final String clientId;
    private final List<GrantedAuthority> authorities;

    public ApiClientAuthentication(String clientId, List<String> scopes) {
        this.clientId = clientId;
        this.authorities = scopes.stream()
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    @Nullable
    public Object getCredentials() {
        return null;
    }

    @Override
    @Nullable
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return clientId;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean b) {}

    @Override
    public String getName() {
        return clientId;
    }
}
