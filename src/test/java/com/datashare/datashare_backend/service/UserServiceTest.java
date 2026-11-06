package com.datashare.datashare_backend.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.datashare.datashare_backend.entities.User;
import com.datashare.datashare_backend.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    private static final String EMAIL = "designanguy@gmail.com";
    private static final String PASSWORD = "password";
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private UserService userService;

    @Test
    public void test_create_null_user_throws_IllegalArgumentException() {
        // GIVEN

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.register(null));
    }

    @Test
    public void test_create_already_exist_user_throws_IllegalArgumentException() {
        // GIVEN
        User user = new User();
        user.setEmail(EMAIL);
        user.setPassword(PASSWORD);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

        // THEN
        Assertions.assertThrows(ResponseStatusException.class,
                () -> userService.register(user));
    }

    @Test
    public void test_create_user() {
        // GIVEN
        User user = new User();
        user.setEmail(EMAIL);
        user.setPassword(PASSWORD);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD);
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // WHEN
        userService.register(user);

        // THEN
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue()).isEqualTo(user);
    }

    /**
     * Vérifie qu'un utilisateur existant
     * peut être récupéré par son identifiant.
     */



    /**
     * Vérifie qu'un login valide retourne un JWT.
     */
    @Test
    public void test_login_user() {

        // GIVEN
        User user = new User();

        user.setEmail(EMAIL);
        user.setPassword(PASSWORD);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                anyString(),
                anyString()
        )).thenReturn(true);

        when(jwtService.generateToken(any()))
                .thenReturn("jwt-token");


        // WHEN
        String result =
                userService.login(
                        EMAIL,
                        PASSWORD
                );


        // THEN
        assertNotNull(result);

        assertEquals(
                "jwt-token",
                result
        );
    }

    /**
     * Vérifie qu'un mauvais mot de passe
     * est refusé.
     */
    @Test
    public void test_login_wrong_password_throws_BadCredentialsException() {
        // GIVEN
        User user = new User();
        user.setEmail(EMAIL);
        user.setPassword(PASSWORD);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", PASSWORD)).thenReturn(false);

        // THEN
        Assertions.assertThrows(BadCredentialsException.class,
                () -> userService.login(EMAIL, "wrong"));
        verify(jwtService, never()).generateToken(any());
    }

    /**
     * Vérifie qu'un utilisateur inconnu
     * est refusé.
     */
    @Test
    public void test_login_unknown_user_throws_BadCredentialsException() {
        // GIVEN
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // THEN
        Assertions.assertThrows(BadCredentialsException.class,
                () -> userService.login(EMAIL, PASSWORD));
    }

    @Test
    public void test_login_null_email_throws_IllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.login(null, PASSWORD));
    }

    @Test
    public void test_login_null_password_throws_IllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.login(EMAIL, null));
    }
}
