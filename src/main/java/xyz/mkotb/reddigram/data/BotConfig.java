package xyz.mkotb.reddigram.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.Collections;

public class BotConfig {
    public static final transient Gson GSON = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT)
            .setPrettyPrinting()
            .create();
    private String botApiKey = "insert telegram key here";
    private String ownerUserId = "remove this entry or enter the owner id";
    private String redditUsername = "your reddit username";
    private String clientId = "your client id";
    private String clientSecret = "your client secret";

    private BotConfig() {
    }

    public String ownerUserId() {
        return ownerUserId;
    }

    public String botApiKey() {
        return botApiKey;
    }

    public String redditUsername() {
        return redditUsername;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public String clientId() {
        return clientId;
    }

    public static BotConfig configFromFile(File file) {
        BotConfig def = new BotConfig();

        if (!file.exists()) {
            System.out.println("[Config] Can't find config file...");

            try {
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }

                file.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                System.err.println("[Config] Unable to create new file for config! Shutting down...");

                System.exit(127);
                return def;
            }

            try {
                Files.write(file.toPath(), Collections.singleton(GSON.toJson(def)));
            } catch (IOException ex) {
                ex.printStackTrace();
                System.err.println("[Config] Unable to write to config file! Shutting down...");

                System.exit(127);
                return def;
            }

            System.out.println("[Config] Successfully generated new config! Please configure config.json " +
                    "appropriately and start the application again!");

            System.exit(1);
            return def;
        }

        System.out.println("[Config] Found config file! Loading...");

        FileReader reader;

        try {
            reader = new FileReader(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(127);
            return def; // not possible
        }

        BotConfig config = GSON.fromJson(reader, BotConfig.class);

        try {
            Files.write(file.toPath(), Collections.singleton(GSON.toJson(config))); // force new default values
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("[Config] Unable to write to config file! Config is still loaded, resuming execution...");
        }

        return config;
    }
}
