package xyz.mkotb.reddigram.live;

import org.json.JSONObject;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;

import java.util.UUID;

public class LiveUpdate {
    private final String threadId;
    private final String author;
    private final String body;
    private final UUID id;
    private final long created;

    public LiveUpdate(String threadId, JSONObject entry) {
        this.threadId = threadId;
        this.author = entry.getString("author");
        this.body = entry.getString("body");
        this.id = UUID.fromString(entry.getString("id"));
        this.created = entry.getLong("created_utc");
    }

    public String author() {
        return author;
    }

    public String body() {
        return body;
    }

    public UUID id() {
        return id;
    }

    public long created() {
        return created;
    }

    public SendableTextMessage toMessage() {
        return SendableTextMessage.builder().textBuilder()
                .bold("\uD83D\uDDE3 Update on ").bold(threadId).bold(" from ").bold(author)
                .newLine().newLine()
                .plain(body)
                .buildText().build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LiveUpdate that = (LiveUpdate) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
