package com.temple.erp.config;

import com.temple.erp.repository.UserRepository;
import com.temple.erp.security.AuthPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {
    private final UserRepository userRepository;

    public GlobalModelAttributes(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addAuthAttributes(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AuthPrincipal p) {
            if (!model.containsAttribute("principal")) {
                model.addAttribute("principal", p);
            }
            if (!p.isAdmin() && !model.containsAttribute("user")) {
                try {
                    userRepository.findById(p.getId()).ifPresent(u -> model.addAttribute("user", u));
                } catch (Exception ignored) {
                    // keep pages rendering even if user lookup fails
                }
            }
        }
    }
}
