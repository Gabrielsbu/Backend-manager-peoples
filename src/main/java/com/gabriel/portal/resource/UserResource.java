package com.gabriel.portal.resource;

import com.gabriel.portal.domain.User;
import com.gabriel.portal.domain.UserPrincipal;
import com.gabriel.portal.exception.domain.EmailExistException;
import com.gabriel.portal.exception.domain.ExceptionHandling;
import com.gabriel.portal.exception.domain.UserNotFoundException;
import com.gabriel.portal.exception.domain.UsernameExistException;
import com.gabriel.portal.service.UserService;
import com.gabriel.portal.utilities.JWTTokenProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.util.List;

import static com.gabriel.portal.constant.SecurityConstant.JWT_TOKEN_HEADER;

@AllArgsConstructor
@NoArgsConstructor
@Data
@RestController
@RequestMapping(path = { "/", "/user"})
public class UserResource extends ExceptionHandling {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user){
        authenticate(user.getUsername(), user.getPassword());
        User loginUser = userService.findUserByUsername(user.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);

        return new ResponseEntity<>(loginUser, jwtHeader, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException {
        User newUser = userService.register(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());

        return new ResponseEntity<>(newUser, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> buscar(){
        List<User> users = userService.getUsers();

        return ResponseEntity.ok(users);
    }

    @GetMapping("/search-name/{name}")
    public ResponseEntity<User> buscar(@PathVariable String name){
        User user = userService.findUserByUsername(name);

        return ResponseEntity.ok(user);
    }

    public HttpHeaders getJwtHeader(UserPrincipal user){
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(user));
        return headers;
    }

    public void authenticate(String username, String password){
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
}
