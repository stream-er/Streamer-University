package com.example.Streamer.University;




import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    public void sendRegistrationEmail(UserBot user) {
        String subject = "🎉 New Bot Registration – " + user.getEmail();
        String html = buildWelcomeHtml(user);


        Map<String, Object> body = new HashMap<>();

        // Sender
        Map<String, String> sender = new HashMap<>();
        sender.put("email", fromEmail);
        sender.put("name", fromEmail);
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

    public void sendWelcomeEmail(UserBot user) {
        Map<String, Object> body = new HashMap<>();

        Map<String, String> sender = new HashMap<>();
        sender.put("email", fromEmail);
        sender.put("name", fromName);
        body.put("sender", sender);

        // Send TO the user this time
        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", user.getEmail());  // ← user's email
        body.put("to", List.of(recipient));

        body.put("subject", "👋 Welcome to Streamer University!");
        body.put("htmlContent", buildWelcomeHtml(user));

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
    private String buildWelcomeHtml(UserBot user) {
        String platform = user.getSocialMediaState() != null
                ? user.getSocialMediaState().name()
                : "Not specified";

        String displayName = user.getFullName() != null ? user.getFullName() : user.getEmail();

        return """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <style>
        body {
          font-family: Georgia, 'Times New Roman', serif;
          background: #f0ece4;
          padding: 40px 20px;
          margin: 0;
          color: #2b2b2b;
        }
        .wrapper {
          background: #ffffff;
          max-width: 560px;
          margin: auto;
          border: 1px solid #e0e0e0;
          border-radius: 10px;
          overflow: hidden;
        }
        .header {
          background: #6B1A2A;
          padding: 28px 48px 20px;
        }
        .header .org-name {
          font-size: 11px;
          letter-spacing: 2px;
          text-transform: uppercase;
          color: #C9A84C;
          margin: 0 0 6px 0;
        }
        .header .divider {
          width: 36px;
          height: 1px;
          background: #C9A84C;
          margin-bottom: 6px;
        }
        .header .est {
          font-size: 11px;
          color: rgba(201, 168, 76, 0.6);
          margin: 0;
          letter-spacing: 1px;
        }
        .body {
          padding: 40px 48px 36px;
        }
        .date {
          font-size: 11px;
          color: #888;
          letter-spacing: 0.5px;
          margin: 0 0 28px 0;
        }
        .greeting {
          font-size: 15px;
          color: #1a1a1a;
          margin: 0 0 20px 0;
        }
        .greeting em {
          font-style: italic;
        }
        .text {
          font-size: 14px;
          line-height: 1.8;
          color: #333;
          margin: 0 0 16px 0;
        }
        .details {
          border-top: 1px solid #C9A84C;
          border-bottom: 1px solid #C9A84C;
          padding: 18px 0;
          margin-bottom: 28px;
        }
        .details table {
          width: 100%%;
          border-collapse: collapse;
          font-size: 13px;
        }
        .details td {
          padding: 5px 0;
          vertical-align: top;
        }
        .details td:first-child {
          color: #888;
          width: 130px;
        }
        .details td:last-child {
          color: #1a1a1a;
        }
        .telegram-box {
          background: #FBF7F0;
          border-left: 3px solid #C9A84C;
          padding: 14px 18px;
          margin-bottom: 28px;
        }
        .telegram-box .label {
          font-size: 11px;
          color: #888;
          letter-spacing: 0.5px;
          text-transform: uppercase;
          margin: 0 0 8px 0;
        }
        .telegram-box a {
          font-size: 13px;
          color: #6B1A2A;
          text-decoration: none;
        }
        .signoff {
          font-size: 14px;
          color: #333;
          margin: 0 0 4px 0;
        }
        .signature {
          font-size: 14px;
          color: #6B1A2A;
          font-style: italic;
          margin: 0;
        }
        .footer {
          border-top: 1px solid #e0e0e0;
          padding: 14px 48px;
          text-align: center;
          font-size: 11px;
          color: #aaa;
          letter-spacing: 0.3px;
        }
      </style>
    </head>
    <body>
      <div class="wrapper">

        <div class="header">
          <p class="org-name">Streamer University</p>
          <div class="divider"></div>
          <p class="est">Est. 2026</p>
        </div>

        <div class="body">
          <p class="date">%s</p>

          <p class="greeting">Dear <em>%s</em>,</p>

          <p class="text">
            On behalf of everyone at Streamer University, it is our great pleasure to welcome
            you to our community. Your registration has been confirmed, and we congratulate
            you on taking this important step in your streaming journey.
          </p>

          <p class="text">
            We are delighted to have you with us and look forward to supporting you every
            step of the way.
          </p>

          <div class="details">
            <table>
              <tr><td>Email</td><td>%s</td></tr>
              <tr><td>Platform</td><td>%s</td></tr>
              <tr><td>Location</td><td>%s</td></tr>
            </table>
          </div>

          <p class="text">
            You will receive updates, resources, and announcements tailored to your journey.
            Should you have any questions or need assistance, please do not hesitate to
            reach out — we are always happy to help.
          </p>

          <div class="telegram-box">
            <p class="label">Get in touch</p>
            <a href="https://t.me/YOUR_HANDLE">&#9992; t.me/YOUR_HANDLE</a>
          </div>

          <p class="signoff">Warm congratulations,</p>
          <p class="signature">The Streamer University Team</p>
        </div>

        <div class="footer">
          You are receiving this because you registered with Streamer University.
          &nbsp;·&nbsp; &copy; 2026 Streamer University
        </div>

      </div>
    </body>
    </html>
    """.formatted(
                java.time.LocalDate.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")
                ),
                displayName,
                user.getEmail(),
                platform,
                user.getLocationType()
        );
    }
}