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
import pro.zackpollard.telegrambot.api.menu.*;
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
            bot.registerMenu(m);
            bot.telegramBot().editMessageText(msg, "Please select a category", ParseMode.NONE, false, m.toKeyboard());
        } else {
            chat.sendMessage(
                    SendableTextMessage.plain("Please select a category").replyMarkup(m.toKeyboard()).build()
            );
        }
    }
}
