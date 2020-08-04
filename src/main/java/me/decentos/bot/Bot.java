package me.decentos.bot;

import lombok.RequiredArgsConstructor;
import me.decentos.dto.SearchDto;
import me.decentos.handler.RequestHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class Bot extends TelegramLongPollingBot {

    private final Map<Long, SearchDto> searchMap = new HashMap<>();
    private final List<RequestHandler> handlers;

    @Value("${bot.username}")
    private String botUserName;

    @Value("${bot.token}")
    private String botToken;

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        String text = update.getMessage().getText();
        handlers.forEach(h -> {
            try {
                h.handle(text, update, searchMap, this);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        });
    }
}
