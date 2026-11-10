package com.temple.erp.security;

import com.temple.erp.model.User;
import com.temple.erp.repository.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TempleUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;
    private final PasswordEncoder passwordEncoder;

    public TempleUserDetailsService(UserRepository userRepository,
                                     @Value("${app.admin.username:Admin123}") String adminUsername,
                                     @Value("${app.admin.email:templeerp2027@gmail.com}") String adminEmail,
                                     @Value("${app.admin.password}") String adminPassword,
                                     PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.passwordEncoder = passwordEncoder;
    }

    private boolean isHardcodedAdmin(String username) {
        return username != null && (adminEmail.equalsIgnoreCase(username) || adminUsername.equalsIgnoreCase(username));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Allow web login with hardcoded admin username or email
        if (isHardcodedAdmin(username)) {
            return new AuthPrincipal("admin", adminUsername, adminEmail, passwordEncoder.encode(adminPassword), true, true,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")));
        }
        User user = userRepository.findByEmailOrUsername(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        boolean admin = "admin".equalsIgnoreCase(user.getRole());
        List<SimpleGrantedAuthority> auths = admin
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new AuthPrincipal(user.getId(), user.getName(), user.getEmail(), user.getPassword(), admin,
                "active".equalsIgnoreCase(user.getStatus()), auths);
    }

    public AuthPrincipal principalForUser(User user) {
        boolean admin = "admin".equalsIgnoreCase(user.getRole());
        List<SimpleGrantedAuthority> auths = admin
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new AuthPrincipal(user.getId(), user.getName(), user.getEmail(), user.getPassword(), admin,
                "active".equalsIgnoreCase(user.getStatus()), auths);
    }

    public AuthPrincipal hardcodedAdminPrincipal() {
        return new AuthPrincipal("admin", adminUsername, adminEmail, passwordEncoder.encode(adminPassword), true, true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")));
    }
}
