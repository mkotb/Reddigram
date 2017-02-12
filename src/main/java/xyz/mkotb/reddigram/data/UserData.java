package xyz.mkotb.reddigram.data;

import net.dean.jraw.paginators.Sorting;

public class UserData {
    private Sorting preferredSorting;
    private boolean subscribedToBreaking = true;

    public Sorting preferredSorting() {
        return preferredSorting;
    }

    public void setPreferredSorting(Sorting preferredSorting) {
        this.preferredSorting = preferredSorting;
    }

    public boolean subscribedToBreaking() {
        return subscribedToBreaking;
    }

    public void setSubscribedToBreaking(boolean subscribedToBreaking) {
        this.subscribedToBreaking = subscribedToBreaking;
    }
}
