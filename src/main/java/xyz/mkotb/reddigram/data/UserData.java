package xyz.mkotb.reddigram.data;

import net.dean.jraw.paginators.Sorting;

public class UserData {
    private Sorting preferredSorting;

    public Sorting preferredSorting() {
        return preferredSorting;
    }

    public void setPreferredSorting(Sorting preferredSorting) {
        this.preferredSorting = preferredSorting;
    }
}
