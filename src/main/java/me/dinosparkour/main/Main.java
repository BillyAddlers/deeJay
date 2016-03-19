package me.dinosparkour.main;

import me.dinosparkour.voice.Music;
import net.dv8tion.jda.JDABuilder;

import javax.security.auth.login.LoginException;

public class Main {

	public static void main(String[] args) throws LoginException, IllegalArgumentException, InterruptedException {

		new JDABuilder()
				.setEmail(BotInfo.getEmail())
				.setPassword(BotInfo.getPassword())
				.addListener(new EvalCommand())
				.addListener(new Music())
				.buildAsync();
	}
}