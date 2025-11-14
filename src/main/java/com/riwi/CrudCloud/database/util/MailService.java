package com.riwi.CrudCloud.database.util;

import com.riwi.CrudCloud.common.models.Database; // ¡CAMBIO! Importamos la nueva entidad Database
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

    // Asegúrate de que el senderEmail es correcto según tu properties
    private final String senderEmail = "crudcloud22@gmail.com";

    /**
     * Sends a database creation notification email.
     * Does NOT include the password in plain text.
     * @param database La entidad Database.
     */
    public void sendInstanceCreationEmail(Database database, String username, String host, int port) { // CAMBIO de Instance a Database
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(database.getUser().getEmail());
            message.setSubject("Database Instance Created in CrudCloud!");

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
                    database.getUser().getUsername(),
                    database.getName(),
                    database.getDbType().name(),
                    host,
                    port,
                    username
            );

            message.setText(text);
            mailSender.send(message);
            log.info("Database creation email sent to {}", database.getUser().getEmail());

        } catch (Exception e) {
            log.error("Error sending database creation email to {}: {}", database.getUser().getEmail(), e.getMessage());
        }
    }

    /**
     * Sends an email notification about password rotation.
     * It does NOT include the password in plain text, it only notifies about the change.
     * @param database La entidad Database.
     */
    public void sendPasswordRotationEmail(Database database, String newRawPassword) { // CAMBIO de Instance a Database
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(database.getUser().getEmail());
            message.setSubject("Rotated Instance Password in CrudCloud!");

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
                    database.getUser().getUsername(),
                    database.getName(),
                    database.getDbType().name()
            );

            message.setText(text);
            mailSender.send(message);
            log.info("Password rotation email sent to {}", database.getUser().getEmail());

        } catch (Exception e) {
            log.error("Error sending password rotation email to {}: {}", database.getUser().getEmail(), e.getMessage());
        }
    }
}