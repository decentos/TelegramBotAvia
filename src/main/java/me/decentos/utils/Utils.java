package me.decentos.utils;

import me.decentos.dto.SearchDto;
import me.decentos.model.AirlinesInfo;
import me.decentos.model.SearchTicketResult;
import me.decentos.model.TicketInfo;

public interface Utils {

    String prepareTicket(TicketInfo cheapestNonStopTicket, String messageTemplate);

    String parseAirlinesNameJson(TicketInfo ticketInfo, AirlinesInfo airlines);

    TicketInfo parseTicketJson(SearchDto searchDto, SearchTicketResult searchTicketResult);

    String parseUrl(SearchDto searchDto);
}
