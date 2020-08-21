package me.decentos.service.impl;

import lombok.RequiredArgsConstructor;
import me.decentos.bot.Bot;
import me.decentos.dto.SearchDto;
import me.decentos.model.AirlinesInfo;
import me.decentos.model.SearchTicketResult;
import me.decentos.model.TicketInfo;
import me.decentos.service.ApiService;
import me.decentos.service.FindTicketsService;
import me.decentos.service.PrepareMessageService;
import me.decentos.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class FindTicketsServiceImpl implements FindTicketsService {

    private final ApiService apiService;
    private final PrepareMessageService prepareMessageService;
    private final Utils utils;
    private final MessageSource messageSource;

    @Value("${api.cheapestTicketTemplate}")
    private String cheapestTicketTemplate;

    @Value("${api.nonStopTicketTemplate}")
    private String nonStopTicketTemplate;

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
            String ticketCheapest = utils.prepareTicket(cheapestTicket, "ticket.cheapest");
            bot.execute(prepareMessageService.prepareMessageConfig(chatId, ticketCheapest));

            if (cheapestNonStopTicket.getPrice() != 0
                    && cheapestTicket.getPrice() != cheapestNonStopTicket.getPrice()
                    && cheapestTicket.getFlightNumber() != cheapestNonStopTicket.getFlightNumber()
            ) {
                airlinesName = getAirlinesName(cheapestNonStopTicket);
                cheapestNonStopTicket.setAirlineName(airlinesName);
                String ticketNonstop = utils.prepareTicket(cheapestNonStopTicket, "ticket.nonstop");
                bot.execute(prepareMessageService.prepareMessageConfig(chatId, ticketNonstop));
            }
            String url = utils.parseUrl(searchDto);
            bot.execute(prepareMessageService.prepareMessageConfig(chatId, url));
        }
    }

    private String getAirlinesName(TicketInfo ticketInfo) {
        AirlinesInfo airlines = apiService.getAirlinesInfo(ticketInfo);
        if (airlines == null) return ticketInfo.getAirline();
        return utils.parseAirlinesNameJson(ticketInfo, airlines);
    }

    private TicketInfo findTicket(SearchDto searchDto, String template) {
        SearchTicketResult searchTicketResult = apiService.getSearchTicketResult(searchDto, template);
        if (searchTicketResult == null) return new TicketInfo();
        return utils.parseTicketJson(searchDto, searchTicketResult);
    }
}
