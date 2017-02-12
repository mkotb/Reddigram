package xyz.mkotb.reddigram.live;

import xyz.mkotb.reddigram.ReddigramBot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LiveManager {
    private final ReddigramBot bot;
    private final Map<String, LiveFollower> followingThreads = new ConcurrentHashMap<>();

    public LiveManager(ReddigramBot bot) {
        this.bot = bot;

        // TODO load saved threads
    }

    public void subscribeTo(String thread, String chat) {
        if (!followingThreads.containsKey(thread)) {
            LiveFollower follower = new LiveFollower(bot, thread, chat);

            follower.start();
            followingThreads.put(thread, follower);
            return;
        }

        followingThreads.get(thread).subscribe(chat);
    }

    public void unsubscribe(String thread, String chat) {
        if (followingThreads.containsKey(thread)) {
            followingThreads.get(thread).unsubscribe(chat);
        }
    }

    public boolean isFollowingThreads(String chat) {
        return followingThreads.values().stream().anyMatch((follower) -> follower.isSubscribed(chat));
    }

    void removeThread(String thread) {
        followingThreads.remove(thread);
    }
}
