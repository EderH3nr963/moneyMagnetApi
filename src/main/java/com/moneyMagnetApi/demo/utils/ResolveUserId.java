package com.moneyMagnetApi.demo.utils;

import com.moneyMagnetApi.demo.security.UsuarioDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class ResolveUserId {
    public static UUID resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioDetailsImpl details)) {
            return null;
        }
        
        return details.getId();
    }
}
