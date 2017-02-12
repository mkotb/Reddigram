package xyz.mkotb.reddigram.cmd;

import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import pro.zackpollard.telegrambot.api.chat.Chat;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.menu.InlineMenu;
import pro.zackpollard.telegrambot.api.menu.InlineMenuBuilder;
import xyz.mkotb.reddigram.ReddigramBot;

import java.util.List;

public class LiveCommand {
    public static void followLive(ReddigramBot bot, Chat chat) {
        List<List<Submission>> submissions = bot.pagesFor("live", Sorting.HOT);
        Message message = chat.sendMessage(SendableTextMessage.plain("Select a live thread to follow:").build());
        InlineMenuBuilder builder = InlineMenu.builder(bot.telegramBot(), chat)
                .message(message);

        submissions.get(0).forEach((submission) ->
                        builder.newRow().toggleButton(submission.getTitle())
                                .toggleCallback((button, newValue) -> {
                                    // Follow thread and inform the user of that
                                    String threadId = bot.liveManager().idFromSubmission(submission);

                                    bot.liveManager().subscribeTo(threadId, chat.getId());
                                    bot.telegramBot().editMessageText(
                                            message,
                                            "*Following " + threadId + "...*",
                                            ParseMode.MARKDOWN,
                                            false, null
                                    );
                                    return null;
                                })
                                .build().build()
        );

        InlineMenu menu = builder.buildMenu();

        bot.registerMenu(menu);
        menu.apply();
    }
}
