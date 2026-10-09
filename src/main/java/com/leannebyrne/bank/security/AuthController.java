package com.leannebyrne.bank.security;

import com.leannebyrne.bank.customer.Customer;
import com.leannebyrne.bank.customer.CustomerRepository;
import com.leannebyrne.bank.customer.Role;
import com.leannebyrne.bank.security.dto.AuthResponse;
import com.leannebyrne.bank.security.dto.LoginRequest;
import com.leannebyrne.bank.security.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(CustomerRepository customerRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        if (customerRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyInUseException(request.email());
        }

        Customer customer = new Customer(
                request.firstName(),
                request.lastName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.CUSTOMER);
        customer = customerRepository.save(customer);

        String token = jwtService.issue(customer.getId(), customer.getRole());
        return new AuthResponse(token, customer.getId(), customer.getRole());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.email())
                .filter(Customer::isEnabled)
                .filter(c -> passwordEncoder.matches(request.password(), c.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        String token = jwtService.issue(customer.getId(), customer.getRole());
        return new AuthResponse(token, customer.getId(), customer.getRole());
    }
}
