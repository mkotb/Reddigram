package xyz.mkotb.reddigram.data;

import xyz.mkotb.reddigram.ReddigramBot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DataFile {
    private transient final ReddigramBot bot;
    private Map<String, UserData> userData = new HashMap<>();
    private Statistics statistics = new Statistics();

    public static DataFile load(ReddigramBot bot) {
        File file = new File("data.json");

        if (!file.exists()) {
            return new DataFile(bot);
        }

        FileReader reader;

        try {
            reader = new FileReader(file);
        } catch (FileNotFoundException ignored) {
            return null;
        }

        return BotConfig.GSON.fromJson(reader, DataFile.class);
    }

    private DataFile(ReddigramBot bot) {
        this.bot = bot;
    }

    public void save() {
        try {
            File file = new File("data.json");

            if (!file.exists()) {
                file.createNewFile();
            }

            statistics.setUsersServed(userData.size());

            Files.write(file.toPath(), Collections.singleton(BotConfig.GSON.toJson(this)));
        } catch (IOException ex) {
            bot.sendToOwner("Could not save data file due to IOException: " + ex.getMessage());
        }
    }

    public UserData dataFor(String chat) {
        return userData.get(chat);
    }

    public void newData(String chat, UserData data) {
        userData.put(chat, data);
    }

    public Statistics statistics() {
        return statistics;
    }
}
