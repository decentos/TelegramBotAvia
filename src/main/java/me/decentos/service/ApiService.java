package me.decentos.service;

import me.decentos.dto.SearchDto;
import me.decentos.model.AirlinesInfo;
import me.decentos.model.CityInfo;
import me.decentos.model.SearchTicketResult;
import me.decentos.model.TicketInfo;

public interface ApiService {

    CityInfo[] getCityInfo(String text);

    AirlinesInfo getAirlinesInfo(TicketInfo ticketInfo);

    SearchTicketResult getSearchTicketResult(SearchDto searchDto, String template);
}
