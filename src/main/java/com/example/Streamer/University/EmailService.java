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
        String html = buildEmailHtml(user);


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





    private String buildEmailHtml(UserBot user) {
        String platform = user.getSocialMediaState() != null
                ? user.getSocialMediaState().name()
                : "Not specified";

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <style>
                body { font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px; }
                .card { background: #fff; border-radius: 8px; padding: 30px; max-width: 500px;
                        margin: auto; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                h2 { color: #0B996E; }
                table { width: 100%%; border-collapse: collapse; margin-top: 16px; }
                td { padding: 10px 12px; border-bottom: 1px solid #eee; }
                td:first-child { font-weight: bold; color: #555; width: 140px; }
                .badge { display: inline-block; background: #0B996E; color: white;
                         padding: 4px 10px; border-radius: 12px; font-size: 12px; }
              </style>
            </head>
            <body>
              <div class="card">
                <h2>🎉 New Registration</h2>
                <p>A new user just completed registration on your Telegram bot.</p>
                <table>
                  <tr><td>Telegram Chat ID</td><td>%s</td></tr>
                  <tr><td>Role</td><td><span class="badge">%s</span></td></tr>
                  <tr><td>Date of Birth</td><td>%s</td></tr>
                  <tr><td>Email</td><td><a href="mailto:%s">%s</a></td></tr>
                  <tr><td>Location</td><td>%s</td></tr>
                  <tr><td>Social Platform</td><td><span class="badge">%s</span></td></tr>
                </table>
                <p style="margin-top:20px; color:#999; font-size:12px;">
                  Sent automatically by your Telegram Bot via Brevo
                </p>
              </div>
            </body>
            </html>
            """.formatted(
                user.getChatId(),
                user.getRoles(),
                user.getDob(),
                user.getEmail(), user.getEmail(),
                user.getLocationType(),
                platform
        );
    }

    private String buildWelcomeHtml(UserBot user) {
        String platform = user.getSocialMediaState() != null
                ? user.getSocialMediaState().name()
                : "Not specified";

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <style>
            body {
              font-family: Georgia, 'Times New Roman', serif;
              background: #f5f5f5;
              padding: 40px 20px;
              margin: 0;
              color: #2b2b2b;
            }
            .letter {
              background: #ffffff;
              max-width: 560px;
              margin: auto;
              padding: 48px 56px;
              border: 1px solid #e0e0e0;
            }
            .letterhead {
              border-bottom: 2px solid #1a1a1a;
              padding-bottom: 16px;
              margin-bottom: 32px;
            }
            .letterhead h1 {
              font-size: 20px;
              font-weight: normal;
              letter-spacing: 1px;
              text-transform: uppercase;
              margin: 0;
              color: #1a1a1a;
            }
            .letterhead p {
              font-size: 12px;
              color: #777;
              margin: 4px 0 0 0;
              letter-spacing: 0.5px;
            }
            .date {
              font-size: 13px;
              color: #777;
              margin-bottom: 24px;
            }
            .greeting {
              font-size: 15px;
              margin-bottom: 18px;
            }
            .body-text {
              font-size: 14px;
              line-height: 1.7;
              color: #333;
              margin-bottom: 24px;
            }
            .details {
              border-top: 1px solid #e0e0e0;
              border-bottom: 1px solid #e0e0e0;
              padding: 20px 0;
              margin-bottom: 28px;
            }
            .details table {
              width: 100%%;
              border-collapse: collapse;
              font-size: 13px;
            }
            .details td {
              padding: 6px 0;
              vertical-align: top;
            }
            .details td:first-child {
              color: #777;
              width: 140px;
            }
            .details td:last-child {
              color: #1a1a1a;
              font-weight: 600;
            }
            .signoff {
              font-size: 14px;
              line-height: 1.7;
              color: #333;
              margin-bottom: 8px;
            }
            .signature {
              font-size: 14px;
              color: #1a1a1a;
              margin-top: 4px;
            }
            .footer {
              margin-top: 40px;
              padding-top: 20px;
              border-top: 1px solid #e0e0e0;
              font-size: 11px;
              color: #999;
              text-align: center;
            }
          </style>
        </head>
        <body>
          <div class="letter">

            <div class="letterhead">
              <h1>Streamer University</h1>
              <p>Registration Confirmation</p>
            </div>

            <p class="greeting">Dear %s,</p>

            <p class="body-text">
              Thank you for registering with Streamer University. Your account has been
              successfully created, and we are pleased to welcome you to the community.
            </p>

            <div class="details">
              <table>
                <tr><td>Email</td><td>%s</td></tr>
                <tr><td>Platform</td><td>%s</td></tr>
                <tr><td>Location</td><td>%s</td></tr>
              </table>
            </div>

            <p class="body-text">
              You will receive periodic updates, resources, and announcements relevant
              to your streaming journey. Should you have any questions, feel free to
              reach out by replying to this email or messaging us through Telegram.
            </p>

            <p class="signoff">Best regards,</p>
            <p class="signature">The Streamer University Team</p>

            <div class="footer">
              You are receiving this email because you registered an account with Streamer University.<br>
              © 2026 Streamer University. All rights reserved.
            </div>

          </div>
        </body>
        </html>
        """.formatted(
                user.getFullName() != null ? user.getFullName() : user.getEmail(),
                user.getEmail(),
                platform,
                user.getLocationType()
        );
    }
}