package me.decentos.model;

import lombok.Data;

@Data
public class TicketInfo {
    private int price;
    private String airline;
    private int flightNumber;
    private String departureAt;
    private String returnAt;
    private String expiresAt;
}
