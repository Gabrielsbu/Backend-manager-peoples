package com.gabriel.portal.resource;

import com.gabriel.portal.exception.domain.EmailExistException;
import com.gabriel.portal.exception.domain.ExceptionHandling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = { "/", "/user"})
public class UserResource extends ExceptionHandling {

    @GetMapping("/home")
    public String showUser() throws EmailExistException{
        throw new EmailExistException("This email address is already taken");
    }
}
