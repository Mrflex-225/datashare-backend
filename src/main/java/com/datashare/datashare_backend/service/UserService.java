package com.datashare.datashare_backend.service;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.datashare.datashare_backend.entities.User;
import com.datashare.datashare_backend.repository.UserRepository;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(User user) {
        Assert.notNull(user, "User must not be null");
        log.info("Registering new user");

        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());
        if (optionalUser.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"User with login " + user.getEmail() + " already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public String login(String email, String password) {
        Assert.notNull(email, "Login must not be null");
        Assert.notNull(password, "Password must not be null");
        Optional<User> user = userRepository.findByEmail(email);
        User user1 = null;
        if(user.isPresent()) {
            user1 = user.get();
        }
        if (user.isPresent() && passwordEncoder.matches(password, user1.getPassword())) {
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(email)
                    .password(user1.getPassword())
                    .authorities(user1.getAuthorities())
                    .build();

            return jwtService.generateToken(userDetails);
        } else {
            throw new BadCredentialsException("Invalid credentials");
        }
    }


}
