package com.tutorsys.controller;

import com.tutorsys.dto.PaymentDto;
import com.tutorsys.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentDto>> getPayments(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(paymentService.getAllPayments());
        } else {
            return ResponseEntity.ok(paymentService.getPaymentsByParentUsername(authentication.getName()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentDto> recordPayment(@RequestBody PaymentDto dto) {
        return ResponseEntity.ok(paymentService.recordPayment(dto));
    }
}
