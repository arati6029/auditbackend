package com.example.demo.repository;

import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;

import java.util.List;
import java.util.Optional;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole(User.Role role);

}
