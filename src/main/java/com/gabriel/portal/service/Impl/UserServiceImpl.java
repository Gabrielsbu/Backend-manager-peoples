package com.gabriel.portal.service.Impl;

import com.gabriel.portal.domain.User;
import com.gabriel.portal.domain.UserPrincipal;
import com.gabriel.portal.enumeration.Role;
import com.gabriel.portal.exception.domain.EmailExistException;
import com.gabriel.portal.exception.domain.EmailNotFoundException;
import com.gabriel.portal.exception.domain.UserNotFoundException;
import com.gabriel.portal.exception.domain.UsernameExistException;
import com.gabriel.portal.repository.UserRepository;
import com.gabriel.portal.service.EmailService;
import com.gabriel.portal.service.LoginAttemptService;
import com.gabriel.portal.service.UserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import static com.gabriel.portal.constant.FileConstant.*;
import static com.gabriel.portal.constant.UserImpl.NO_USER_FOUND_BY_EMAIL;
import static com.gabriel.portal.constant.UserImpl.NO_USER_FOUND_BY_USERNAME;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@Transactional
@Qualifier("UserDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {
    private Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private EmailService emailService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User usuario = userRepository.findUserByUsername(username);

        if(usuario == null) {
            LOGGER.error("User not found by username: " + username);
            throw new UsernameNotFoundException("User not found by username: " + username);
        } else {
            validateLoginAttempt(usuario);
            usuario.setLastLoginDateDisplay(usuario.getLastLoginDate());
            usuario.setLastLoginDate(new Date());
            userRepository.save(usuario);

            UserPrincipal userPrincipal = new UserPrincipal(usuario);
            LOGGER.info("Returning found user by username: " + username);
            return userPrincipal;
        }
    }

    private void validateLoginAttempt(User user) {
        if(user.isNotLocked()) {
            if(loginAttemptService.hasExceededMaxAttempts(user.getUsername())) {
                user.setNotLocked(false);
            } else {
                user.setNotLocked(true);
            }
        } else {
            loginAttemptService.evictUserToLoginAttemptCache(user.getUsername());
        }
    }

    @Override
    public User register(String firstName, String lastName, String username, String email) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException {
        validateNewUsernmameAndEmail(StringUtils.EMPTY, username, email);

        User user = new User();
        user.setUserId(generateUserId());
        String password = generatePassword();
        String encodedPassword = passwordEncoder.encode(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodedPassword);
        user.setActive(true);
        user.setNotLocked(true);
        user.setRole(Role.ROLE_USER.name());
        user.setAuthorities(Role.ROLE_USER.getAuthorities());
        user.setProfileImageUrl(getTemporaryProfileImageUrl(username));
        userRepository.save(user);
//        LOGGER.info("New user password: " + password);
        emailService.sendNewPasswordEmail(firstName, password, email);
        return user;
    }

    private String getTemporaryProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/image/profile/" + username).toUriString();
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    public User validateNewUsernmameAndEmail(String currentUsername, String newUsername, String newEmail) throws UserNotFoundException, UsernameExistException, EmailExistException {
        User userByNewUsername = findUserByUsername(newUsername);
        User userByNewEmail = findUserByEmail(newEmail);

        if(StringUtils.isNotBlank(currentUsername)){
            User currentUser = findUserByUsername(currentUsername);

            if(currentUser == null) {
                throw new UserNotFoundException("No user found by Username: " + currentUsername);
            }
            if(userByNewUsername != null && !currentUser.getId().equals(userByNewUsername.getId())){
                throw new UsernameExistException("Username already exist");
            }
            if(userByNewEmail != null && !currentUser.getId().equals(userByNewEmail.getId())){
                throw new EmailExistException("Email already exist");
            }

            return currentUser;

        } else {
            if(userByNewUsername != null){
                throw new UsernameExistException("Username already exist");
            }
            if(userByNewEmail != null){
                throw new EmailExistException("Email already exist");
            }
            return null;
        }
    }


    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    @Override
    public User addNewUser(String firstName, String lastName, String username, String email, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, EmailExistException, UsernameExistException, IOException, MessagingException {
        validateNewUsernmameAndEmail(StringUtils.EMPTY, username, email);

        User user = new User();
        String password = generatePassword();
        String pass = passwordEncoder.encode(password);
        user.setUserId(generateUserId());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setJoinDate(new Date());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(pass);
        user.setActive(isActive);
        user.setNotLocked(isNonLocked);
        user.setRole(getRoleEnumName(role).name());
        user.setAuthorities(getRoleEnumName(role).getAuthorities());
        user.setProfileImageUrl(getTemporaryProfileImageUrl(username));
        userRepository.save(user);
        saveProfileImage(user, profileImage);

        emailService.sendNewPasswordEmail(firstName, password, email);

        return user;
    }

    private void saveProfileImage(User user, MultipartFile profileImage) throws IOException {
        if(profileImage != null) {
            Path userFolder = Paths.get(USER_FOLDER + user.getUsername()).toAbsolutePath().normalize();

            if(!Files.exists(userFolder)){
                Files.createDirectories(userFolder);
                LOGGER.info(DIRECTORY_CREATED + userFolder);
            }

            Files.deleteIfExists(Paths.get(userFolder + user.getUsername() + DOT + JPG_EXTENSION));
            Files.copy(profileImage.getInputStream(), userFolder.resolve(user.getUsername() + DOT + JPG_EXTENSION), REPLACE_EXISTING);
            user.setProfileImageUrl(setProfileImageUrl(user.getUsername()));
            userRepository.save(user);
            LOGGER.info(FILE_SAVED_IN_FILE_SYSTEM + profileImage.getOriginalFilename());
        }
    }

    private String setProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(USER_IMAGE_PATH + username + FORGWARD_SLASH + username + DOT + JPG_EXTENSION).toUriString();
    }

    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }

    @Override
    public User updateUser(String currentUsername, String newFirstName, String newLastName, String newUsername, String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, EmailExistException, UsernameExistException, IOException {
        User userCurrent = validateNewUsernmameAndEmail(currentUsername, newFirstName, newEmail);

        userCurrent.setFirstName(newFirstName);
        userCurrent.setLastName(newLastName);
        userCurrent.setJoinDate(new Date());
        userCurrent.setUsername(newUsername);
        userCurrent.setEmail(newEmail);
        userCurrent.setActive(isActive);
        userCurrent.setNotLocked(isNonLocked);
        userCurrent.setRole(getRoleEnumName(role).name());
        userCurrent.setAuthorities(getRoleEnumName(role).getAuthorities());
        userRepository.save(userCurrent);
        saveProfileImage(userCurrent, profileImage);

        return userCurrent;
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public void resetPassword(String email) throws EmailNotFoundException, MessagingException {
        User user = userRepository.findUserByEmail(email);
        if(user == null) {
            throw new EmailNotFoundException("No user found by email " + email);
        }
        String password = generatePassword();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        emailService.sendNewPasswordEmail(user.getFirstName(), password, user.getEmail());
    }

    @Override
    public User updateProfileImage(String username, MultipartFile profileImage) throws UserNotFoundException, EmailExistException, UsernameExistException, IOException {
        User user = validateNewUsernmameAndEmail(username, null, null);

        saveProfileImage(user, profileImage);
        return user;

    }
}
