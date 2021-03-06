package me.decentos.handler.impl;

import lombok.RequiredArgsConstructor;
import me.decentos.bot.Bot;
import me.decentos.dto.SearchDto;
import me.decentos.handler.RequestHandler;
import me.decentos.model.CityInfo;
import me.decentos.service.ApiService;
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
public class CityInfoRequestHandler implements RequestHandler {

    private final PrepareMessageService prepareMessageService;
    private final ButtonService buttonService;
    private final ApiService apiService;
    private final MessageSource messageSource;

    @Override
    public void handle(String text, Update update, Map<Long, SearchDto> searchMap, Bot bot) throws TelegramApiException {
        String start = messageSource.getMessage("start", null, Locale.getDefault());
        String end = messageSource.getMessage("end", null, Locale.getDefault());
        String search = messageSource.getMessage("search", null, Locale.getDefault());
        Long chatId = update.getMessage().getChatId();
        SearchDto searchDto = searchMap.get(chatId);
        if (text.equals(start) ||text.equals(end) || text.equals(search) || !(searchDto == null || searchDto.getCityTo() == null)) return;

        String cityNotfound = messageSource.getMessage("city.notfound", null, Locale.getDefault());
        String cityTo = messageSource.getMessage("city.to", null, Locale.getDefault());
        String dateDepart = messageSource.getMessage("date.depart", null, Locale.getDefault());

        CityInfo[] cities = apiService.getCityInfo(text);

        if (cities == null || cities.length == 0) {
            searchMap.remove(chatId);
            bot.execute(prepareMessageService.prepareMessageConfig(chatId, cityNotfound));
        } else if (searchDto == null) {
            searchDto = new SearchDto();
            searchDto.setCityFrom(cities[0].getName());
            searchDto.setCityFromCode(cities[0].getCode());
            searchMap.put(chatId, searchDto);
            SendMessage city = prepareMessageService.prepareMessageConfig(chatId, cityTo);
            buttonService.setCitiesButtons(city, "Санкт-Петербург", "Сочи");
            bot.execute(city);
        } else if (searchDto.getCityTo() == null) {
            searchDto = searchMap.get(chatId);
            searchDto.setCityTo(cities[0].getName());
            searchDto.setCityToCode(cities[0].getCode());
            searchMap.put(chatId, searchDto);
            SendMessage date = prepareMessageService.prepareMessageConfig(chatId, dateDepart);
            buttonService.setNewSearchButtons(date);
            bot.execute(date);
        }
    }
}
