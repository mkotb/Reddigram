package xyz.mkotb.reddigram;

import com.sun.xml.internal.ws.util.StringUtils;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import pro.zackpollard.telegrambot.api.chat.Chat;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.content.TextContent;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.conversations.Conversation;
import pro.zackpollard.telegrambot.api.conversations.ConversationContext;
import pro.zackpollard.telegrambot.api.conversations.prompt.TextPrompt;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.message.CommandMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.extensions.Extensions;
import pro.zackpollard.telegrambot.api.menu.*;

import java.util.ArrayList;
import java.util.List;

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
            sortingMenu(null, event.getChat(), "all");
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
                                sortingMenu(message, event.getChat(), input.getContent());
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

            sortingMenu(null, event.getChat(), event.getArgs()[0]);
        }
    }

    /*
     * Presents to the user a menu to select a category.
     *
     * TODO: Learn the preference and use that unless they want to change it.
     */
    public void sortingMenu(Message msg, Chat chat, String subreddit) {
        if (msg == null) {
            msg = chat.sendMessage("Please select a category:");
        } else {
            bot.telegramBot().editMessageText(msg, "Please select a category", ParseMode.NONE, false, null);
        }

        Message message = msg;
        InlineMenuBuilder menu = InlineMenu.builder(bot.telegramBot())
                .forWhom(chat)
                .message(message);

        for (Sorting sorting : Sorting.values()) {
            menu.newRow()
                    .toggleButton(StringUtils.capitalize(sorting.name().toLowerCase()))
                       .toggleCallback((button, value) -> {
                           button.getMenu().unregister();
                           sendSubreddit(message, chat, subreddit, sorting);
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
