package xyz.mkotb.reddigram.cmd;

import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import pro.zackpollard.telegrambot.api.chat.Chat;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.menu.InlineMenu;
import pro.zackpollard.telegrambot.api.menu.InlineMenuBuilder;
import pro.zackpollard.telegrambot.api.menu.InlineMenuRowBuilder;
import pro.zackpollard.telegrambot.api.menu.SubInlineMenuBuilder;
import xyz.mkotb.reddigram.ReddigramBot;

import java.util.ArrayList;
import java.util.List;

// NOTES:
//
// Throughout the menu part of this code, toggleButton is used as a dummy button
// which allows us to do whatever we want when the user clicks it. In the future,
// the API will have an open 'dummyButton' option for menus.
public class SubredditCommand {
    public static void sendSubreddit(ReddigramBot bot, Message message, Chat chat, String subreddit, Sorting sorting) {
        List<List<Submission>> paginated = bot.pagesFor(subreddit, sorting);
        // edit the message to contain the contents of the first page

        // create a dummy builder to encase all the pages
        InlineMenuBuilder dummyMenuBuilder = InlineMenu.builder(bot.telegramBot());

        dummyMenuBuilder.forWhom(chat);
        dummyMenuBuilder.message(message);

        List<InlineMenu> menus = new ArrayList<>(paginated.size());

        for (int page = 0; page < paginated.size(); page++) {
            /*
             * Create a menu with one row with one or two buttons
             * depending on if there is a page to go to forward or backward
             */
            SubInlineMenuBuilder menuBuilder = dummyMenuBuilder.subMenu();
            InlineMenuRowBuilder<SubInlineMenuBuilder> row = menuBuilder.newRow();

            if (page != 0) {
                int backMenu = page - 1;

                row.toggleButton("⬅️ Back (" + (page) + "/" + paginated.size() + ")")
                        .toggleCallback((button, value) -> {
                            // move to next menu
                            button.getMenu().unregister();
                            bot.registerMenu(menus.get(backMenu));

                            // edit text to match the page
                            bot.telegramBot().editMessageText(
                                    message,
                                    bot.messageFor(paginated.get(backMenu)),
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

                row.toggleButton("➡️ Next (" + (page + 2) + "/" + paginated.size() + ")")
                        .toggleCallback((button, value) -> {
                            // move to next menu
                            button.getMenu().unregister();
                            bot.registerMenu(menus.get(nextMenu));

                            // edit text and menu to match the page
                            bot.telegramBot().editMessageText(
                                    message,
                                    bot.messageFor(paginated.get(nextMenu)),
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
        bot.registerMenu(menus.get(0));
        bot.telegramBot().editMessageText(
                message,
                bot.messageFor(paginated.get(0)),
                ParseMode.HTML,
                true,
                menus.get(0).toKeyboard()
        );
        bot.dataFile().statistics().incrementRequests();
    }
}
