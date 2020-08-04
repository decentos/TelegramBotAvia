package me.decentos.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface PrepareMessageService {

    SendMessage prepareMessageConfig(Long chatId, String text);
}
