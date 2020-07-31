package me.decentos.bot;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.val;
import me.decentos.dto.SearchDto;
import me.decentos.model.CityInfo;
import me.decentos.model.SearchTicketResult;
import me.decentos.model.TicketInfo;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.*;

@Component
public class Bot extends TelegramLongPollingBot {

    private final Map<Long, SearchDto> search = new HashMap<>();
    private final Gson gson = new Gson();

    private final String botUserName;
    private final String botToken;
    private final String cityInfoTemplate;
    private final String cheapestTicketTemplate;
    private final String nonStopTicketTemplate;
    private final String token;
    private final RestTemplate restTemplate;
    private final MessageSource messageSource;

    @Autowired
    public Bot(@Value("${bot.username}") String botUserName,
               @Value("${bot.token}") String botToken,
               @Value("${api.cityInfoTemplate}") String cityInfoTemplate,
               @Value("${api.cheapestTicketTemplate}") String cheapestTicketTemplate,
               @Value("${api.nonStopTicketTemplate}") String nonStopTicketTemplate,
               @Value("${api.token}") String token,
               RestTemplate restTemplate,
               MessageSource messageSource) {
        this.botUserName = botUserName;
        this.botToken = botToken;
        this.cityInfoTemplate = cityInfoTemplate;
        this.cheapestTicketTemplate = cheapestTicketTemplate;
        this.nonStopTicketTemplate = nonStopTicketTemplate;
        this.token = token;
        this.restTemplate = restTemplate;
        this.messageSource = messageSource;
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
        String text = update.getMessage().getText();
        SearchDto searchDto = search.get(chatId);

        String start = messageSource.getMessage("start", null, Locale.getDefault());
        String greeting = messageSource.getMessage("greeting", null, Locale.getDefault());
        String search = messageSource.getMessage("search", null, Locale.getDefault());
        String cityFrom = messageSource.getMessage("city.from", null, Locale.getDefault());

        if (text.equals(start)) {
            SendMessage startSearch = prepareMessageConfig(chatId, greeting);
            setStartButtons(startSearch);
            execute(startSearch);
        } else if (text.equals(search)) {
            execute(prepareMessageConfig(chatId, cityFrom));
        } else if (searchDto == null || searchDto.getCityTo() == null) {
            fillCityInfo(chatId, text, searchDto);
        } else if (searchDto.getReturnDate() == null) {
            fillDateInfo(chatId, text, searchDto);
        }
    }

    @SneakyThrows
    private void fillCityInfo(Long chatId, String text, SearchDto searchDto) {
        String cityNotfound = messageSource.getMessage("city.notfound", null, Locale.getDefault());
        String cityTo = messageSource.getMessage("city.to", null, Locale.getDefault());
        String dateDepart = messageSource.getMessage("date.depart", null, Locale.getDefault());

        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("CITY", text);
        ResponseEntity<CityInfo[]> cityInfoResponse = restTemplate.getForEntity(cityInfoTemplate, CityInfo[].class, urlParams);
        CityInfo[] cities = cityInfoResponse.getBody();

        if (cities == null || cities.length == 0) {
            execute(prepareMessageConfig(chatId, cityNotfound));
        } else if (searchDto == null) {
            searchDto = new SearchDto();
            searchDto.setCityFrom(cities[0].getName());
            searchDto.setCityFromCode(cities[0].getCode());
            search.put(chatId, searchDto);
            execute(prepareMessageConfig(chatId, cityTo));
        } else if (searchDto.getCityTo() == null) {
            searchDto = search.get(chatId);
            searchDto.setCityTo(cities[0].getName());
            searchDto.setCityToCode(cities[0].getCode());
            search.put(chatId, searchDto);
            execute(prepareMessageConfig(chatId, dateDepart));
        }
    }

    @SneakyThrows
    private void fillDateInfo(Long chatId, String text, SearchDto searchDto) {
        String dateReturn = messageSource.getMessage("date.return", null, Locale.getDefault());

        if (searchDto.getDepartDate() == null) {
            searchDto = search.get(chatId);
            searchDto.setDepartDate(text);
            search.put(chatId, searchDto);
            execute(prepareMessageConfig(chatId, dateReturn));
        } else {
            searchDto = search.get(chatId);
            searchDto.setReturnDate(text);
            search.put(chatId, searchDto);
            findTickets(chatId, searchDto);
        }
    }

    @SneakyThrows
    private void findTickets(Long chatId, SearchDto searchDto) {
        String ticketNotfound = messageSource.getMessage("ticket.notfound", null, Locale.getDefault());

        TicketInfo cheapestTicket = findTicket(searchDto, cheapestTicketTemplate);
        TicketInfo cheapestNonStopTicket = findTicket(searchDto, nonStopTicketTemplate);
        search.remove(chatId);

        if (cheapestTicket.getPrice() == 0 && cheapestNonStopTicket.getPrice() == 0) {
            execute(prepareMessageConfig(chatId, ticketNotfound));
        } else {
            String ticketCheapest = messageSource.getMessage("ticket.cheapest",
                    new Object[]{cheapestTicket.getPrice(),
                            cheapestTicket.getAirline(),
                            String.format("%s-%s", cheapestTicket.getAirline(), cheapestTicket.getFlightNumber()),
                            cheapestTicket.getDepartureAt().substring(11, 16),
                            cheapestTicket.getReturnAt().substring(11, 16)},
                    Locale.getDefault());

            execute(prepareMessageConfig(chatId, ticketCheapest));

            if (cheapestTicket.getPrice() != cheapestNonStopTicket.getPrice()
                    && cheapestTicket.getFlightNumber() != cheapestNonStopTicket.getFlightNumber()) {
                String ticketNonstop = messageSource.getMessage("ticket.nonstop",
                        new Object[]{cheapestNonStopTicket.getPrice(),
                                cheapestNonStopTicket.getAirline(),
                                String.format("%s-%s", cheapestNonStopTicket.getAirline(), cheapestNonStopTicket.getFlightNumber()),
                                cheapestNonStopTicket.getDepartureAt().substring(11, 16),
                                cheapestNonStopTicket.getReturnAt().substring(11, 16)},
                        Locale.getDefault());

                execute(prepareMessageConfig(chatId, ticketNonstop));
            }
            String url = parseUrl(searchDto);
            execute(prepareMessageConfig(chatId, url));
        }
    }

    private TicketInfo findTicket(SearchDto searchDto, String template) {
        Map<String, String> codeParams = new HashMap<>();
        codeParams.put("CITY_FROM_CODE", searchDto.getCityFromCode());
        codeParams.put("CITY_TO_CODE", searchDto.getCityToCode());
        codeParams.put("DEPART_DATE", searchDto.getDepartDate());
        codeParams.put("RETURN_DATE", searchDto.getReturnDate());
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

    private String parseUrl(SearchDto searchDto) {
        String departDay = searchDto.getDepartDate().substring(8);
        String departMonth = searchDto.getDepartDate().substring(5, 7);
        String departDate = String.format("%s%s", departDay, departMonth);

        String returnDay = searchDto.getReturnDate().substring(8);
        String returnMonth = searchDto.getReturnDate().substring(5, 7);
        String returnDate = String.format("%s%s", returnDay, returnMonth);
        return String.format("https://www.aviasales.ru/search/%s%s%s%s1", searchDto.getCityFromCode(), departDate, searchDto.getCityToCode(), returnDate);
    }

    private SendMessage prepareMessageConfig(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        return sendMessage;
    }

    public void setStartButtons(SendMessage sendMessage) {
        String search = messageSource.getMessage("search", null, Locale.getDefault());

        val replyKeyboardMarkup = new ReplyKeyboardMarkup();
        val keyboard = createKeyboardTemplate(replyKeyboardMarkup, sendMessage);
        val keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton(search));
        keyboard.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    private List<KeyboardRow> createKeyboardTemplate(ReplyKeyboardMarkup replyKeyboardMarkup, SendMessage sendMessage) {
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        return new ArrayList<>();
    }
}
