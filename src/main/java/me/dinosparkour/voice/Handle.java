package me.dinosparkour.voice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.UnsupportedAudioFileException;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.managers.AudioManager;
import net.dv8tion.jda.player.MyPlayer;

import me.dinosparkour.utilities.Tasks;

public class Handle {

	private static float volume = 0.25f;
	private static int skipVotes;
	private static List<String> ignoredUsers = new ArrayList<String>();
	private static MyPlayer player;

	public static void play(JDA jda) {

		AudioManager am = jda.getAudioManager();
		Song s = Queue.getCurrentSong();
		TextChannel chan = Queue.getCurrentText();
		VoiceChannel vChan = Queue.getCurrentVoice();
		User dj = s.getDj();

		try {
			player = new MyPlayer(s.getUrl()) {

				@Override
				public void stop() {
					super.stop();
					am.closeAudioConnection();
					Queue.removeCurrentSong();
					Handle.resetVotes();

					if(!Queue.isEmpty()) {
						try {
							TimeUnit.MILLISECONDS.sleep(200);

						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}

						Handle.play(jda);
					}
				}
			};

		} catch (UnsupportedAudioFileException ex) {
			if(Tasks.canIType(jda, s.getTextChannel()))
				sendMessage(s.getDj().getAsMention() + ": Could not play audio file! (is it too big?)", chan);
			return;
		}

		am.setSendingHandler(player);
		player.play();
		am.openAudioConnection(vChan);
		if(Handle.getVolume() < 0.05f || Handle.getVolume() > 0.45f)
			Handle.setVolume(0.25f);
		else
			Handle.setVolume(volume);

		StringBuilder ex = new StringBuilder("Now playing in #" + vChan.getName() + " "
				+ "a song requested by **" + dj.getUsername() + "**!");

		if(Queue.getSize() >= 1) {
			ex.append("\n(");

			if(Queue.getSize() == 1)
				ex.append("1 more song");
			else
				ex.append(Queue.getSize() + " more songs");

			ex.append(" queued up)");
		}
		sendMessage(ex.toString(), chan);
	}

	public static float getVolume() {
		return Handle.volume;
	}

	public static void setVolume(Float volume) {
		player.setVolume(volume);
		Handle.volume = volume;
	}

	public static int getVotes() {
		return skipVotes;
	}

	public static void addVote(User u) {
		ignoredUsers.add(u.getId());
		skipVotes++;
	}

	public static void skip() {
		if(Handle.isPlayerActive())
			player.stop();
		else
			Queue.removeCurrentSong();
	}

	public static boolean isUserIgnored(User u) {		
		boolean ignoreCheck = Handle.ignoredUsers.contains(u.getId());
		return ignoreCheck;
	}

	public static boolean isPlayerActive() {
		if(player == null)
			return false;
		else
			return player.isStarted();
	}

	public static boolean isPlayerInGuild(Guild guild) {
		if(!Handle.isPlayerActive() || Queue.isEmpty())
			return false;

		if(Queue.getCurrentGuild() == guild)
			return true;

		return false;
	}

	public static void resetVotes() {
		ignoredUsers.clear();
		skipVotes = 0;
	}

	private static void sendMessage(String msg, TextChannel channel) {
		Tasks.sendMessage(msg, channel);
	}
}