package com.boilerplate.infrastructure.security;

import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Email email;
        try {
            email = new Email(username);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return userRepository
                .findDetailedByEmail(email)
                .map(SecurityUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
