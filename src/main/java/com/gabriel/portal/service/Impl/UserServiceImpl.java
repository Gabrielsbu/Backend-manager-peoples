package com.gabriel.portal.service.Impl;

import com.gabriel.portal.domain.User;
import com.gabriel.portal.domain.UserPrincipal;
import com.gabriel.portal.repository.UserRepository;
import com.gabriel.portal.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {
    private Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User usuario = userRepository.findByUsername(username);

        if(usuario == null) {
            LOGGER.error("User not found by username: " + username);
            throw new UsernameNotFoundException("User not found by username: " + username);
        } else {
            usuario.setLastLoginDateDisplay(usuario.getLastLoginDate());
            usuario.setLastLoginDate(new Date());
            userRepository.save(usuario);

            UserPrincipal userPrincipal = new UserPrincipal(usuario);
            LOGGER.info("Returning found user by username: " + username);
            return userPrincipal;
        }
    }
}
