package me.dinosparkour.voice;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.exceptions.PermissionException;

public class Queue {

	private static List<Song> playlist = new ArrayList<Song>();

	public static Song getCurrentSong() {
		return playlist.get(0);
	}

	public static Song getSongAt(int x) throws IndexOutOfBoundsException {
		return playlist.get(x);
	}

	public static Guild getCurrentGuild() {
		return playlist.get(0).getGuild();
	}

	public static VoiceChannel getCurrentVoice() {
		return playlist.get(0).getVoiceChannel();
	}

	public static TextChannel getCurrentText() {
		return playlist.get(0).getTextChannel();
	}

	public static void addSong(Song s) throws PermissionException {
		playlist.add(s);
		s.getTextChannel().sendMessageAsync("Added to the Queue! [Position: **" + Queue.getSize() + "**]", null);
	}

	public static void removeCurrentSong() {
		playlist.remove(0);
	}

	public static boolean isEmpty() {
		return playlist.isEmpty();
	}

	public static boolean isInLine(User dj) {
		return playlist.stream().anyMatch(s -> s.getDj().getId().equals(dj.getId()));
	}

	public static int getSize() {
		return playlist.size()-1;
	}

	public static void playNext() {
		if(Queue.getSize() <= 1) 
			return;

		Handle.skip();
	}

	public static void reset() {
		playlist.clear();
	}
}