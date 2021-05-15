package com.gabriel.portal.domain.repository;

import com.gabriel.portal.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    User findUserByUsername(String name);

    User findUserByEmail(String name);
}
