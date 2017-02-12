package xyz.mkotb.reddigram.cmd;

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
import pro.zackpollard.telegrambot.api.conversations.prompt.TextValidatingPrompt;
import pro.zackpollard.telegrambot.api.menu.InlineMenu;
import pro.zackpollard.telegrambot.api.menu.InlineMenuBuilder;
import xyz.mkotb.reddigram.ReddigramBot;
import xyz.mkotb.reddigram.live.LiveFollower;

import java.util.List;
import java.util.function.Consumer;

public class LiveCommands {
    public static void live(ReddigramBot bot, Chat chat) {
        List<List<Submission>> submissions = bot.pagesFor("live", Sorting.HOT);
        Message message = chat.sendMessage(SendableTextMessage.plain("Select a live thread to follow:").build());
        InlineMenuBuilder builder = InlineMenu.builder(bot.telegramBot(), chat)
                .message(message);

        submissions.get(0).stream()
                .filter((submission) -> !submission.isStickied())
                .forEach((submission) -> {
                    String threadId = bot.liveManager().idFromSubmission(submission);

                    if (threadId == null) {
                        return;
                    }

                    builder.newRow().toggleButton(submission.getTitle())
                            .toggleCallback((button, newValue) -> {
                                // Follow thread and inform the user of that
                                bot.liveManager().subscribeTo(threadId, chat.getId());
                                bot.telegramBot().editMessageText(
                                        message,
                                        "*Following " + threadId + "...*",
                                        ParseMode.MARKDOWN,
                                        false, null
                                );
                                return null;
                            })
                            .build().build();
                });

        InlineMenu menu = builder.buildMenu();

        bot.registerMenu(menu);
        menu.apply();
    }

    public static void follow(ReddigramBot bot, String[] args, Chat chat) {
        if (args.length == 0) {
            // prompt them for a thread id to follow
            Message message = chat.sendMessage(SendableTextMessage.plain("Please send the live thread id you wish to follow (like yeitdh0583mc)\n" +
                    "You can also send a link like https://reddit.com/live/yeitdh0583mc")
                    .disableWebPagePreview(true).build());

            Conversation.builder(bot.telegramBot()).forWhom(chat)
                    .silent(true)
                    .prompts()
                    .first(new ThreadIdPrompt(bot, (id) -> {
                        bot.liveManager().subscribeTo(id, chat.getId());
                        bot.telegramBot().editMessageText(
                                message,
                                "*Following " + id + "...*",
                                ParseMode.MARKDOWN,
                                false, null
                        );
                    })).end()
                    .build()
                    .begin();
            return;
        }

        String threadId = bot.liveManager().idFromInput(args[0]);

        if (threadId == null) {
            chat.sendMessage(SendableTextMessage.plain("Please send a valid live thread id or link (like yeitdh0583mc or https://reddit.com/live/yeitdh0583mc)")
                    .disableWebPagePreview(true).build());
            return;
        }

        bot.liveManager().subscribeTo(threadId, chat.getId());
        chat.sendMessage(SendableTextMessage.markdown("*Following " + threadId + "...*").build());
    }

    public static void unfollow(ReddigramBot bot, Chat chat) {
        LiveFollower follower = bot.liveManager().threadBy(chat.getId());

        if (follower == null) {
            chat.sendMessage("You are not following any threads!");
            return;
        }

        bot.liveManager().unsubscribe(follower.threadId(), chat.getId());
        chat.sendMessage("Unfollowing " + follower.threadId() + "!");
    }

    public static class ThreadIdPrompt extends TextValidatingPrompt {
        private final ReddigramBot bot;
        private final Consumer<String> threadIdConsumer;

        public ThreadIdPrompt(ReddigramBot bot, Consumer<String> threadIdConsumer) {
            this.bot = bot;
            this.threadIdConsumer = threadIdConsumer;
        }

        @Override
        protected boolean validate(ConversationContext context, TextContent input) {
            return bot.liveManager().idFromInput(input.getContent()) != null;
        }

        @Override
        protected boolean accept(ConversationContext context, TextContent input) {
            threadIdConsumer.accept(bot.liveManager().idFromInput(input.getContent()));
            return false;
        }

        @Override
        public SendableMessage promptMessage(ConversationContext context) {
            return null;
        }

        @Override
        protected SendableMessage invalidationMessage(ConversationContext context, TextContent input) {
            return SendableTextMessage.plain("Please send a valid thread id or link to a reddit live thread!").build();
        }
    }
}
