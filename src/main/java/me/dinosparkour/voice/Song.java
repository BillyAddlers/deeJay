package me.dinosparkour.voice;

import net.dv8tion.jda.entities.*;

public class Song {

	private User user;
	private String url;
	private Guild guild;
	private TextChannel textChannel;
	private VoiceChannel voiceChannel;

	public Song(User author, String input, Guild guild, TextChannel channel, VoiceChannel vChan) {
		this.user = author;
		this.url = input;
		this.guild = guild;
		this.textChannel = channel;
		this.voiceChannel = vChan;
	}

	public User getDj() {
		return user;
	}

	public String getUrl() {
		return url;
	}

	public Guild getGuild() {
		return guild;
	}

	public TextChannel getTextChannel() {
		return textChannel;
	}

	public VoiceChannel getVoiceChannel() {
		return voiceChannel;
	}
}