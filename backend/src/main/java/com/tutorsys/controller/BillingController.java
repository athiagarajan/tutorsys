package com.tutorsys.controller;

import com.tutorsys.dto.InvoiceDto;
import com.tutorsys.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices & Billing", description = "Endpoints for managing billing invoices, calculating totals from sessions, generating PDFs, and finalizations")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    @Operation(summary = "Get Invoices List", description = "Retrieves all billing invoices. Admins get all records; Parents get only their family invoices.")
    public ResponseEntity<List<InvoiceDto>> getInvoices(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(billingService.getAllInvoices());
        } else {
            return ResponseEntity.ok(billingService.getInvoicesByParentUsername(authentication.getName()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Invoice Details by ID", description = "Retrieves a single invoice by its ID. Non-admin users are restricted to their own family invoices.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice details retrieved"),
        @ApiResponse(responseCode = "403", description = "Access denied to requested invoice"),
        @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceDto> getInvoiceById(
            @Parameter(description = "Invoice unique identifier", example = "101", required = true) @PathVariable Long id, 
            Authentication authentication
    ) {
        InvoiceDto dto = billingService.getInvoiceById(id);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !dto.getParentName().equalsIgnoreCase(authentication.getName()) && 
            !billingService.getInvoicesByParentUsername(authentication.getName()).stream()
                    .anyMatch(i -> i.getId().equals(id))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate Invoice", description = "Calculates session totals and generates a new draft invoice for a parent over a date range. Admin only.")
    public ResponseEntity<InvoiceDto> generateInvoice(
            @Parameter(description = "Parent database ID", example = "5", required = true) @RequestParam Long parentId,
            @Parameter(description = "Billing start date", example = "2026-06-01", required = true) @RequestParam String startDate,
            @Parameter(description = "Billing end date", example = "2026-06-30", required = true) @RequestParam String endDate
    ) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return ResponseEntity.ok(billingService.generateInvoice(parentId, start, end));
    }

    @PostMapping("/{id}/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Regenerate Draft Invoice", description = "Recalculates totals and rebuilds a draft invoice with updated sessions. Admin only.")
    public ResponseEntity<InvoiceDto> regenerateInvoice(
            @Parameter(description = "Invoice unique identifier", example = "101", required = true) @PathVariable Long id
    ) {
        return ResponseEntity.ok(billingService.regenerateInvoice(id));
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Finalize Invoice", description = "Freezes an invoice draft, marks it as SENT, and generates the PDF sheet for the parent. Admin only.")
    public ResponseEntity<InvoiceDto> finalizeInvoice(
            @Parameter(description = "Invoice unique identifier", example = "101", required = true) @PathVariable Long id
    ) {
        return ResponseEntity.ok(billingService.finalizeAndSendInvoice(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download Invoice PDF", description = "Downloads the generated PDF document file for a finalized invoice.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF binary document returned"),
        @ApiResponse(responseCode = "403", description = "Access denied to requested invoice"),
        @ApiResponse(responseCode = "404", description = "Invoice PDF file not found or not generated yet")
    })
    public ResponseEntity<Resource> downloadInvoicePdf(
            @Parameter(description = "Invoice unique identifier", example = "101", required = true) @PathVariable Long id, 
            Authentication authentication
    ) {
        InvoiceDto dto = billingService.getInvoiceById(id);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !dto.getParentName().equalsIgnoreCase(authentication.getName()) && 
            !billingService.getInvoicesByParentUsername(authentication.getName()).stream()
                    .anyMatch(i -> i.getId().equals(id))) {
            return ResponseEntity.status(403).build();
        }

        if (dto.getPdfFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(dto.getPdfFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
