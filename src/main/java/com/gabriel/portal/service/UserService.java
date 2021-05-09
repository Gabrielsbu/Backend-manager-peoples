package com.gabriel.portal.service;

import com.gabriel.portal.domain.User;
import com.gabriel.portal.exception.domain.EmailExistException;
import com.gabriel.portal.exception.domain.UserNotFoundException;
import com.gabriel.portal.exception.domain.UsernameExistException;

import javax.mail.MessagingException;
import java.util.List;

public interface UserService {
    User register(String firstName, String lastName, String username, String email) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException;
    List<User> getUsers();
    User findUserByUsername(String username);
    User findUserByEmail(String email);

}
