package xyz.mkotb.reddigram.data;

import xyz.mkotb.reddigram.ReddigramBot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class DataFile {
    private transient ReddigramBot bot;
    private Map<String, UserData> userData = new HashMap<>();
    private Statistics statistics = new Statistics();
    private List<SavedLiveThread> savedThreads = new ArrayList<>();

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

        DataFile df = BotConfig.GSON.fromJson(reader, DataFile.class);

        df.bot = bot;
        return df;
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

            statistics().setUsersServed(userData.size());

            savedThreads.clear();
            bot.liveManager().followers().forEach((thread) -> savedThreads.add(new SavedLiveThread(thread.threadId(), thread.subscribedChats())));

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

    public void removeData(String chat) {
        userData.remove(chat);
    }

    public Statistics statistics() {
        if (statistics == null) {
            statistics = new Statistics();
        }

        return statistics;
    }

    public Set<Map.Entry<String, UserData>> userData() {
        return userData.entrySet();
    }

    public List<SavedLiveThread> savedThreads() {
        if (savedThreads == null) {
            savedThreads = new ArrayList<>();
        }

        return savedThreads;
    }
}
