package me.decentos.handler.impl;

import lombok.RequiredArgsConstructor;
import me.decentos.bot.Bot;
import me.decentos.dto.SearchDto;
import me.decentos.handler.RequestHandler;
import me.decentos.model.CityInfo;
import me.decentos.service.ButtonService;
import me.decentos.service.PrepareMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class CityInfoRequestHandler implements RequestHandler {

    private final PrepareMessageService prepareMessageService;
    private final ButtonService buttonService;
    private final MessageSource messageSource;
    private final RestTemplate restTemplate;

    @Value("${api.cityInfoTemplate}")
    private String cityInfoTemplate;

    @Override
    public void handle(String text, Update update, Map<Long, SearchDto> searchMap, Bot bot) throws TelegramApiException {
        String search = messageSource.getMessage("search", null, Locale.getDefault());
        Long chatId = update.getMessage().getChatId();
        SearchDto searchDto = searchMap.get(chatId);
        if (text.equals(search) || !(searchDto == null || searchDto.getCityTo() == null)) return;

        String cityNotfound = messageSource.getMessage("city.notfound", null, Locale.getDefault());
        String cityTo = messageSource.getMessage("city.to", null, Locale.getDefault());
        String dateDepart = messageSource.getMessage("date.depart", null, Locale.getDefault());

        CityInfo[] cities = getCityInfo(text);

        if (cities == null || cities.length == 0) {
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

    private CityInfo[] getCityInfo(String text) {
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("CITY", text);
        ResponseEntity<CityInfo[]> cityInfoResponse = restTemplate.getForEntity(cityInfoTemplate, CityInfo[].class, urlParams);
        return cityInfoResponse.getBody();
    }
}
