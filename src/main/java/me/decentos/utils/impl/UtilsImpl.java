package me.decentos.utils.impl;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import me.decentos.dto.SearchDto;
import me.decentos.model.AirlinesInfo;
import me.decentos.model.SearchTicketResult;
import me.decentos.model.TicketInfo;
import me.decentos.utils.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.*;

@RequiredArgsConstructor
@Component
public class UtilsImpl implements Utils {

    private final MessageSource messageSource;
    private final Gson gson;

    @Override
    public String prepareTicket(TicketInfo ticketInfo, String messageTemplate) {
        return messageSource.getMessage(messageTemplate,
                new Object[]{ticketInfo.getPrice(),
                        ticketInfo.getAirlineName(),
                        String.format("%s-%s", ticketInfo.getAirline(), ticketInfo.getFlightNumber()),
                        ticketInfo.getDepartureAt().substring(11, 16),
                        ticketInfo.getReturnAt().substring(11, 16)},
                Locale.getDefault());
    }

    @Override
    public String parseAirlinesNameJson(TicketInfo ticketInfo, AirlinesInfo airlines) {
        String dataToJson = gson.toJson(airlines);
        if (dataToJson.equals("{}")) return ticketInfo.getAirline();

        JSONObject dataJson = new JSONObject(dataToJson);
        JSONArray airlinesArr = dataJson.getJSONArray("response");
        JSONObject airlinesName = airlinesArr.getJSONObject(0);

        return airlinesName.get("name").toString();
    }

    @Override
    public TicketInfo parseTicketJson(SearchDto searchDto, SearchTicketResult searchTicketResult) {
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

    @Override
    public String parseUrl(SearchDto searchDto) {
        String departDay = searchDto.getDepartDate().substring(8);
        String departMonth = searchDto.getDepartDate().substring(5, 7);
        String departDate = String.format("%s%s", departDay, departMonth);

        String returnDay = searchDto.getReturnDate().substring(8);
        String returnMonth = searchDto.getReturnDate().substring(5, 7);
        String returnDate = String.format("%s%s", returnDay, returnMonth);
        return String.format("https://www.aviasales.ru/search/%s%s%s%s1", searchDto.getCityFromCode(), departDate, searchDto.getCityToCode(), returnDate);
    }
}
