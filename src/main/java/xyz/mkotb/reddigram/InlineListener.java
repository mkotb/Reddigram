package xyz.mkotb.reddigram;

import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import pro.zackpollard.telegrambot.api.chat.inline.send.InlineQueryResponse;
import pro.zackpollard.telegrambot.api.chat.inline.send.content.InputTextMessageContent;
import pro.zackpollard.telegrambot.api.chat.inline.send.results.InlineQueryResult;
import pro.zackpollard.telegrambot.api.chat.inline.send.results.InlineQueryResultArticle;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.inline.InlineQueryReceivedEvent;
import xyz.mkotb.reddigram.data.UserData;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class InlineListener implements Listener {
    private final ReddigramBot bot;

    public InlineListener(ReddigramBot bot) {
        this.bot = bot;
    }

    @Override
    public void onInlineQueryReceived(InlineQueryReceivedEvent event) {
        String subreddit = event.getQuery().getQuery();

        // default to frontpage
        if (subreddit.trim().isEmpty()) {
            subreddit = "all";
        }

        Sorting sorting;
        UserData data = bot.dataFile().dataFor(String.valueOf(event.getQuery().getSender().getId()));

        if (data == null || data.preferredSorting() == null) {
            sorting = Sorting.HOT;
        } else {
            sorting = data.preferredSorting();
        }

        List<List<Submission>> pages = bot.pagesFor(subreddit, sorting);
        List<InlineQueryResult> results = new ArrayList<>(pages.size());

        pages.forEach((page) -> page.forEach((submission) -> {
            SendableTextMessage.SendableTextBuilder messageBuilder = SendableTextMessage.builder().textBuilder()
                    .link(submission.getTitle(), submission.getShortURL())
                    .plain(" on Reddit").newLine();
            String subredditLink = "https://reddit.com/r/" + submission.getSubredditName();
            String userLink = "https://reddit.com/u/" + submission.getAuthor();
            String title = submission.getTitle();
            String description = "By /u/" + submission.getAuthor() + " on /r/" + submission.getSubredditName();
            URL url = null;

            messageBuilder.plain("[by ").link("/u/" + submission.getAuthor(), userLink).plain(" on ")
                    .link("/r/" + submission.getSubredditName(), subredditLink);

            if (title.length() > 43 || (submission.isNsfw() && title.length() > 36)) {
                if (submission.isNsfw()) {
                    title = title.substring(0, 33) + "...";
                } else {
                    title = title.substring(0, 40) + "...";
                }
            }

            if (description.length() > 41) {
                description = description.substring(0, 38) + "...";
            }

            if (submission.isNsfw()) {
                messageBuilder.bold(" (NSFW)");
                title += " (NSFW)";
            }

            messageBuilder.plain("]");

            String messageText = messageBuilder.buildText().build().getMessage();

            try {
                url = new URL(submission.getShortURL());
            } catch (MalformedURLException ignored) {
            }

            results.add(InlineQueryResultArticle.builder()
                            .id(submission.getId())
                            .title(title)
                            .description(description)
                            .inputMessageContent(InputTextMessageContent.builder()
                                    .messageText(messageText)
                                    .parseMode(ParseMode.HTML)
                                    .disableWebPagePreview(false)
                                    .build())
                            .url(url)
                            .build()
            );
        }));

        event.getQuery().answer(bot.telegramBot(), InlineQueryResponse.builder()
                        .results(results)
                        .cacheTime(600) // stay in cache for 600s
                        .isPersonal(false) // although sorting is not personal sometimes, this will save processing time
                        .nextOffset("").build()
        );
        bot.dataFile().statistics().incrementRequests();
    }
}
