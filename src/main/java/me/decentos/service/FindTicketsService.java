package me.decentos.service;

import me.decentos.bot.Bot;
import me.decentos.dto.SearchDto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;

public interface FindTicketsService {

    void findTickets(Long chatId, SearchDto searchDto, Map<Long, SearchDto> searchMap, Bot bot) throws TelegramApiException;
}
