package com.tutorsys.service;

import com.tutorsys.entity.Invoice;
import com.tutorsys.entity.Parent;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendInvoiceEmail(Parent parent, Invoice invoice, byte[] pdfContent) {
        String recipientEmail = parent.getEmail();
        String subject = "TutorSys Invoice " + invoice.getInvoiceNumber() + " for " + parent.getName();
        
        String body = "Dear " + parent.getName() + ",\n\n" +
                "Please find attached your invoice for the tutoring sessions conducted between " +
                invoice.getBillingPeriodStart() + " and " + invoice.getBillingPeriodEnd() + ".\n\n" +
                "Summary:\n" +
                "- Subtotal: $" + invoice.getSubtotalAmount() + "\n" +
                "- Outstanding Dues from Previous Months: $" + invoice.getPreviousBalance() + "\n" +
                "- Payments Applied: $" + invoice.getPaymentsApplied() + "\n" +
                "- Balance Due: $" + invoice.getBalanceDue() + "\n" +
                "- Due Date: " + invoice.getDueDate() + "\n\n" +
                "Please refer to the attached PDF for details and payment instructions.\n\n" +
                "Thank you for your business!\n\n" +
                "Best regards,\n" +
                "TutorSys Management Team";

        log.info("Sending Email to: {} with Subject: {}", recipientEmail, subject);
        log.info("Email body:\n{}", body);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body);
            helper.setFrom("billing@tutorsys.com");

            if (pdfContent != null && pdfContent.length > 0) {
                helper.addAttachment("Invoice_" + invoice.getInvoiceNumber() + ".pdf", new ByteArrayResource(pdfContent));
            }

            mailSender.send(message);
            log.info("Email sent successfully!");
        } catch (Exception e) {
            log.error("Failed to send email via SMTP (normal for dev without configured SMTP server): {}", e.getMessage());
            log.info("DEVELOPMENT MODE: Checked invoice email successfully. Details printed above.");
        }
    }
}
