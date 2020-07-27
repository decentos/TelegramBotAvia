package me.decentos.bot;

import lombok.SneakyThrows;
import me.decentos.dto.SearchDto;
import me.decentos.model.CheapestTicket;
import me.decentos.model.CityInfo;
import me.decentos.model.NonStopTicket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;

@Component
public class Bot extends TelegramLongPollingBot {

    private final Map<Long, SearchDto> search = new HashMap<>();

    private final String botUserName;
    private final String botToken;
    private final String cityInfoTemplate;
    private final String token;
    private final String cheapestTicketTemplate;
    private final String nonStopTicketTemplate;
    private final RestTemplate restTemplate;

    @Autowired
    public Bot(@Value("${bot.username}") String botUserName, @Value("${bot.token}") String botToken,
               @Value("${api.cityInfoTemplate}") String cityInfoTemplate, RestTemplate restTemplate,
               @Value("${api.cheapestTicketTemplate}") String cheapestTicketTemplate, @Value("${api.cheapestTicketTemplate}") String nonStopTicketTemplate,
               @Value("${api.token}") String token) {
        this.botUserName = botUserName;
        this.botToken = botToken;
        this.cityInfoTemplate = cityInfoTemplate;
        this.token = token;
        this.cheapestTicketTemplate = cheapestTicketTemplate;
        this.nonStopTicketTemplate = nonStopTicketTemplate;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        Long chatId = update.getMessage().getChatId();

        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("CITY", update.getMessage().getText());

        ResponseEntity<CityInfo[]> cityInfoResponse = restTemplate.getForEntity(cityInfoTemplate, CityInfo[].class, urlParams);
        CityInfo[] cities = cityInfoResponse.getBody();

        if (search.get(chatId) != null && search.get(chatId).getCityTo() != null) search.remove(chatId);

        if (cities == null || cities.length == 0) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.enableMarkdown(true);
            sendMessage.setChatId(update.getMessage().getChatId());
            sendMessage.setText("Город введен неверно или в нем отсутствует аэропорт.\nПожалуйста, повторите попытку:");
            execute(sendMessage);
        } else if (search.get(chatId) == null) {
            search.put(chatId, new SearchDto(cities[0].getName(), cities[0].getCode(), null, null));
        } else {
            SearchDto searchDto = search.get(chatId);
            searchDto.setCityTo(cities[0].getName());
            searchDto.setCityToCode(cities[0].getCode());
            search.put(chatId, searchDto);

            Map<String, String> codeParams = new HashMap<>();
            codeParams.put("CITY_FROM_CODE", search.get(chatId).getCityFromCode());
            codeParams.put("CITY_TO_CODE", search.get(chatId).getCityToCode());
            codeParams.put("DEPART_DATE", "2020-09-01");
            codeParams.put("RETURN_DATE", "2020-09-05");
            codeParams.put("TOKEN", token);

            ResponseEntity<CheapestTicket[]> cheapestTicketResponse = restTemplate.getForEntity(cheapestTicketTemplate, CheapestTicket[].class, codeParams);
            CheapestTicket[] cheapestTickets = cheapestTicketResponse.getBody();

            ResponseEntity<NonStopTicket[]> nonStopTicketResponse = restTemplate.getForEntity(nonStopTicketTemplate, NonStopTicket[].class, codeParams);
            NonStopTicket[] nonStopTickets = nonStopTicketResponse.getBody();

            SendMessage sendCheapest = new SendMessage();
            sendCheapest.enableMarkdown(true);
            sendCheapest.setChatId(update.getMessage().getChatId());
            sendCheapest.setText(cheapestTickets[0].toString());
            execute(sendCheapest);

            SendMessage sendNonStop = new SendMessage();
            sendNonStop.enableMarkdown(true);
            sendNonStop.setChatId(update.getMessage().getChatId());
            sendNonStop.setText(nonStopTickets[0].toString());
            execute(sendNonStop);
        }
    }
}
