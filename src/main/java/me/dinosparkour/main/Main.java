package me.dinosparkour.main;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.JDABuilder;

import me.dinosparkour.voice.Music;

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