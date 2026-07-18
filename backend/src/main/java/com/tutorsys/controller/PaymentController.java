package com.tutorsys.controller;

import com.tutorsys.dto.PaymentDto;
import com.tutorsys.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Allocations", description = "Endpoints for recording client payments and auditing chronological allocations")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(summary = "Get Payments List", description = "Retrieves recorded payments history. Admins get all records; Parents get only their family payment receipts.")
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
    @Operation(summary = "Record Client Payment", description = "Records a new client payment receipt and automatically triggers FIFO invoice balance allocations. Admin only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment recorded and allocated successfully")
    })
    public ResponseEntity<PaymentDto> recordPayment(@RequestBody PaymentDto dto) {
        return ResponseEntity.ok(paymentService.recordPayment(dto));
    }
}
