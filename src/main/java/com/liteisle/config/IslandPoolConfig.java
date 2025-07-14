package com.liteisle.config;

import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Random;

@Configuration
public class IslandPoolConfig {

    private static final Map<Integer, String> ISLAND_URLS = Map.ofEntries(
            Map.entry(1, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF1.png"),
            Map.entry(2, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF2.png"),
            Map.entry(3, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF3.png"),
            Map.entry(4, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF4.png"),
            Map.entry(5, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF5.png"),
            Map.entry(6, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF6.png"),
            Map.entry(7, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF7.png"),
            Map.entry(8, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF8.png"),
            Map.entry(9, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF9.png"),
            Map.entry(10, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF10.png"),
            Map.entry(11, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF11.png"),
            Map.entry(12, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF12.png"),
            Map.entry(13, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF13.png"),
            Map.entry(14, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF14.png"),
            Map.entry(15, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF15.png"),
            Map.entry(16, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF16.png"),
            Map.entry(17, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF17.png"),
            Map.entry(18, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF18.png"),
            Map.entry(19, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF19.png"),
            Map.entry(20, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF20.png"),
            Map.entry(21, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF21.png"),
            Map.entry(22, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF22.png"),
            Map.entry(23, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF23.png"),
            Map.entry(24, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF24.png"),
            Map.entry(25, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF25.png"),
            Map.entry(26, "https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/%E5%B2%9B%E5%B1%BF26.png")
    );

    private final Random random = new Random();

    public int getRandomIslandId() {
        return random.nextInt(26) + 1;
    }

}
