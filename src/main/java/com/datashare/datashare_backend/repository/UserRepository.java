package com.datashare.datashare_backend.repository;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.datashare.datashare_backend.entities.User;

import java.util.Optional;

@Repository
@NullMarked
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
