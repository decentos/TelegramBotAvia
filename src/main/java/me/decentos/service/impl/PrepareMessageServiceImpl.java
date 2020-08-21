package me.decentos.service.impl;

import lombok.RequiredArgsConstructor;
import me.decentos.service.PrepareMessageService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@RequiredArgsConstructor
@Service
public class PrepareMessageServiceImpl implements PrepareMessageService {

    @Override
    public SendMessage prepareMessageConfig(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        return sendMessage;
    }
}
