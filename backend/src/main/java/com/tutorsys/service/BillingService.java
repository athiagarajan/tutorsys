package com.tutorsys.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tutorsys.dto.InvoiceDto;
import com.tutorsys.dto.SessionDto;
import com.tutorsys.entity.Invoice;
import com.tutorsys.entity.Parent;
import com.tutorsys.entity.Session;
import com.tutorsys.entity.Student;
import com.tutorsys.repository.InvoiceRepository;
import com.tutorsys.repository.ParentRepository;
import com.tutorsys.repository.SessionRepository;
import com.tutorsys.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final SessionRepository sessionRepository;
    private final EmailService emailService;
    private final StudentService studentService;

    @Value("${tutorsys.invoices.dir}")
    private String invoicesDir;

    public BillingService(InvoiceRepository invoiceRepository, ParentRepository parentRepository,
                          StudentRepository studentRepository, SessionRepository sessionRepository,
                          EmailService emailService, StudentService studentService) {
        this.invoiceRepository = invoiceRepository;
        this.parentRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.emailService = emailService;
        this.studentService = studentService;
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> getInvoicesByParentUsername(String username) {
        return invoiceRepository.findByParentUserUsername(username).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        return convertToDto(invoice);
    }

    @Transactional
    public InvoiceDto generateInvoice(Long parentId, LocalDate start, LocalDate end) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));

        // 1. Calculate previous outstanding balance
        BigDecimal previousBalance = invoiceRepository.getOutstandingBalanceForParent(parentId);

        // 2. Fetch all billable sessions in the period for all students of this parent
        List<Student> students = studentRepository.findByParentIdAndDeletedFalse(parentId);
        List<Session> billableSessions = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Student student : students) {
            List<Session> sessions = sessionRepository.findBillableSessionsForParentInPeriod(parentId, start, end);
            for (Session session : sessions) {
                if (session.getStudent().getId().equals(student.getId())) {
                    // Populate rate charged if not set
                    if (session.getRateCharged() == null) {
                        BigDecimal rate = studentService.getStudentRateForSession(student.getId(), session.getSubject().getId(), session.getSessionDate());
                        session.setRateCharged(rate);
                    }
                    billableSessions.add(session);
                    subtotal = subtotal.add(session.getRateCharged());
                }
            }
        }

        if (billableSessions.isEmpty()) {
            throw new IllegalArgumentException("No billable sessions found for parent " + parent.getName() + " in this period");
        }

        // 3. Create invoice entity
        Invoice invoice = new Invoice();
        invoice.setParent(parent);
        invoice.setInvoiceNumber("INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        invoice.setBillingPeriodStart(start);
        invoice.setBillingPeriodEnd(end);
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(15)); // 15 days payment term
        invoice.setStatus("DRAFT");
        invoice.setSubtotalAmount(subtotal);
        invoice.setPreviousBalance(previousBalance);
        invoice.setPaymentsApplied(BigDecimal.ZERO);
        invoice.setBalanceDue(subtotal.add(previousBalance));
        invoice.setNotes("Thank you for your business!");

        invoice = invoiceRepository.save(invoice);

        // 4. Associate sessions with the invoice
        for (Session session : billableSessions) {
            session.setInvoice(invoice);
            sessionRepository.save(session);
        }

        // 5. Generate PDF
        generatePdfForInvoice(invoice, billableSessions);

        return convertToDto(invoice);
    }

    @Transactional
    public InvoiceDto regenerateInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalArgumentException("Only DRAFT invoices can be regenerated");
        }

        // 1. Dissociate old sessions
        List<Session> oldSessions = sessionRepository.findByInvoiceIdAndDeletedFalse(invoice.getId());
        for (Session session : oldSessions) {
            session.setInvoice(null);
            sessionRepository.save(session);
        }

        // 2. Re-fetch all billable sessions in the period
        List<Student> students = studentRepository.findByParentIdAndDeletedFalse(invoice.getParent().getId());
        List<Session> billableSessions = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Student student : students) {
            List<Session> sessions = sessionRepository.findBillableSessionsForParentInPeriod(
                    invoice.getParent().getId(), invoice.getBillingPeriodStart(), invoice.getBillingPeriodEnd());
            for (Session session : sessions) {
                if (session.getStudent().getId().equals(student.getId())) {
                    BigDecimal rate = studentService.getStudentRateForSession(student.getId(), session.getSubject().getId(), session.getSessionDate());
                    session.setRateCharged(rate);
                    billableSessions.add(session);
                    subtotal = subtotal.add(session.getRateCharged());
                    session.setInvoice(invoice);
                    sessionRepository.save(session);
                }
            }
        }

        // 3. Update amounts
        invoice.setSubtotalAmount(subtotal);
        invoice.setBalanceDue(subtotal.add(invoice.getPreviousBalance()).subtract(invoice.getPaymentsApplied()));
        invoice = invoiceRepository.save(invoice);

        // 4. Re-generate PDF
        generatePdfForInvoice(invoice, billableSessions);

        return convertToDto(invoice);
    }

    @Transactional
    public InvoiceDto finalizeAndSendInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if ("CANCELLED".equals(invoice.getStatus())) {
            throw new IllegalArgumentException("Cancelled invoice cannot be sent");
        }

        invoice.setStatus("SENT");
        invoice = invoiceRepository.save(invoice);

        // Fetch sessions
        List<Session> sessions = sessionRepository.findByInvoiceIdAndDeletedFalse(invoice.getId());
        
        // Read PDF bytes to email
        byte[] pdfBytes = null;
        if (invoice.getPdfFilePath() != null) {
            try {
                File file = new File(invoice.getPdfFilePath());
                if (file.exists()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                    }
                    pdfBytes = bos.toByteArray();
                }
            } catch (Exception e) {
                log.error("Failed to read PDF file for email attachment: {}", e.getMessage());
            }
        }

        // Email invoice to parent
        emailService.sendInvoiceEmail(invoice.getParent(), invoice, pdfBytes);

        return convertToDto(invoice);
    }

    public void generatePdfForInvoice(Invoice invoice, List<Session> sessions) {
        try {
            File dir = new File(invoicesDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = "Invoice_" + invoice.getInvoiceNumber() + ".pdf";
            File pdfFile = new File(dir, fileName);

            String htmlContent = buildInvoiceHtml(invoice, sessions);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, "/");
            builder.toStream(os);
            builder.run();

            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                fos.write(os.toByteArray());
            }

            invoice.setPdfFilePath(pdfFile.getAbsolutePath());
            invoiceRepository.save(invoice);
            log.info("PDF generated successfully at: {}", pdfFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
        }
    }

    private String buildInvoiceHtml(Invoice invoice, List<Session> sessions) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><style>");
        sb.append("body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #333; margin: 30px; }");
        sb.append(".header { border-bottom: 2px solid #3f51b5; padding-bottom: 20px; margin-bottom: 30px; }");
        sb.append(".header table { width: 100%; }");
        sb.append(".logo { font-size: 28px; font-weight: bold; color: #3f51b5; }");
        sb.append(".title { font-size: 24px; font-weight: bold; text-align: right; color: #555; }");
        sb.append(".details { margin-bottom: 30px; }");
        sb.append(".details table { width: 100%; }");
        sb.append(".details td { vertical-align: top; width: 50%; }");
        sb.append(".section-title { font-size: 14px; font-weight: bold; text-transform: uppercase; color: #777; margin-bottom: 8px; }");
        sb.append(".table-sessions { width: 100%; border-collapse: collapse; margin-bottom: 30px; }");
        sb.append(".table-sessions th { background-color: #f5f5f5; border-bottom: 1px solid #ddd; padding: 10px; text-align: left; font-size: 13px; font-weight: bold; }");
        sb.append(".table-sessions td { border-bottom: 1px solid #eee; padding: 10px; font-size: 13px; }");
        sb.append(".totals { text-align: right; width: 100%; }");
        sb.append(".totals table { margin-left: auto; border-collapse: collapse; }");
        sb.append(".totals td { padding: 6px 12px; font-size: 14px; }");
        sb.append(".totals .grand-total { font-size: 18px; font-weight: bold; color: #3f51b5; border-top: 1px solid #ddd; padding-top: 10px; }");
        sb.append(".footer { margin-top: 50px; text-align: center; font-size: 12px; color: #999; border-top: 1px solid #eee; padding-top: 15px; }");
        sb.append("</style></head><body>");

        // Header
        sb.append("<div class='header'><table><tr>");
        sb.append("<td class='logo'>TutorSys Tuition</td>");
        sb.append("<td class='title'>INVOICE</td>");
        sb.append("</tr></table></div>");

        // Details (Bill To / Info)
        sb.append("<div class='details'><table><tr>");
        sb.append("<td>");
        sb.append("<div class='section-title'>Bill To:</div>");
        sb.append("<strong>").append(invoice.getParent().getName()).append("</strong><br/>");
        sb.append("Email: ").append(invoice.getParent().getEmail()).append("<br/>");
        if (invoice.getParent().getPhone() != null) sb.append("Phone: ").append(invoice.getParent().getPhone()).append("<br/>");
        if (invoice.getParent().getAddress() != null) sb.append("Address: ").append(invoice.getParent().getAddress().replace("\n", "<br/>")).append("<br/>");
        sb.append("</td>");
        sb.append("<td>");
        sb.append("<div class='section-title'>Invoice Info:</div>");
        sb.append("<strong>Invoice #:</strong> ").append(invoice.getInvoiceNumber()).append("<br/>");
        sb.append("<strong>Date:</strong> ").append(invoice.getIssueDate().toString()).append("<br/>");
        sb.append("<strong>Due Date:</strong> ").append(invoice.getDueDate().toString()).append("<br/>");
        sb.append("<strong>Billing Period:</strong> ").append(invoice.getBillingPeriodStart().toString()).append(" to ").append(invoice.getBillingPeriodEnd().toString()).append("<br/>");
        sb.append("<strong>Status:</strong> ").append(invoice.getStatus()).append("<br/>");
        sb.append("</td>");
        sb.append("</tr></table></div>");

        // Sessions Table
        sb.append("<div class='section-title'>Tutoring Sessions</div>");
        sb.append("<table class='table-sessions'>");
        sb.append("<thead><tr><th>Date</th><th>Student</th><th>Subject</th><th>Status</th><th>Rate</th></tr></thead>");
        sb.append("<tbody>");
        for (Session session : sessions) {
            sb.append("<tr>");
            sb.append("<td>").append(session.getSessionDate().toString()).append("</td>");
            sb.append("<td>").append(session.getStudent().getFirstName()).append(" ").append(session.getStudent().getLastName()).append("</td>");
            sb.append("<td>").append(session.getSubject().getName()).append("</td>");
            sb.append("<td>").append(session.getStatus()).append("</td>");
            sb.append("<td>$").append(session.getRateCharged()).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");

        // Totals
        sb.append("<div class='totals'><table>");
        sb.append("<tr><td>Subtotal:</td><td>$").append(invoice.getSubtotalAmount()).append("</td></tr>");
        sb.append("<tr><td>Previous Unpaid Balance:</td><td>$").append(invoice.getPreviousBalance()).append("</td></tr>");
        if (invoice.getPaymentsApplied().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("<tr><td>Payments Applied:</td><td>-$").append(invoice.getPaymentsApplied()).append("</td></tr>");
        }
        sb.append("<tr class='grand-total'><td>Balance Due:</td><td>$").append(invoice.getBalanceDue()).append("</td></tr>");
        sb.append("</table></div>");

        // Footer
        sb.append("<div class='footer'>");
        sb.append("Payment Methods: Cash, Check (payable to TutorSys), or Venmo (@TutorSys).<br/>");
        sb.append("Please pay by the due date to avoid late fees. Thank you for choosing TutorSys!");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    public InvoiceDto convertToDto(Invoice invoice) {
        InvoiceDto dto = new InvoiceDto();
        dto.setId(invoice.getId());
        dto.setParentId(invoice.getParent().getId());
        dto.setParentName(invoice.getParent().getName());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setBillingPeriodStart(invoice.getBillingPeriodStart());
        dto.setBillingPeriodEnd(invoice.getBillingPeriodEnd());
        dto.setIssueDate(invoice.getIssueDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setStatus(invoice.getStatus());
        dto.setSubtotalAmount(invoice.getSubtotalAmount());
        dto.setPreviousBalance(invoice.getPreviousBalance());
        dto.setPaymentsApplied(invoice.getPaymentsApplied());
        dto.setBalanceDue(invoice.getBalanceDue());
        dto.setPdfFilePath(invoice.getPdfFilePath());
        dto.setNotes(invoice.getNotes());
        
        // Load sessions
        try {
            List<Session> sessions = sessionRepository.findByInvoiceIdAndDeletedFalse(invoice.getId());
            dto.setSessions(sessions.stream().map(s -> {
                SessionDto sDto = new SessionDto();
                sDto.setId(s.getId());
                sDto.setStudentId(s.getStudent().getId());
                sDto.setStudentName(s.getStudent().getFirstName() + " " + s.getStudent().getLastName());
                sDto.setSubjectId(s.getSubject().getId());
                sDto.setSubjectName(s.getSubject().getName());
                sDto.setSessionDate(s.getSessionDate());
                sDto.setScheduledStartTime(s.getScheduledStartTime());
                sDto.setActualStartTime(s.getActualStartTime());
                sDto.setActualDurationMinutes(s.getActualDurationMinutes());
                sDto.setStatus(s.getStatus());
                sDto.setRateCharged(s.getRateCharged());
                sDto.setNotes(s.getNotes());
                return sDto;
            }).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Failed to load sessions for invoice DTO: {}", e.getMessage());
        }

        return dto;
    }
}
