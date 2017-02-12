package xyz.mkotb.reddigram.live;

import net.dean.jraw.models.Submission;
import xyz.mkotb.reddigram.ReddigramBot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveManager {
    public static final Pattern THREAD_ID_PATTERN = Pattern.compile("\\w{12}");
    public static final Pattern LINK_PATTERN = Pattern.compile("reddit.com\\/live\\/(.{12})");
    private final ReddigramBot bot;
    private final Map<String, LiveFollower> followingThreads = new ConcurrentHashMap<>();

    public LiveManager(ReddigramBot bot) {
        this.bot = bot;

        // TODO load saved threads
    }

    public void subscribeTo(String thread, String chat) {
        LiveFollower previousThread = threadBy(chat);

        if (previousThread != null) {
            previousThread.unsubscribe(chat);
        }

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

    public LiveFollower threadBy(String chat) {
        return followingThreads.values().stream()
                .filter((follower) -> follower.isSubscribed(chat))
                .findFirst().orElse(null);
    }

    void removeThread(String thread) {
        followingThreads.remove(thread);
    }

    public String idFromSubmission(Submission submission) {
        String id;

        if (submission.getUrl() != null && !submission.getUrl().isEmpty()) {
            id = idFromInput(submission.getUrl());

            if (id == null && submission.getSelftext() != null && !submission.getSelftext().isEmpty()) {
                return idFromInput(submission.getSelftext());
            } else if (id != null) {
                return id;
            } else {
                return null;
            }
        }

        return null;
    }

    public String idFromInput(String input) {
        if (THREAD_ID_PATTERN.matcher(input).matches()) {
            return input;
        }

        Matcher matcher = LINK_PATTERN.matcher(input);

        if (!matcher.find()) {
            return null;
        }

        try {
            return matcher.group(1);
        } catch (Exception ex) {
            return null;
        }
    }
}
