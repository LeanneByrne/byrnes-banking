package com.leannebyrne.bank.security;

import com.leannebyrne.bank.customer.Customer;
import com.leannebyrne.bank.customer.CustomerRepository;
import com.leannebyrne.bank.customer.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountInitializer(
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_EMAIL:}") String adminEmail,
            @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }
        if (customerRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }
        Customer admin = new Customer("Admin", "User", adminEmail, passwordEncoder.encode(adminPassword), Role.ADMIN);
        customerRepository.save(admin);
    }
}
