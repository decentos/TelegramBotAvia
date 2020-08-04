package me.decentos.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface ButtonService {

    void setNewSearchButtons(SendMessage sendMessage);

    void setCitiesButtons(SendMessage sendMessage, String firstCity, String secondCity);
}
