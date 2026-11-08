package com.boilerplate.infrastructure.security;

import com.boilerplate.domain.auth.User;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security's view of a {@link User}. Keeps the {@code UserDetails} contract out of
 * the domain (ARCHITECTURE.md seam 2). Authorities are materialised in the constructor so
 * no lazy association is touched after the persistence context closes — callers must load
 * the {@code User} with roles and permissions already fetched.
 */
public final class SecurityUser implements UserDetails {

    private final transient User user;
    private final List<GrantedAuthority> authorities;

    public SecurityUser(User user) {
        this.user = user;
        this.authorities = user.getRoles().stream()
                .flatMap(role -> Stream.concat(
                        Stream.of(new SimpleGrantedAuthority("ROLE_" + role.getName())),
                        role.getPermissions().stream().map(p -> new SimpleGrantedAuthority(p.getName()))))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    public User domainUser() {
        return user;
    }

    public UUID id() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash().value();
    }

    @Override
    public String getUsername() {
        return user.getEmail().value();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
