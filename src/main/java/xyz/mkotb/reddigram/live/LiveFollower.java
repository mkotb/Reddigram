package xyz.mkotb.reddigram.live;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.mkotb.reddigram.ReddigramBot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class LiveFollower extends Thread {
    public static final long KILL_TIME = TimeUnit.HOURS.toMillis(3);
    public static final SendableTextMessage FAREWELL_MESSSAGE = SendableTextMessage
            .markdown("*Reddigram is unfollowing this thread due to inactivity...*")
            .build();
    private final ReddigramBot bot;
    private final String id;
    private final Set<String> subscribedChats = new ConcurrentSkipListSet<>();
    private final Set<LiveUpdate> sentUpdates = new HashSet<>(); // local to this thread
    private final AtomicLong lastUpdate = new AtomicLong(System.currentTimeMillis());
    private boolean virgin = true;

    public LiveFollower(ReddigramBot bot, String id) {
        super("RedditLive Thread: " + id);
        this.bot = bot;
        this.id = id;
    }

    @Override
    public void run() {
        if (subscribedChats.isEmpty()) {
            bot.liveManager().removeThread(id);
            return;
        }

        try {
            JSONObject obj = Unirest.get("https://reddit.com/live/" + id + ".json")
                    .header("User-Agent", bot.client().getUserAgent())
                    .asJson().getBody().getObject();

            if (!obj.getString("kind").equals("Listing")) {
                bot.sendToOwner("Live Thread update for " + id + " was not a listing: " + obj.toString());
            } else {
                JSONArray jsonUpdates = obj.getJSONObject("data").getJSONArray("children");
                List<LiveUpdate> updates = new ArrayList<>(jsonUpdates.length());

                jsonUpdates.forEach((o) -> {
                    JSONObject jsonUpdate = (JSONObject) o;

                    if (jsonUpdate.has("kind") && jsonUpdate.getString("kind").equals("LiveUpdate")) {
                        updates.add(new LiveUpdate(id, jsonUpdate.getJSONObject("data")));
                    }
                });

                updates.removeIf(sentUpdates::contains);
                //updates.removeIf((update) -> (System.currentTimeMillis() - update.created()) >= 60000L);
                updates.sort((e1, e2) -> (int) (e1.created() - e2.created()));

                if (virgin) {
                    sentUpdates.addAll(updates);
                    virgin = false;
                } else if (!updates.isEmpty()) {
                    updates.forEach((update) -> {
                        sendUpdate(update);
                        sentUpdates.add(update);
                    });

                    lastUpdate.set(System.currentTimeMillis());
                } else if ((System.currentTimeMillis() - lastUpdate.get()) >= KILL_TIME) {
                    // todo tell subscribers bye bye and unregister thread
                    farewell();
                    bot.liveManager().removeThread(id);
                    return;
                }
            }
        } catch (UnirestException ex) {
            bot.sendToOwner("Could not get an update for live thread " + id + " due to UnirestException: " + ex.getMessage());
        }

        try {
            Thread.sleep(1500L);
        } catch (InterruptedException ex) {
            bot.sendToOwner("Live Thread follower for " + id + " was interrupted");
            return;
        }

        run();
    }

    public String threadId() {
        return id;
    }

    public Set<String> subscribedChats() {
        return subscribedChats;
    }

    public void subscribe(String chat) {
        subscribedChats.add(chat);
    }

    public void unsubscribe(String chat) {
        subscribedChats.remove(chat);
    }

    public boolean isSubscribed(String chat) {
        return subscribedChats.contains(chat);
    }

    public void sendUpdate(LiveUpdate update) {
        SendableTextMessage message = update.toMessage();

        for (String subscriber : subscribedChats) {
            bot.telegramBot().getChat(subscriber).sendMessage(message);

            try {
                Thread.sleep(75L);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void farewell() {
        for (String subscriber : subscribedChats) {
            bot.telegramBot().getChat(subscriber).sendMessage(FAREWELL_MESSSAGE);
        }
    }
}
