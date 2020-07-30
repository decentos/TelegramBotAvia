package me.decentos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        ApiContextInitializer.init();
        SpringApplication.run(Main.class, args);
    }
}

// TODO Добавить отображение авиакомпании по коду IATA
// TODO Рейс = Код ак + номер рейса
// TODO Обработка время вылета и прилета
// TODO Обработка всего ответа
// TODO Добавить кнопки для начала поиска и выбора города
// TODO Подумать, как упростить ввод даты
