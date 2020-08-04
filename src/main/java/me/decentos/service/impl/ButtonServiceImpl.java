package me.decentos.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.val;
import me.decentos.service.ButtonService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Service
public class ButtonServiceImpl implements ButtonService {

    private final MessageSource messageSource;

    @Override
    public void setNewSearchButtons(SendMessage sendMessage) {
        String search = messageSource.getMessage("search", null, Locale.getDefault());

        val replyKeyboardMarkup = new ReplyKeyboardMarkup();
        val keyboard = createKeyboardTemplate(replyKeyboardMarkup, sendMessage);
        val keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton(search));
        keyboard.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    @Override
    public void setCitiesButtons(SendMessage sendMessage, String firstCity, String secondCity) {
        String search = messageSource.getMessage("search", null, Locale.getDefault());

        val replyKeyboardMarkup = new ReplyKeyboardMarkup();
        val keyboard = createKeyboardTemplate(replyKeyboardMarkup, sendMessage);
        val keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton(firstCity));
        keyboardFirstRow.add(new KeyboardButton(secondCity));

        val keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(new KeyboardButton(search));

        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    private List<KeyboardRow> createKeyboardTemplate(ReplyKeyboardMarkup replyKeyboardMarkup, SendMessage sendMessage) {
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        return new ArrayList<>();
    }
}
