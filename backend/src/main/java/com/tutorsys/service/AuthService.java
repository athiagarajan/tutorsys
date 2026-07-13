package com.tutorsys.service;

import com.tutorsys.dto.AuthRequest;
import com.tutorsys.dto.AuthResponse;
import com.tutorsys.entity.Parent;
import com.tutorsys.entity.Role;
import com.tutorsys.entity.User;
import com.tutorsys.repository.ParentRepository;
import com.tutorsys.repository.UserRepository;
import com.tutorsys.security.JwtUtils;
import com.tutorsys.security.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final ParentRepository parentRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       ParentRepository parentRepository, PasswordEncoder encoder, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.parentRepository = parentRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    public AuthResponse authenticateUser(AuthRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken(authentication.getName());

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        return new AuthResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), role);
    }

    @Transactional
    public User registerParentUser(String username, String password, String email, String parentName, String phone, String address, String preferredComm) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        // Create user account
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setEmail(email);
        user.setRole(Role.PARENT);
        user.setActive(true);
        user = userRepository.save(user);

        // Create parent profile
        Parent parent = new Parent();
        parent.setUser(user);
        parent.setName(parentName);
        parent.setEmail(email);
        parent.setPhone(phone);
        parent.setAddress(address);
        parent.setPreferredCommunication(preferredComm);
        parentRepository.save(parent);

        return user;
    }
}
