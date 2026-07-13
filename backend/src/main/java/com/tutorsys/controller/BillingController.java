package com.tutorsys.controller;

import com.tutorsys.dto.InvoiceDto;
import com.tutorsys.service.BillingService;
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
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
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
    public ResponseEntity<InvoiceDto> getInvoiceById(@PathVariable Long id, Authentication authentication) {
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
    public ResponseEntity<InvoiceDto> generateInvoice(
            @RequestParam Long parentId,
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return ResponseEntity.ok(billingService.generateInvoice(parentId, start, end));
    }

    @PostMapping("/{id}/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InvoiceDto> regenerateInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.regenerateInvoice(id));
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InvoiceDto> finalizeInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.finalizeAndSendInvoice(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadInvoicePdf(@PathVariable Long id, Authentication authentication) {
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
