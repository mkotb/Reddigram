package xyz.mkotb.reddigram;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;
import pro.zackpollard.telegrambot.api.TelegramBot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

public class ReddigramBot {
    private final Timer timer;
    private final BotConfig config;
    private final UUID deviceId = UUID.randomUUID();
    private final TelegramBot telegramBot;
    private RedditClient client;

    public static void main(String[] args) throws Exception {
        new ReddigramBot();
    }

    ReddigramBot() throws Exception {
        timer = new Timer();
        config = BotConfig.configFromFile(new File("config.json"));
        client = new RedditClient(UserAgent.of("server", "xyz.mkotb.reddigram", "1.0", config.redditUsername()));
        telegramBot = TelegramBot.login(config.botApiKey());

        telegramBot.getEventsManager().register(new TelegramListener(this));
        telegramBot.startUpdates(false);
        log("Successfully logged in");

        timer.scheduleAtFixedRate(new OAuthTask(this), 0L, 3500000L); // every 58 minutes reauth.
    }

    // gets an extract of 20 submissions from requested subreddit
    // paginates them into pages with 5 entries
    public List<List<Submission>> pagesFor(String subreddit, Sorting sorting) {
        SubredditPaginator paginator = new SubredditPaginator(client(), subreddit);

        if (sorting != null) {
            paginator.setSorting(sorting);
        }

        paginator.setLimit(20);
        Listing<Submission> listing = paginator.next();
        // list of pages with 5 entries
        List<List<Submission>> paginated = new ArrayList<>(TelegramListener.PAGES);

        for (int page = 0; page < TelegramListener.PAGES; page++) {
            List<Submission> submissions = new ArrayList<>(5);

            for (int index = 0; index < 5; index++) {
                submissions.add(listing.get((page * 5) + index));
            }

            paginated.add(submissions);
        }

        return paginated;
    }

    public void log(String text) {
        System.out.println("[" + telegramBot.getBotUsername() + "] " + text);
    }

    public void sendToOwner(String text) {
        if (config.ownerUserId() != null) {
            telegramBot.getChat(config.ownerUserId()).sendMessage("[" + telegramBot.getBotUsername() + "] " + text);
        }
    }

    public RedditClient client() {
        return client;
    }

    public UUID deviceId() {
        return deviceId;
    }

    public BotConfig config() {
        return config;
    }

    public TelegramBot telegramBot() {
        return telegramBot;
    }
}
