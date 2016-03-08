package me.dinosparkour.voice;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.math.NumberUtils;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.MessageBuilder.Formatting;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

import me.dinosparkour.main.BotInfo;
import me.dinosparkour.utilities.Tasks;

public class Music extends ListenerAdapter {

	private static boolean multiQueue = false;

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent e) {

		JDA jda = e.getJDA();
		Guild guild = e.getGuild();
		TextChannel channel = e.getChannel();
		VoiceChannel vChannel = guild.getVoiceStatusOfUser(e.getAuthor()).getChannel();
		String message = e.getMessage().getContent();
		User author = e.getMessage().getAuthor();
		String prefix = BotInfo.getPrefix();

		String cmd;
		String input;

		if(author.getUsername().equals(jda.getSelfInfo().getUsername())
				|| message.isEmpty()
				|| !message.startsWith(prefix))
			return;

		if(message.contains(" ")) {
			cmd = message.substring(prefix.length(), message.indexOf(" ")).toLowerCase();
			input = message.replace(prefix + cmd + " ", "");
		} else {
			cmd = message.substring(prefix.length()).toLowerCase();
			input = null;
		}

		switch(cmd) {
		case "play":
			VoiceChannel voice = guild.getVoiceStatusOfUser(author).getChannel();
			if(voice == null) {
				sendMessage("**You are not in a channel!**", channel);
				return;
			}

			if(input == null || input.contains(" ")) {
				sendMessage("Correct usage `" + prefix + "play <URL>`", channel);
				return;
			}

			if(Queue.isInLine(author) && !multiQueue) {
				sendMessage("You already have a song queued!", channel);
				return;
			}

			try {
				new URL(input);

			} catch (MalformedURLException ex) {
				sendMessage("That's not a valid URL!", channel);
				return;
			}

			Song song = new Song(author, input, guild, channel, vChannel);
			boolean autoPlay = false;
			if(Queue.isEmpty())
				autoPlay = true;
			Queue.addSong(song);
			if(autoPlay)
				Handle.play(jda);

			break;

		case "skip":
			if(!Handle.isPlayerInGuild(guild))
				sendMessage("The bot isn't playing music in this server!", channel);

			else {
				if(Handle.isUserIgnored(author)) {
					sendMessage("You have already voted to skip this song!", channel);
					return;
				}

				if(author.getId().equals(Queue.getCurrentSong().getDj().getId())
						|| Handle.getVotes() == 5) {
					sendMessage("Skipping current song!", channel);
					Handle.skip();
				} else {
					Handle.addVote(author);
					sendMessage("Amount of votes: **" + Handle.getVotes() + "** out of **5 needed**", channel);
				}
			}
			break;

		case "forceskip":
			if(!Handle.isPlayerInGuild(guild))
				sendMessage("The bot isn't playing music in this server!", channel);
			else
				if(Tasks.isDj(author, guild, channel)) {
					sendMessage("Skipping current song!", channel);
					Handle.skip();
				}
			break;

		case "volume":
			if(!Handle.isPlayerInGuild(guild)) {
				sendMessage("The bot isn't playing music in this server!", channel);
				return;
			}

			if(input == null) {
				sendMessage("Current volume: **" + Handle.getVolume() + "**", channel);
				return;
			}

			if(Queue.getCurrentSong().getDj() != author && !Tasks.isDj(author, guild, channel)) {
				sendMessage("You are not the current song's DJ!", channel);
				return;
			}

			Float volume;
			if(NumberUtils.isNumber(input))
				volume = Float.parseFloat(input);
			else {
				sendMessage("That's not a valid volume level!", channel);
				return;
			}

			if(volume > 2f || volume < 0f) {
				sendMessage("Please enter a value between 0 and 2.0", channel);
				return;
			}

			Handle.setVolume(volume);
			sendMessage("Set the volume to **" + volume + "**", channel);
			break;

		case "queue":
			int size = Queue.getSize();
			if(Queue.isEmpty() || size == 0)
				sendMessage("The queue is empty!", channel);
			else {
				MessageBuilder msg = new MessageBuilder().appendString("Queue size: **" + size + " songs** from the following DJs:\n");
				for(int q = 0; q < size;) {
					q++;
					Song s = Queue.getSongAt(q);
					msg.appendString("\n**[" + q + "]** ")
					.appendMention(s.getDj()).appendString(" ")
					.appendString("in " + s.getGuild().getName(), Formatting.ITALICS);
				}
				sendMessage(msg.build(), channel);
			}
			break;

		case "music":
			sendMessage("__Currently available music commands__: \n"
					+ "\n**`" + prefix + "play [link]`**: *Currently only supports direct MP3s and some YT videos*\n"
					+ "\n**`" + prefix + "skip`**: *Initiates a vote to skip current song*\n"
					+ "\n**`" + prefix + "forceskip`**: *Immediately skips the current song* - Requires `DJ Rank`\n"
					+ "\n**`" + prefix + "volume`**: *Checks the player's volume*\n"
					+ "\n**`" + prefix + "volume (level)`**: *Sets the player's volume if you're the song's DJ*\n"
					+ "\n**`" + prefix + "queue`**: *Checks the queue's size and DJs*\n", channel);
			break;

		case "multiqueue":
			if(!author.getId().equals(BotInfo.getAuthorId()))
				return;

			multiQueue = !multiQueue;
			sendMessage("Now set to **" + multiQueue + "** :)", channel);
			break;
		}
	}

	private void sendMessage(String msg, TextChannel channel) {
		Tasks.sendMessage(msg, channel);
	}

	private void sendMessage(Message msg, TextChannel channel) {
		sendMessage(msg.getRawContent(), channel);
	}
}