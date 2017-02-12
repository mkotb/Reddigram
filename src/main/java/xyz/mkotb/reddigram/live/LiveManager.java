package xyz.mkotb.reddigram.live;

import net.dean.jraw.models.Submission;
import xyz.mkotb.reddigram.ReddigramBot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveManager {
    public static final Pattern LINK_PATTERN = Pattern.compile("reddit.com\\/live\\/(.{12})");
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

    public String idFromSubmission(Submission submission) {
        String link = "";

        if (submission.getUrl() != null && !submission.getUrl().isEmpty()) {
            link = submission.getUrl();
        }

        Matcher matcher = LINK_PATTERN.matcher(link);

        if (!matcher.find()) {
            if (submission.getSelftext() != null && !submission.getSelftext().isEmpty()) {
                link = submission.getSelftext();
                matcher = LINK_PATTERN.matcher(link);
                matcher.find();
            } else {
                return null;
            }
        }

        return matcher.group(1);
    }
}
