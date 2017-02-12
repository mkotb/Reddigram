package xyz.mkotb.reddigram.live;

import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.mkotb.reddigram.ReddigramBot;

import java.util.List;
import java.util.TimerTask;

public class ScanLiveThreadsTask extends TimerTask {
    private final ReddigramBot bot;

    public ScanLiveThreadsTask(ReddigramBot bot) {
        this.bot = bot;
    }

    @Override
    public void run() {
        List<List<Submission>> pages = bot.pagesFor("live", Sorting.HOT);

        pages.get(0).forEach((submission) -> {
            if (submission.getScore() < 70 || submission.isStickied()) {
                return;
            }

            if ((System.currentTimeMillis() - submission.getCreated().getTime()) >= 3600000) {
                return;
            }

            String threadId = bot.liveManager().idFromSubmission(submission);

            if (threadId == null) {
                return;
            }

            // BREAKING
            bot.dataFile().userData().stream()
                    .filter((entry) -> entry.getValue().subscribedToBreaking())
                    .forEach((entry) -> {
                        bot.liveManager().subscribeTo(threadId, entry.getKey());
                        bot.telegramBot().getChat(entry.getKey()).sendMessage(SendableTextMessage.builder().textBuilder()
                                .bold("▶️ BREAKING: Following urgent feed: ").link(submission.getTitle(), submission.getUrl()).bold("!").newLine().newLine()
                                .italics("You can unfollow this thread by sending /unfollow").newLine()
                                .italics("You can unsubscribe to urgent feeds by sending /unsubscribe")
                                .buildText().build());

                        try {
                            Thread.sleep(50L);
                        } catch (InterruptedException ex) {
                        }
                    });
        });
    }
}
