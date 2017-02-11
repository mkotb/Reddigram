package xyz.mkotb.reddigram.data;

import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {
    private AtomicInteger requestsServed = new AtomicInteger(0);
    private int usersServed = 0;

    public void incrementRequests() {
        requestsServed.getAndIncrement();
    }

    public int usersServed() {
        return usersServed;
    }

    public int requestsServed() {
        return requestsServed.get();
    }

    public void setUsersServed(int usersServed) {
        this.usersServed = usersServed;
    }
}
