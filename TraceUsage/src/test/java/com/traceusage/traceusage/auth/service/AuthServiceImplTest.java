package com.traceusage.traceusage.auth.service;

import com.traceusage.traceusage.auth.dto.LoginRequest;
import com.traceusage.traceusage.auth.dto.RegisterRequest;
import com.traceusage.traceusage.auth.exception.DuplicateEmailException;
import com.traceusage.traceusage.auth.exception.InvalidCredentialsException;
import com.traceusage.traceusage.auth.security.AuthenticatedUser;
import com.traceusage.traceusage.auth.security.JwtService;
import com.traceusage.traceusage.user.entity.User;
import com.traceusage.traceusage.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_withValidRequest_shouldNormalizeHashAndSaveUser() {
        RegisterRequest request = new RegisterRequest(
                "  Dharaka  ", "  Dharaka@Example.com  ", "password");
        when(userRepository.existsByEmailIgnoreCase("dharaka@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("password-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(AuthenticatedUser.class))).thenReturn("jwt-token");

        var response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo("Dharaka");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("dharaka@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("password-hash");
        assertThat(response.email()).isEqualTo("dharaka@example.com");
        assertThat(response.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void register_withExistingEmail_shouldRejectRequest() {
        RegisterRequest request = new RegisterRequest(
                "Dharaka", "dharaka@example.com", "password");
        when(userRepository.existsByEmailIgnoreCase("dharaka@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_withValidCredentials_shouldReturnAccessToken() {
        User user = User.register("Dharaka", "dharaka@example.com", "password-hash");
        when(userRepository.findByEmailIgnoreCase("dharaka@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "password-hash")).thenReturn(true);
        when(jwtService.generateAccessToken(any(AuthenticatedUser.class))).thenReturn("jwt-token");

        var response = authService.login(
                new LoginRequest("Dharaka@Example.com", "password"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_withWrongPassword_shouldRejectRequest() {
        User user = User.register("Dharaka", "dharaka@example.com", "password-hash");
        when(userRepository.findByEmailIgnoreCase("dharaka@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "password-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("dharaka@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateAccessToken(any());
    }
}
