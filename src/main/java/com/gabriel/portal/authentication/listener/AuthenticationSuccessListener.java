package com.gabriel.portal.authentication.listener;

import com.gabriel.portal.domain.entities.User;
import com.gabriel.portal.domain.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessListener {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        if(principal instanceof User) {
            User user = (User) event.getAuthentication().getPrincipal();
            loginAttemptService.evictUserToLoginAttemptCache(user.getUsername());
        }
    }
}
