package xyz.mkotb.reddigram;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;
import pro.zackpollard.telegrambot.api.TelegramBot;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.extensions.Extensions;
import pro.zackpollard.telegrambot.api.menu.InlineMenu;
import pro.zackpollard.telegrambot.api.menu.InlineMenuRegistry;
import xyz.mkotb.reddigram.data.BotConfig;
import xyz.mkotb.reddigram.data.DataFile;

import java.io.File;
import java.util.*;

public class ReddigramBot {
    public static final String[] NUMBER_EMOJIS = new String[] {"1⃣", "2⃣", "3⃣", "4⃣", "5⃣", "6⃣", "7⃣", "8⃣", "9⃣"};
    public static final int PAGES = 4;
    private final Timer timer;
    private final BotConfig config;
    private final UUID deviceId = UUID.randomUUID();
    private final TelegramBot telegramBot;
    private final DataFile dataFile;
    private RedditClient client;

    public static void main(String[] args) throws Exception {
        new ReddigramBot();
    }

    ReddigramBot() throws Exception {
        timer = new Timer();
        config = BotConfig.configFromFile(new File("config.json"));
        dataFile = DataFile.load(this);
        client = new RedditClient(UserAgent.of("server", "xyz.mkotb.reddigram", "1.0", config.redditUsername()));
        telegramBot = TelegramBot.login(config.botApiKey());

        telegramBot.getEventsManager().register(new InlineListener(this));
        telegramBot.getEventsManager().register(new CommandListener(this));
        telegramBot.startUpdates(false);
        log("Successfully logged in");

        timer.scheduleAtFixedRate(new OAuthTask(this), 0L, 3500000L); // every 58 minutes reauth.
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (dataFile != null) {
                    dataFile.save();
                }
            }
        }, 0L, 600000L); // every 10m save data file
    }

    // gets an extract of 20 submissions from requested subreddit
    // paginates them into pages with 5 entries
    public List<List<Submission>> pagesFor(String subreddit, Sorting sorting) {
        SubredditPaginator paginator = new SubredditPaginator(client(), subreddit);

        if (sorting != null) {
            paginator.setSorting(sorting);
        }

        paginator.setLimit(20);
        Listing<Submission> listing;

        try {
            listing = paginator.next();
        } catch (NetworkException ex) {
            if (ex.getResponse().getStatusCode() != 404) {
                sendToOwner("Was unable to make a request to reddit due to a NetworkException: " + ex.getMessage());
            }

            return new ArrayList<>();
        }

        // list of pages with 5 entries
        List<List<Submission>> paginated = new ArrayList<>();

        for (int page = 0; page < PAGES; page++) {
            List<Submission> submissions = new ArrayList<>(5);

            for (int index = 0; index < 5; index++) {
                int i = (page * 5) + index;

                if (i >= listing.size()) {
                    break;
                }

                submissions.add(listing.get(i));
            }

            paginated.add(submissions);
        }

        return paginated;
    }

    // generates message for the page of submissions
    public String messageFor(List<Submission> submissions) {
        SendableTextMessage.SendableTextBuilder builder = SendableTextMessage.builder().textBuilder();

        for (int i = 0; i < submissions.size(); i++) {
            Submission submission = submissions.get(i);
            String subredditLink = "https://reddit.com/r/" + submission.getSubredditName();
            String userLink = "https://reddit.com/u/" + submission.getAuthor();

            builder.plain(NUMBER_EMOJIS[i]).space().link(submission.getTitle(), submission.getShortURL()).space().newLine()
                    .plain("[by ").link("/u/" + submission.getAuthor(), userLink).plain(" on ")
                    .link("/r/" + submission.getSubredditName(), subredditLink);

            if (submission.isNsfw()) {
                builder.bold(" (NSFW)");
            }

            builder.plain("]").newLine().newLine();
        }

        String message = builder.buildText().build().getMessage();
        return message.substring(0, message.length() - 2);
    }

    // register menu manually and bypass the start
    // method which sends out an update when called
    public void registerMenu(InlineMenu menu) {
        Extensions.get(telegramBot(), InlineMenuRegistry.class).register(menu);
    }

    public void log(String text) {
        System.out.println("[" + telegramBot.getBotUsername() + "] " + text);
    }

    public void sendToOwner(String text) {
        if (config.ownerUserId() != null) {
            telegramBot.getChat(config.ownerUserId()).sendMessage("[" + telegramBot.getBotUsername() + "] " + text);
        }
    }

    public DataFile dataFile() {
        return dataFile;
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
