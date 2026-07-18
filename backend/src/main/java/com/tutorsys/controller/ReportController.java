package com.tutorsys.controller;

import com.tutorsys.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Reports & Analytics", description = "Endpoints for retrieving admin dashboard statistics and exporting Excel sheets. Admin only.")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get Dashboard Stats", description = "Fetches key business indicators such as total revenue, total hours tutored, active student counts, and unpaid billing indicators.")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(reportService.getAdminDashboardStats());
    }

    @GetMapping("/export/revenue")
    @Operation(summary = "Export Revenue Excel", description = "Generates and exports an Excel spreadsheet detailing all billing invoices, payment allocations, and total revenues over a date range.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Excel file generated and returned successfully")
    })
    public ResponseEntity<byte[]> exportRevenue(
            @Parameter(description = "Start date", example = "2026-06-01", required = true) @RequestParam String startDate,
            @Parameter(description = "End date", example = "2026-06-30", required = true) @RequestParam String endDate
    ) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        byte[] excelContent = reportService.exportRevenueReportToExcel(start, end);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"revenue_report_" + startDate + "_to_" + endDate + ".xlsx\"")
                .body(excelContent);
    }

    @GetMapping("/export/students")
    @Operation(summary = "Export Students Excel", description = "Generates and exports an Excel spreadsheet detailing all active students, parent contacts, addresses, and individual learning hourly rates.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Excel file generated and returned successfully")
    })
    public ResponseEntity<byte[]> exportStudents() {
        byte[] excelContent = reportService.exportStudentListToExcel();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"students_list.xlsx\"")
                .body(excelContent);
    }
}
