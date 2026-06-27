package com.example.Streamer.University;




import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;
    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name}")
    private String fromName;

    @Value("${brevo.to.email}")
    private String toEmail;

    private final RestTemplate restTemplate;

    /**
     * Fires an email to YOUR inbox when a user completes registration.
     */
    public void sendRegistrationEmail(UserBot user) throws IOException {
        String subject = "🎉 New Bot Registration – " + user.getEmail();
        String html = new String(
                new ClassPathResource("templates/welcome-email.html").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
        html = html.replace("{{name}}",user.getFullName());


        Map<String, Object> body = new HashMap<>();

        // Sender
        Map<String, String> sender = new HashMap<>();
        sender.put("email", fromEmail);
        sender.put("name", fromName);
        body.put("sender", sender);

        // Recipient (you — the admin)
        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", toEmail);
        body.put("to", List.of(recipient));

        body.put("subject", subject);
        body.put("htmlContent", html);   // Brevo uses "htmlContent" not "html"

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.brevo.com/v3/smtp/email",
                    request,
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Registration email sent for chatId={}", user.getChatId());
            } else {
                log.error("❌ Brevo returned status: {} body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send registration email for chatId={}: {}", user.getChatId(), e.getMessage());
        }
    }

    public void sendWelcomeEmail(UserBot user) throws IOException {
        Map<String, Object> body = new HashMap<>();

        Map<String, String> sender = new HashMap<>();
        sender.put("email", fromEmail);
        sender.put("name", fromName);
        body.put("sender", sender);

        // Send TO the user this time
        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", user.getEmail());  // ← user's email
        body.put("to", List.of(recipient));
        String html = new String(
                new ClassPathResource("templates/welcome-email.html").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
        html = html.replace("{{name}}",user.getFullName());

        body.put("subject", "Welcome to Streamer University!");
        body.put("htmlContent", html);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.brevo.com/v3/smtp/email",
                    request,
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Welcome email sent to {}", user.getEmail());
            } else {
                log.error("❌ Brevo returned status: {} body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }

    }


}