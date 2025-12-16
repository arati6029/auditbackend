package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class UserController {


    @Autowired
    private UserService userService;

    @PostMapping("/user")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // Implementation for creating a user
       User savedUser= userService.createUser(user);
        return ResponseEntity.ok().body(savedUser);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        // Implementation for retrieving a user by ID
        User user= userService.getUserById(id);
        return ResponseEntity.ok().body(user);

    }
    @GetMapping("/user/byRole")
    public ResponseEntity<List<User>> getUserByRole(@RequestParam String role) {
        // Implementation for retrieving a user by role
        List<User> user= userService.getUserByRole(role);
        return ResponseEntity.ok().body(user);
    }

}
