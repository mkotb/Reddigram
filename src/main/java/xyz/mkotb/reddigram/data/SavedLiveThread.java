package xyz.mkotb.reddigram.data;

import xyz.mkotb.reddigram.ReddigramBot;
import xyz.mkotb.reddigram.live.LiveFollower;

import java.util.Set;

public class SavedLiveThread {
    private String id;
    private Set<String> subscribedChats;

    public SavedLiveThread(String id, Set<String> subscribedChats) {
        this.id = id;
        this.subscribedChats = subscribedChats;
    }

    public SavedLiveThread() {
    }

    public String id() {
        return id;
    }

    public Set<String> subscribedChats() {
        return subscribedChats;
    }

    public LiveFollower toActiveFollower(ReddigramBot bot) {
        LiveFollower follower = new LiveFollower(bot, id);

        subscribedChats.forEach(follower::subscribe);
        follower.start();
        return follower;
    }
}
