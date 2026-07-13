package com.tutorsys.service;

import com.tutorsys.dto.StudentDto;
import com.tutorsys.entity.Invoice;
import com.tutorsys.entity.Parent;
import com.tutorsys.entity.Payment;
import com.tutorsys.entity.Student;
import com.tutorsys.repository.InvoiceRepository;
import com.tutorsys.repository.ParentRepository;
import com.tutorsys.repository.PaymentRepository;
import com.tutorsys.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportService {

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    public ReportService(StudentRepository studentRepository, ParentRepository parentRepository,
                         PaymentRepository paymentRepository, InvoiceRepository invoiceRepository) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Total active students
        List<Student> activeStudents = studentRepository.findByDeletedFalse().stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .collect(Collectors.toList());
        stats.put("totalStudents", activeStudents.size());

        // Students by grade
        Map<String, Long> studentsByGrade = activeStudents.stream()
                .filter(s -> s.getGrade() != null)
                .collect(Collectors.groupingBy(Student::getGrade, Collectors.counting()));
        stats.put("studentsByGrade", studentsByGrade);

        // Students by subject (dynamic map)
        Map<String, Integer> studentsBySubject = new HashMap<>();
        for (Student student : activeStudents) {
            student.getSubjects().forEach(subj -> {
                studentsBySubject.put(subj.getName(), studentsBySubject.getOrDefault(subj.getName(), 0) + 1);
            });
        }
        stats.put("studentsBySubject", studentsBySubject);

        // Revenue this month (payments received)
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        BigDecimal monthlyRevenue = paymentRepository.getTotalRevenueInPeriod(startOfMonth, endOfMonth);
        stats.put("monthlyRevenue", monthlyRevenue);

        // Total outstanding dues
        BigDecimal totalOutstandingDues = invoiceRepository.getTotalOutstandingDues();
        stats.put("totalOutstandingDues", totalOutstandingDues);

        // List parents with overdue balances (balance > 0 and invoice is overdue)
        List<Invoice> overdueInvoices = invoiceRepository.findAll().stream()
                .filter(i -> "SENT".equals(i.getStatus()) && i.getDueDate().isBefore(LocalDate.now()) && i.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        
        List<Map<String, Object>> overdueParents = overdueInvoices.stream()
                .map(i -> {
                    Map<String, Object> pMap = new HashMap<>();
                    pMap.put("parentName", i.getParent().getName());
                    pMap.put("invoiceNumber", i.getInvoiceNumber());
                    pMap.put("balanceDue", i.getBalanceDue());
                    pMap.put("dueDate", i.getDueDate());
                    return pMap;
                })
                .collect(Collectors.toList());
        stats.put("overdueParents", overdueParents);

        return stats;
    }

    public byte[] exportRevenueReportToExcel(LocalDate start, LocalDate end) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Revenue Report");

            // Header row
            Row header = sheet.createRow(0);
            String[] headers = {"Payment Date", "Parent Name", "Amount", "Payment Method", "Reference #", "Notes"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }

            List<Payment> payments = paymentRepository.findByPaymentDateBetween(start, end);
            int rowIdx = 1;
            BigDecimal total = BigDecimal.ZERO;
            for (Payment payment : payments) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(payment.getPaymentDate().toString());
                row.createCell(1).setCellValue(payment.getParent().getName());
                row.createCell(2).setCellValue(payment.getAmount().doubleValue());
                row.createCell(3).setCellValue(payment.getPaymentMethod());
                row.createCell(4).setCellValue(payment.getReferenceNumber() != null ? payment.getReferenceNumber() : "");
                row.createCell(5).setCellValue(payment.getNotes() != null ? payment.getNotes() : "");
                total = total.add(payment.getAmount());
            }

            // Total row
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.createCell(1).setCellValue("TOTAL");
            totalRow.createCell(2).setCellValue(total.doubleValue());

            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Excel revenue report", e);
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    public byte[] exportStudentListToExcel() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students");

            Row header = sheet.createRow(0);
            String[] headers = {"First Name", "Last Name", "Preferred Name", "Grade", "School", "Parent Name", "Parent Email", "Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }

            List<Student> students = studentRepository.findByDeletedFalse();
            int rowIdx = 1;
            for (Student student : students) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(student.getFirstName());
                row.createCell(1).setCellValue(student.getLastName());
                row.createCell(2).setCellValue(student.getPreferredName() != null ? student.getPreferredName() : "");
                row.createCell(3).setCellValue(student.getGrade() != null ? student.getGrade() : "");
                row.createCell(4).setCellValue(student.getSchool() != null ? student.getSchool() : "");
                row.createCell(5).setCellValue(student.getParent().getName());
                row.createCell(6).setCellValue(student.getParent().getEmail());
                row.createCell(7).setCellValue(student.getStatus());
            }

            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Excel student list", e);
            throw new RuntimeException("Excel generation failed", e);
        }
    }
}
