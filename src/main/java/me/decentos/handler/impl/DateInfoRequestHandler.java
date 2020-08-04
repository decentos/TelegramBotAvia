package me.decentos.handler.impl;

import lombok.RequiredArgsConstructor;
import me.decentos.bot.Bot;
import me.decentos.dto.SearchDto;
import me.decentos.handler.RequestHandler;
import me.decentos.service.FindTicketsService;
import me.decentos.service.PrepareMessageService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class DateInfoRequestHandler implements RequestHandler {

    private final FindTicketsService findTickets;
    private final PrepareMessageService prepareMessageService;
    private final MessageSource messageSource;

    @Override
    public void handle(String text, Update update, Map<Long, SearchDto> searchMap, Bot bot) throws TelegramApiException {
        String search = messageSource.getMessage("search", null, Locale.getDefault());
        Long chatId = update.getMessage().getChatId();
        SearchDto searchDto = searchMap.get(chatId);
        if (text.equals(search) || searchDto == null || !text.matches("\\d\\d\\d\\d-\\d\\d-\\d\\d")) return;

        String dateReturn = messageSource.getMessage("date.return", null, Locale.getDefault());

        if (searchDto.getDepartDate() == null) {
            searchDto = searchMap.get(chatId);
            searchDto.setDepartDate(text);
            searchMap.put(chatId, searchDto);
            bot.execute(prepareMessageService.prepareMessageConfig(chatId, dateReturn));
        } else {
            searchDto = searchMap.get(chatId);
            searchDto.setReturnDate(text);
            searchMap.put(chatId, searchDto);
            findTickets.findTickets(chatId, searchDto, searchMap, bot);
        }
    }
}
