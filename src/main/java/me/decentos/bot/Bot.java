package me.decentos.bot;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import me.decentos.dto.SearchDto;
import me.decentos.model.CityInfo;
import me.decentos.model.SearchTicketResult;
import me.decentos.model.TicketInfo;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;

@Component
public class Bot extends TelegramLongPollingBot {

    private final Map<Long, SearchDto> search = new HashMap<>();
    private final Gson gson = new Gson();

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
               @Value("${api.cheapestTicketTemplate}") String cheapestTicketTemplate, @Value("${api.nonStopTicketTemplate}") String nonStopTicketTemplate,
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
            sendMessage.setChatId(chatId);
            sendMessage.setText("Город введен неверно или в нем отсутствует аэропорт.\nПожалуйста, повторите попытку:");
            execute(sendMessage);
        } else if (search.get(chatId) == null) {
            search.put(chatId, new SearchDto(cities[0].getName(), cities[0].getCode(), null, null));
        } else {
            SearchDto searchDto = search.get(chatId);
            searchDto.setCityTo(cities[0].getName());
            searchDto.setCityToCode(cities[0].getCode());
            search.put(chatId, searchDto);

            TicketInfo cheapestTicket = findTicket(searchDto, cheapestTicketTemplate);
            TicketInfo cheapestNonStopTicket = findTicket(searchDto, nonStopTicketTemplate);

            if (cheapestTicket.getPrice() == 0 && cheapestNonStopTicket.getPrice() == 0) {
                SendMessage sendNotFound = new SendMessage();
                sendNotFound.enableMarkdown(true);
                sendNotFound.setChatId(chatId);
                sendNotFound.setText("По данному запросу билеты не найдены!\nПожалуйста, повторите попытку с другими параметрами:");
                execute(sendNotFound);
            } else {
                SendMessage sendCheapest = new SendMessage();
                sendCheapest.enableMarkdown(true);
                sendCheapest.setChatId(chatId);
                sendCheapest.setText(cheapestTicket.toString());
                execute(sendCheapest);

                if (cheapestTicket.getPrice() != cheapestNonStopTicket.getPrice()
                        && cheapestTicket.getFlightNumber() != cheapestNonStopTicket.getFlightNumber()) {
                    SendMessage sendNonStop = new SendMessage();
                    sendNonStop.enableMarkdown(true);
                    sendNonStop.setChatId(chatId);
                    sendNonStop.setText(cheapestNonStopTicket.toString());
                    execute(sendNonStop);
                }

                SendMessage sendUrl = new SendMessage();
                sendUrl.enableMarkdown(true);
                sendUrl.setChatId(chatId);
                sendUrl.setText(String.format("https://www.aviasales.ru/search/%s0109%s05091", searchDto.getCityFromCode(), searchDto.getCityToCode()));
                execute(sendUrl);
            }
        }
    }

    private TicketInfo findTicket(SearchDto searchDto, String template) {
        Map<String, String> codeParams = new HashMap<>();
        codeParams.put("CITY_FROM_CODE", searchDto.getCityFromCode());
        codeParams.put("CITY_TO_CODE", searchDto.getCityToCode());
        codeParams.put("DEPART_DATE", "2020-09-01");
        codeParams.put("RETURN_DATE", "2020-09-05");
        codeParams.put("TOKEN", token);

        ResponseEntity<SearchTicketResult> searchTicketResultResponse = restTemplate.getForEntity(template, SearchTicketResult.class, codeParams);
        SearchTicketResult searchTicketResult = searchTicketResultResponse.getBody();
        if (searchTicketResult == null) return new TicketInfo();

        String dataToJson = gson.toJson(searchTicketResult.getData());
        if (dataToJson.equals("{}")) return new TicketInfo();

        JSONObject dataJson = new JSONObject(dataToJson);
        JSONObject ticketsByCity = dataJson.getJSONObject(searchDto.getCityToCode());
        List<TicketInfo> ticketInfoList = new ArrayList<>();

        for (int i = 0; i < ticketsByCity.length(); i++) {
            JSONObject ticketOptions = ticketsByCity.getJSONObject(String.valueOf(i));
            TicketInfo ticketInfo = new TicketInfo();
            ticketInfo.setPrice((int) ticketOptions.get("price"));
            ticketInfo.setAirline(ticketOptions.get("airline").toString());
            ticketInfo.setFlightNumber((int) ticketOptions.get("flight_number"));
            ticketInfo.setDepartureAt(ticketOptions.get("departure_at").toString());
            ticketInfo.setReturnAt(ticketOptions.get("return_at").toString());
            ticketInfo.setExpiresAt(ticketOptions.get("expires_at").toString());
            ticketInfoList.add(ticketInfo);
        }
        return ticketInfoList
                .stream()
                .min(Comparator.comparing(TicketInfo::getPrice))
                .orElseThrow(NoSuchElementException::new);
    }
}
