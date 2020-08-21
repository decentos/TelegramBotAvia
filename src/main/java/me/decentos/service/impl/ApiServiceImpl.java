package me.decentos.service.impl;

import lombok.RequiredArgsConstructor;
import me.decentos.dto.SearchDto;
import me.decentos.model.AirlinesInfo;
import me.decentos.model.CityInfo;
import me.decentos.model.SearchTicketResult;
import me.decentos.model.TicketInfo;
import me.decentos.service.ApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ApiServiceImpl implements ApiService {

    private final RestTemplate restTemplate;

    @Value("${api.cityInfoTemplate}")
    private String cityInfoTemplate;

    @Value("${api.airlinesInfoTemplate}")
    private String airlinesInfoTemplate;

    @Value("${api.travelPayoutsToken}")
    private String travelPayoutsToken;

    @Override
    public CityInfo[] getCityInfo(String text) {
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("CITY", text);
        ResponseEntity<CityInfo[]> cityInfoResponse = restTemplate.getForEntity(cityInfoTemplate, CityInfo[].class, urlParams);
        return cityInfoResponse.getBody();
    }

    @Override
    public AirlinesInfo getAirlinesInfo(TicketInfo ticketInfo) {
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("IATA_CODE", ticketInfo.getAirline());
        ResponseEntity<AirlinesInfo> airlinesInfoResponse = restTemplate.getForEntity(airlinesInfoTemplate, AirlinesInfo.class, urlParams);
        return airlinesInfoResponse.getBody();
    }

    @Override
    public SearchTicketResult getSearchTicketResult(SearchDto searchDto, String template) {
        Map<String, String> codeParams = new HashMap<>();
        codeParams.put("CITY_FROM_CODE", searchDto.getCityFromCode());
        codeParams.put("CITY_TO_CODE", searchDto.getCityToCode());
        codeParams.put("DEPART_DATE", searchDto.getDepartDate());
        codeParams.put("RETURN_DATE", searchDto.getReturnDate());
        codeParams.put("TOKEN", travelPayoutsToken);

        ResponseEntity<SearchTicketResult> searchTicketResultResponse = restTemplate.getForEntity(template, SearchTicketResult.class, codeParams);
        return searchTicketResultResponse.getBody();
    }
}
