package xyz.mkotb.reddigram;

import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;

import java.util.TimerTask;

public class OAuthTask extends TimerTask {
    private final ReddigramBot reddigramBot;

    public OAuthTask(ReddigramBot reddigramBot) {
        this.reddigramBot = reddigramBot;
    }

    @Override
    public void run() {
        Credentials creds = Credentials.userless(
                reddigramBot.config().clientId(),
                reddigramBot.config().clientSecret(),
                reddigramBot.deviceId()
        );
        OAuthData data;

        try {
            data = reddigramBot.client().getOAuthHelper().easyAuth(creds);
        } catch (OAuthException ex) {
            // Oh no! Complain to owner and shut down
            reddigramBot.log("Couldn't authenticate the bot! Shutting down...");
            ex.printStackTrace();
            reddigramBot.sendToOwner("Couldn't complete OAuth with Reddit (message=" + ex.getMessage() + ")");
            return;
        }

        reddigramBot.client().authenticate(data);
    }
}
