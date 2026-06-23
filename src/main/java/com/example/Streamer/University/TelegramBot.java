package com.example.Streamer.University;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final UserRepository userBotRepository;
    private final EmailService emailService;
    private final MessageSource messageSource;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {

        // Handle button clicks first
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        // Fetch or create user
        UserBot user = userBotRepository.findById(chatId).orElse(null);

        if (user == null) {
            user = UserBot.builder()
                    .chatId(chatId)
                    .state(UseState.START)
                    .build();
            userBotRepository.save(user);
        }

        // Handle /start
        if (text.equalsIgnoreCase("/start")) {
            handleStart(user);
            return;
        }

        switch (user.getState()) {
            case ROLE_SELECTION -> handleRoleSelection(user, text);
            case FULL_NAME -> handleFullName(user, text);
            case DOB -> handleDob(user, text);
            case EMAIL -> handleEmail(user, text);
            case LOCATION -> handleLocation(user, text);
            case SOCIAL_HANDLE -> handleSocailMedia(user, text);   // ✅ added
            default -> sendMessage(chatId, "Type /start to begin.");
        }
    }

    // ─── START ─────────────────────────────────────────────

    private void handleStart(UserBot user) {
        user.setState(UseState.ROLE_SELECTION);
        userBotRepository.save(user);

        sendMessageWithInlineKeyboard(user.getChatId(),
                "Welcome! Let's get you registered.\n\nSelect your role:",
                List.of(
                        List.of("Student", "ROLE_STUDENT"),
                        List.of("Professor", "ROLE_PROFESSOR")
                )
        );
    }

    // ─── CALLBACK HANDLER ─────────────────────────────────

    private void handleCallbackQuery(Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String data = update.getCallbackQuery().getData();

        UserBot user = userBotRepository.findById(chatId).orElse(null);
        if (user == null) {
            sendMessage(chatId, "Session expired. Type /start.");
            return;
        }

        // ROLE selection
        if (data.startsWith("ROLE_") && user.getState() == UseState.ROLE_SELECTION) {
            user.setRoles(data.replace("ROLE_", ""));
            user.setState(UseState.FULL_NAME);
            userBotRepository.save(user);

            sendMessage(chatId, "Great! Now enter your full name:");
        }

        else if (data.startsWith("SOCIAL_") && user.getState() == UseState.SOCIAL) {
            String platform = data.replace("SOCIAL_", "");
            user.setSocialMediaState(SocialMediaState.valueOf(platform));
            user.setState(UseState.SOCIAL_HANDLE);   // ✅ move to handle-collection state
            userBotRepository.save(user);

            sendMessage(chatId, "Great! Now enter your social media handle (e.g. @yourname):");
        }
    }

    // ─── STATES ───────────────────────────────────────────

    private void handleRoleSelection(UserBot user, String text) {
        sendMessageWithInlineKeyboard(user.getChatId(),
                "Please select your role:",
                List.of(
                        List.of("Student", "ROLE_STUDENT"),
                        List.of("Professor", "ROLE_PROFESSOR")
                )
        );
    }

    private void handleFullName(UserBot user, String text) {
        if (text.length() < 2) {
            sendMessage(user.getChatId(), "Invalid name. Try again.");
            return;
        }

        user.setFullName(text);
        user.setState(UseState.DOB);
        userBotRepository.save(user);

        sendMessage(user.getChatId(), "Enter your DOB (DD/MM/YYYY):");
    }

    private void handleDob(UserBot user, String text) {
        if (!text.matches("\\d{2}/\\d{2}/\\d{4}")) {
            sendMessage(user.getChatId(), "Invalid format. Use DD/MM/YYYY");
            return;
        }

        user.setDob(text);
        user.setState(UseState.EMAIL);
        userBotRepository.save(user);

        sendMessage(user.getChatId(), "Enter your email:");
    }

    private void handleEmail(UserBot user, String text) {
        if (!text.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            sendMessage(user.getChatId(), "Invalid email. Try again.");
            return;
        }

        user.setEmail(text);
        user.setState(UseState.LOCATION);
        userBotRepository.save(user);

        sendMessage(user.getChatId(), "Enter your location:");
    }

    private void handleLocation(UserBot user, String text) {
        user.setLocationType(text);
        user.setState(UseState.SOCIAL);
        userBotRepository.save(user);

        sendMessageWithInlineKeyboard(user.getChatId(),
                "Select your social platform you use while streaming:",
                List.of(
                        List.of("Twitch", "SOCIAL_TWITCH"),
                        List.of("TikTok", "SOCIAL_TIKTOK"),
                        List.of("Twitter", "SOCIAL_TWITTER")

                )
        );
        sendMessage(user.getChatId(),"Enter enter the social media handle: ");

    }

    private void handleSocailMedia(UserBot user, String text) {
        if (text.trim().isEmpty()) {
            sendMessage(user.getChatId(), "Handle can't be empty. Try again:");
            return;
        }

        user.setSocialMediaHandle(text.trim());
        user.setState(UseState.COMPLETED);
        userBotRepository.save(user);

        sendMessage(user.getChatId(), "Registration complete! Thank you!");

        emailService.sendRegistrationEmail(user);
        emailService.sendWelcomeEmail(user);
    }



    // ─── HELPERS ──────────────────────────────────────────

    private void sendMessage(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message", e);
        }
    }

    private void sendMessageWithInlineKeyboard(long chatId, String text, List<List<String>> buttons) {

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (List<String> btn : buttons) {
            keyboard.add(List.of(
                    InlineKeyboardButton.builder()
                            .text(btn.get(0))
                            .callbackData(btn.get(1))
                            .build()
            ));
        }

        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(keyboard).build())
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending keyboard", e);
        }
    }
}