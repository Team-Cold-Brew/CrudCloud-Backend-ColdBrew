package com.riwi.CrudCloud.instance.util;

import com.riwi.CrudCloud.instance.model.Instance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    private final String senderEmail = "crudcloud22@gmail.com";

    /**
     * Sends an instance creation notification email.
     * Does NOT include the password in plain text.
     */
    public void sendInstanceCreationEmail(Instance instance, String username, String host, int port) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(instance.getUser().getEmail());
            message.setSubject("¡Database Instance Created in CrudCloud!");

            String text = String.format("""
                Hello %s,
                
                Your new database instance ‘%s’ has been successfully created.
                
                **Connection Details:**
                Type: %s
                Host: %s
                Port: %d
                User: %s

                **IMPORTANT:** For security reasons, the password is not included in this email.
                You can download the secure PDF with the complete credentials from your Dashboard in CrudCloud.

                Happy developing!
                The CrudCloud team.
                """,
                    instance.getUser().getUsername(),
                    instance.getName(),
                    instance.getDbType().name(),
                    host,
                    port,
                    username
            );

            message.setText(text);
            mailSender.send(message);
            log.info("Instance creation email sent to {}", instance.getUser().getEmail());

        } catch (Exception e) {
            log.error("Error sending instance creation email to {}: {}", instance.getUser().getEmail(), e.getMessage());
        }
    }

    /**
     * Sends an email notification about password rotation.
     * It does NOT include the password in plain text, it only notifies about the change.
     */
    public void sendPasswordRotationEmail(Instance instance, String newRawPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(instance.getUser().getEmail());
            message.setSubject("¡Rotated Instance Password in CrudCloud!");

            String text = String.format("""
               Hello %s,

               The password for your database instance ‘%s’ (%s) has been successfully changed.

               **IMPORTANT:**
               The previous password has been invalidated.
               For security reasons, the new password is not included in this email.
               You must download the **new PDF** with your credentials from your CrudCloud Dashboard to obtain the new password. Remember that it will only be displayed once.
               If you did not request this change, please contact support immediately.

               The CrudCloud team.
                """,
                    instance.getUser().getUsername(),
                    instance.getName(),
                    instance.getDbType().name()
            );

            message.setText(text);
            mailSender.send(message);
            log.info("Password rotation email sent to{}", instance.getUser().getEmail());

        } catch (Exception e) {
            log.error("Error sending password rotation email to {}: {}", instance.getUser().getEmail(), e.getMessage());
        }
    }
}