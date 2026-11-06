package com.datashare.datashare_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.datashare.datashare_backend.entities.User;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    private JwtService jwtService;

    private User user;


    @BeforeEach
    void setUp() {

        jwtService =
                new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secretKey",
                "VGhpc0lzQVN1ZmZpY2llbnRseUxvbmdTZWNyZXRLZXlGb3JKV1RUZXN0cw=="
        );


        user =
                new User();

        user.setEmail("designanguy@gmail.com");
        user.setPassword("password");
    }


    /**
     * Vérifie qu'un token JWT est généré.
     */
    @Test
    public void test_generate_token() {

        // WHEN
        String token =
                jwtService.generateToken(user);


        // THEN
        assertNotNull(token);

        assertFalse(
                token.isBlank()
        );
    }


    /**
     * Vérifie que le email peut être
     * extrait du JWT.
     */
    @Test
    public void test_extract_username() {

        // GIVEN
        String token =
                jwtService.generateToken(user);


        // WHEN
        String email =
                jwtService.extractUsername(token);


        // THEN
        assertEquals(
                "designanguy@gmail.com",
                email
        );
    }


    /**
     * Vérifie qu'un JWT valide appartient
     * bien à l'utilisateur.
     */
    @Test
    public void test_token_is_valid() {

        // GIVEN
        String token =
                jwtService.generateToken(user);


        // WHEN
        boolean valid =
                jwtService.isTokenValid(
                        token,
                        user
                );


        // THEN
        assertTrue(valid);
    }


    /**
     * Vérifie qu'un JWT n'est pas valide
     * pour un autre utilisateur.
     */
    @Test
    public void test_token_is_invalid_for_other_user() {

        // GIVEN
        String token =
                jwtService.generateToken(user);

        User other =
                new User();

        other.setEmail("other@gmail.com");


        // WHEN
        boolean valid =
                jwtService.isTokenValid(
                        token,
                        other
                );


        // THEN
        assertFalse(valid);
    }
}
