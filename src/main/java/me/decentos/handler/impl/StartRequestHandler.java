package me.decentos.handler.impl;

import lombok.RequiredArgsConstructor;
import me.decentos.bot.Bot;
import me.decentos.dto.SearchDto;
import me.decentos.handler.RequestHandler;
import me.decentos.service.ButtonService;
import me.decentos.service.PrepareMessageService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class StartRequestHandler implements RequestHandler {

    private final PrepareMessageService prepareMessageService;
    private final ButtonService buttonService;
    private final MessageSource messageSource;

    @Override
    public void handle(String text, Update update, Map<Long, SearchDto> searchMap, Bot bot) throws TelegramApiException {
        String start = messageSource.getMessage("start", null, Locale.getDefault());
        String end = messageSource.getMessage("end", null, Locale.getDefault());
        if (!text.equals(start) && !text.equals(end)) return;

        String greeting = messageSource.getMessage("greeting", null, Locale.getDefault());
        Long chatId = update.getMessage().getChatId();

        searchMap.remove(chatId);
        SendMessage startSearch = prepareMessageService.prepareMessageConfig(chatId, greeting);
        buttonService.setNewSearchButtons(startSearch);
        bot.execute(startSearch);
    }
}
