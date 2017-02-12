package xyz.mkotb.reddigram;

import com.sun.xml.internal.ws.util.StringUtils;
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
import pro.zackpollard.telegrambot.api.menu.*;
import xyz.mkotb.reddigram.cmd.LiveCommands;
import xyz.mkotb.reddigram.cmd.SubredditCommand;
import xyz.mkotb.reddigram.data.UserData;

import java.util.function.BiConsumer;

// NOTES:
//
// Throughout the menu part of this code, toggleButton is used as a dummy button
// which allows us to do whatever we want when the user clicks it. In the future,
// the API will have an open 'dummyButton' option for menus.
public class CommandListener implements Listener {
    private ReddigramBot bot;

    public CommandListener(ReddigramBot bot) {
        this.bot = bot;
    }

    @Override
    public void onCommandMessageReceived(CommandMessageReceivedEvent event) {
        UserData data = bot.dataFile().dataFor(event.getChat().getId());

        if (data == null) {
            data = new UserData();
            bot.dataFile().newData(event.getChat().getId(), data);
        }

        if (event.getCommand().equals("start")) {
            event.getChat().sendMessage("This bot acts as a Reddit utility bot!" +
                    "\n\n- You are able to view subreddits and the front page with /frontpage and /subreddit or using the inline feature @ReddigramBot" +
                    "\n\n- Or follow live threads with /follow and you will be shown breaking live threads, which will give you the latest updates on news and major events!" +
                    "\n\n- And more to be added, with features such as notifications and personalized front pages!" +
                    "\n\nMade by @MazenK. Contact him for support.");
        }

        if (event.getCommand().equals("git")) {
            event.getChat().sendMessage("This bot is open source! Check it out here:\nhttps://github.com/mkotb/Reddigram");
        }

        if (event.getCommand().equals("stop")) {
            bot.dataFile().removeData(event.getChat().getId());
            event.getChat().sendMessage("Removed user data!");
        }

        if (event.getCommand().equals("frontpage")) {
            sortingMenu(null, event.getChat(), false, (message, sorting) ->
                    SubredditCommand.sendSubreddit(bot, message, event.getChat(), "all", sorting));
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
                                        SubredditCommand.sendSubreddit(bot, message, event.getChat(), event.getArgs()[0], sorting));
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
                    SubredditCommand.sendSubreddit(bot, message, event.getChat(), event.getArgs()[0], sorting));
        }

        if (event.getCommand().equals("live")) {
            LiveCommands.live(bot, event.getChat());
        }

        if (event.getCommand().equals("follow")) {
            LiveCommands.follow(bot, event.getArgs(), event.getChat());
        }

        if (event.getCommand().equals("unfollow")) {
            LiveCommands.unfollow(bot, event.getChat());
        }

        if (event.getCommand().equals("subscribe")) {
            data.setSubscribedToBreaking(true);
            bot.dataFile().save();
            event.getChat().sendMessage("Successfully subscribed to breaking live threads!");
        }

        if (event.getCommand().equals("unsubscribe")) {
            data.setSubscribedToBreaking(false);
            bot.dataFile().save();
            event.getChat().sendMessage("Successfully unsubscribed from breaking live threads!");
        }
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
                           data.setPreferredSorting(sorting);

                           consumer.accept(message, sorting);
                           bot.dataFile().save();
                           return null;
                       })
                    .buildRow();
        }

        InlineMenu m = menu.buildMenu();

        if (msg != null) {
            bot.registerMenu(m);
            bot.telegramBot().editMessageText(msg, "Please select a category", ParseMode.NONE, false, m.toKeyboard());
        } else {
            chat.sendMessage(
                    SendableTextMessage.plain("Please select a category").replyMarkup(m.toKeyboard()).build()
            );
        }
    }
}
