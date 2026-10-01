package com.traceusage.traceusage.auth.service;

import com.traceusage.traceusage.auth.dto.LoginRequest;
import com.traceusage.traceusage.auth.dto.LoginResponse;
import com.traceusage.traceusage.auth.dto.RegisterRequest;
import com.traceusage.traceusage.auth.dto.RegisterResponse;
import com.traceusage.traceusage.auth.exception.DuplicateEmailException;
import com.traceusage.traceusage.auth.exception.InvalidCredentialsException;
import com.traceusage.traceusage.auth.security.AuthenticatedUser;
import com.traceusage.traceusage.auth.security.JwtService;
import com.traceusage.traceusage.user.entity.User;
import com.traceusage.traceusage.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }

        User user = User.register(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()));

        try {
            User savedUser = userRepository.save(user);
            String accessToken = jwtService.generateAccessToken(AuthenticatedUser.from(savedUser));
            return RegisterResponse.from(savedUser, accessToken);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(AuthenticatedUser.from(user));
        return new LoginResponse(accessToken);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
