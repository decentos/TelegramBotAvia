package me.decentos.service.impl;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import me.decentos.bot.Bot;
import me.decentos.dto.SearchDto;
import me.decentos.model.AirlinesInfo;
import me.decentos.model.SearchTicketResult;
import me.decentos.model.TicketInfo;
import me.decentos.service.FindTicketsService;
import me.decentos.service.PrepareMessageService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@RequiredArgsConstructor
@Service
public class FindTicketsServiceImpl implements FindTicketsService {

    private final PrepareMessageService prepareMessageService;
    private final MessageSource messageSource;
    private final RestTemplate restTemplate;
    private final Gson gson;

    @Value("${api.travelPayoutsToken}")
    private String travelPayoutsToken;

    @Value("${api.cheapestTicketTemplate}")
    private String cheapestTicketTemplate;

    @Value("${api.nonStopTicketTemplate}")
    private String nonStopTicketTemplate;

    @Value("${api.airlinesInfoTemplate}")
    private String airlinesInfoTemplate;

    @Override
    public void findTickets(Long chatId, SearchDto searchDto, Map<Long, SearchDto> searchMap, Bot bot) throws TelegramApiException {
        String ticketNotfound = messageSource.getMessage("ticket.notfound", null, Locale.getDefault());

        TicketInfo cheapestTicket = findTicket(searchDto, cheapestTicketTemplate);
        TicketInfo cheapestNonStopTicket = findTicket(searchDto, nonStopTicketTemplate);
        searchMap.remove(chatId);


        if (cheapestTicket.getPrice() == 0 && cheapestNonStopTicket.getPrice() == 0) {
            bot.execute(prepareMessageService.prepareMessageConfig(chatId, ticketNotfound));
        } else {
            String airlinesName = getAirlinesName(cheapestTicket);
            cheapestTicket.setAirlineName(airlinesName);

            String ticketCheapest = messageSource.getMessage("ticket.cheapest",
                    new Object[]{cheapestTicket.getPrice(),
                            cheapestTicket.getAirlineName(),
                            String.format("%s-%s", cheapestTicket.getAirline(), cheapestTicket.getFlightNumber()),
                            cheapestTicket.getDepartureAt().substring(11, 16),
                            cheapestTicket.getReturnAt().substring(11, 16)},
                    Locale.getDefault());

            bot.execute(prepareMessageService.prepareMessageConfig(chatId, ticketCheapest));

            if (cheapestTicket.getPrice() != cheapestNonStopTicket.getPrice()
                    && cheapestTicket.getFlightNumber() != cheapestNonStopTicket.getFlightNumber()) {
                airlinesName = getAirlinesName(cheapestNonStopTicket);
                cheapestNonStopTicket.setAirlineName(airlinesName);

                String ticketNonstop = messageSource.getMessage("ticket.nonstop",
                        new Object[]{cheapestNonStopTicket.getPrice(),
                                cheapestNonStopTicket.getAirlineName(),
                                String.format("%s-%s", cheapestNonStopTicket.getAirline(), cheapestNonStopTicket.getFlightNumber()),
                                cheapestNonStopTicket.getDepartureAt().substring(11, 16),
                                cheapestNonStopTicket.getReturnAt().substring(11, 16)},
                        Locale.getDefault());

                bot.execute(prepareMessageService.prepareMessageConfig(chatId, ticketNonstop));
            }
            String url = parseUrl(searchDto);
            bot.execute(prepareMessageService.prepareMessageConfig(chatId, url));
        }
    }

    private String getAirlinesName(TicketInfo ticketInfo) {
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("IATA_CODE", ticketInfo.getAirline());
        ResponseEntity<AirlinesInfo> airlinesInfoResponse = restTemplate.getForEntity(airlinesInfoTemplate, AirlinesInfo.class, urlParams);
        AirlinesInfo airlines = airlinesInfoResponse.getBody();
        if (airlines == null) return ticketInfo.getAirline();

        String dataToJson = gson.toJson(airlines);
        if (dataToJson.equals("{}")) return ticketInfo.getAirline();

        JSONObject dataJson = new JSONObject(dataToJson);
        JSONArray airlinesArr = dataJson.getJSONArray("response");
        JSONObject airlinesName = airlinesArr.getJSONObject(0);

        return airlinesName.get("name").toString();
    }

    private TicketInfo findTicket(SearchDto searchDto, String template) {
        Map<String, String> codeParams = new HashMap<>();
        codeParams.put("CITY_FROM_CODE", searchDto.getCityFromCode());
        codeParams.put("CITY_TO_CODE", searchDto.getCityToCode());
        codeParams.put("DEPART_DATE", searchDto.getDepartDate());
        codeParams.put("RETURN_DATE", searchDto.getReturnDate());
        codeParams.put("TOKEN", travelPayoutsToken);

        ResponseEntity<SearchTicketResult> searchTicketResultResponse = restTemplate.getForEntity(template, SearchTicketResult.class, codeParams);
        SearchTicketResult searchTicketResult = searchTicketResultResponse.getBody();
        if (searchTicketResult == null) return new TicketInfo();

        String dataToJson = gson.toJson(searchTicketResult.getData());
        if (dataToJson.equals("{}")) return new TicketInfo();

        JSONObject dataJson = new JSONObject(dataToJson);
        JSONObject ticketsByCity = dataJson.getJSONObject(searchDto.getCityToCode());
        List<TicketInfo> ticketInfoList = new ArrayList<>();

        for (int i = 0; i < ticketsByCity.length(); i++) {
            JSONObject ticketOptions;
            try {
                ticketOptions = ticketsByCity.getJSONObject(String.valueOf(i));
            } catch (JSONException ex) {
                ticketOptions = ticketsByCity.getJSONObject(String.valueOf(++i));
            }
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
}
