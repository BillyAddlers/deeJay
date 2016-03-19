package me.dinosparkour.main;

import me.dinosparkour.voice.Music;
import me.dinosparkour.voice.Queue;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.events.ReconnectedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

public class Main extends ListenerAdapter {

    public static void main(String[] args) throws LoginException, IllegalArgumentException, InterruptedException {

        new JDABuilder()
                .setEmail(BotInfo.getEmail())
                .setPassword(BotInfo.getPassword())
                .addListener(new EvalCommand())
                .addListener(new Music())
                .addListener(new Main())
                .buildAsync();
    }

    @Override
    public void onReconnect(ReconnectedEvent e) {
        Queue.reset();
    }
}