package xyz.mkotb.reddigram;

import com.sun.xml.internal.ws.util.StringUtils;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import pro.zackpollard.telegrambot.api.chat.Chat;
import pro.zackpollard.telegrambot.api.chat.inline.send.InlineQueryResponse;
import pro.zackpollard.telegrambot.api.chat.inline.send.content.InputTextMessageContent;
import pro.zackpollard.telegrambot.api.chat.inline.send.results.InlineQueryResult;
import pro.zackpollard.telegrambot.api.chat.inline.send.results.InlineQueryResultArticle;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.content.TextContent;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.conversations.Conversation;
import pro.zackpollard.telegrambot.api.conversations.ConversationContext;
import pro.zackpollard.telegrambot.api.conversations.prompt.TextPrompt;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.inline.InlineQueryReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.CommandMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.extensions.Extensions;
import pro.zackpollard.telegrambot.api.menu.*;
import xyz.mkotb.reddigram.data.UserData;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

// NOTES:
//
// Throughout the menu part of this code, toggleButton is used as a dummy button
// which allows us to do whatever we want when the user clicks it. In the future,
// the API will have an open 'dummyButton' option for menus.
public class TelegramListener implements Listener {
    public static final int PAGES = 4;
    public static final String[] NUMBER_EMOJIS = new String[] {"1⃣", "2⃣", "3⃣", "4⃣", "5⃣", "6⃣", "7⃣", "8⃣", "9⃣"};
    private ReddigramBot bot;

    public TelegramListener(ReddigramBot bot) {
        this.bot = bot;
    }

    @Override
    public void onCommandMessageReceived(CommandMessageReceivedEvent event) {
        if (event.getCommand().equals("frontpage")) {
            sortingMenu(null, event.getChat(), false, (message, sorting) ->
                    sendSubreddit(message, event.getChat(), "all", sorting));
        }

        if (event.getCommand().equals("goto") || event.getCommand().equals("subreddit")) {
            if (event.getArgs().length == 0) {
                // if they do not provide a subreddit, prompt them to send the
                // subreddit they wish to view. This feature exists for a greater
                // mobile experience as a tap on a command sends it through without
                // allowing them to provide arguments
                Message message = event.getChat().sendMessage("What is the subreddit you wish to view?");

                Conversation.builder(bot.telegramBot()).forWhom(event.getChat())
                        .silent(true)
                        .prompts()
                        .first(new TextPrompt() {
                            @Override
                            public boolean process(ConversationContext context, TextContent input) {
                                sortingMenu(message, event.getChat(), false, (message, sorting) ->
                                        sendSubreddit(message, event.getChat(), event.getArgs()[0], sorting));
                                return false;
                            }

                            @Override
                            public SendableMessage promptMessage(ConversationContext context) {
                                return null;
                            }
                        }).end()
                        .build()
                        .begin();
                return;
            }

            sortingMenu(null, event.getChat(), false, (message, sorting) ->
                sendSubreddit(message, event.getChat(), event.getArgs()[0], sorting));
        }
    }

    @Override
    public void onInlineQueryReceived(InlineQueryReceivedEvent event) {
        String subreddit = event.getQuery().getQuery();

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
            SendableTextMessage.SendableTextBuilder builder = SendableTextMessage.builder().textBuilder()
                    .link(submission.getTitle(), submission.getShortURL())
                    .plain(" on Reddit").newLine();
            String subredditLink = "https://reddit.com/r/" + submission.getSubredditName();
            String userLink = "https://reddit.com/u/" + submission.getAuthor();
            URL url = null;

            builder.plain("[by ").link("/u/" + submission.getAuthor(), userLink).plain(" on ")
                    .link("/r/" + submission.getSubredditName(), subredditLink);

            if (submission.isNsfw()) {
                builder.bold(" (NSFW)");
            }

            builder.plain("]");

            String messageText = builder.buildText().build().getMessage();

            try {
                url = new URL(submission.getShortURL());
            } catch (MalformedURLException ignored) {
            }

            results.add(InlineQueryResultArticle.builder()
                    .id(submission.getId())
                    .title(submission.getTitle())
                    .description("By /u/" + submission.getAuthor() + " on /r/" + submission.getSubredditName())
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

    /*
     * Presents to the user a menu to select a category.
     */
    public void sortingMenu(Message msg, Chat chat,
                            boolean force, BiConsumer<Message, Sorting> consumer) {
        if (msg == null) {
            msg = chat.sendMessage("Please select a category:");
        } else {
            bot.telegramBot().editMessageText(msg, "Please select a category", ParseMode.NONE, false, null);
        }

        UserData data = bot.dataFile().dataFor(chat.getId());

        if (!force && data != null) {
            if (data.preferredSorting() != null) {
                consumer.accept(msg, data.preferredSorting());
                return;
            }
        } else if (data == null) {
            data = new UserData();
            bot.dataFile().newData(chat.getId(), data);
        }

        UserData userData = data; // effectively final
        Message message = msg;
        InlineMenuBuilder menu = InlineMenu.builder(bot.telegramBot())
                .forWhom(chat)
                .message(message);

        for (Sorting sorting : Sorting.values()) {
            menu.newRow()
                    .toggleButton(StringUtils.capitalize(sorting.name().toLowerCase()))
                       .toggleCallback((button, value) -> {
                           button.getMenu().unregister();
                           userData.setPreferredSorting(sorting);

                           consumer.accept(message, sorting);
                           bot.dataFile().save();
                           return null;
                       })
                    .buildRow();
        }

        InlineMenu m = menu.buildMenu();

        if (msg != null) {
            registerMenu(m);
            bot.telegramBot().editMessageText(msg, "Please select a category", ParseMode.NONE, false, m.toKeyboard());
        } else {
            chat.sendMessage(
                    SendableTextMessage.plain("Please select a category").replyMarkup(m.toKeyboard()).build()
            );
        }
    }

    public void sendSubreddit(Message message, Chat chat, String subreddit, Sorting sorting) {
        List<List<Submission>> paginated = bot.pagesFor(subreddit, sorting);
        // edit the message to contain the contents of the first page

        // create a dummy builder to encase all the pages
        InlineMenuBuilder dummyMenuBuilder = InlineMenu.builder(bot.telegramBot());

        dummyMenuBuilder.forWhom(chat);
        dummyMenuBuilder.message(message);

        List<InlineMenu> menus = new ArrayList<>(PAGES);

        for (int page = 0; page < PAGES; page++) {
            /*
             * Create a menu with one row with one or two buttons
             * depending on if there is a page to go to forward or backward
             */
            SubInlineMenuBuilder menuBuilder = dummyMenuBuilder.subMenu();
            InlineMenuRowBuilder<SubInlineMenuBuilder> row = menuBuilder.newRow();

            if (page != 0) {
                int backMenu = page - 1;

                row.toggleButton("⬅️ Back (" + (page) + "/" + PAGES + ")")
                        .toggleCallback((button, value) -> {
                            // move to next menu
                            button.getMenu().unregister();
                            registerMenu(menus.get(backMenu));

                            // edit text to match the page
                            bot.telegramBot().editMessageText(
                                    message,
                                    messageFor(paginated.get(backMenu)),
                                    ParseMode.HTML,
                                    true,
                                    menus.get(backMenu).toKeyboard()
                            );
                            return null;
                        })
                        .build();
            }

            if (page != 3) {
                int nextMenu = page + 1;

                row.toggleButton("➡️ Next (" + (page + 2) + "/" + PAGES + ")")
                        .toggleCallback((button, value) -> {
                            // move to next menu
                            button.getMenu().unregister();
                            registerMenu(menus.get(nextMenu));

                            // edit text and menu to match the page
                            bot.telegramBot().editMessageText(
                                    message,
                                    messageFor(paginated.get(nextMenu)),
                                    ParseMode.HTML,
                                    true,
                                    menus.get(nextMenu).toKeyboard()
                            );
                            return null;
                        })
                        .build();
            }

            menus.add(row.build().buildMenu());
        }

        // start the menu encasing the menus and unregistering it
        dummyMenuBuilder.buildMenu().unregister();
        // start the first page's menu
        registerMenu(menus.get(0));
        bot.telegramBot().editMessageText(
                message,
                messageFor(paginated.get(0)),
                ParseMode.HTML,
                true,
                menus.get(0).toKeyboard()
        );
        bot.dataFile().statistics().incrementRequests();
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
    private void registerMenu(InlineMenu menu) {
        Extensions.get(bot.telegramBot(), InlineMenuRegistry.class).register(menu);
    }
}
