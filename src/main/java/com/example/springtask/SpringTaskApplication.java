package com.example.springtask;

import com.example.springtask.domain.security.Role;
import com.example.springtask.domain.security.User;
import com.example.springtask.repos.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
public class SpringTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringTaskApplication.class, args);
    }

    private final UserRepository userRepository;

    public SpringTaskApplication(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public CommandLineRunner createAdmin() {
        return (args) -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin");
            admin.setActive(true);

            Set<Role> roles = new HashSet<>();
            roles.add(Role.ADMIN);
            roles.add(Role.USER);
            admin.setRoles(roles);

            userRepository.save(admin);
        };
    }

    @Bean
    public CommandLineRunner createUser() {
        return (args) -> {
            User user = new User();
            user.setUsername("user");
            user.setPassword("user");
            user.setActive(true);

            Set<Role> roles = new HashSet<>();
            roles.add(Role.USER);
            user.setRoles(roles);

            userRepository.save(user);
        };
    }
}
