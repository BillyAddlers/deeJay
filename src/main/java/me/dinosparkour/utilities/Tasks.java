package me.dinosparkour.utilities;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.*;

import me.dinosparkour.main.BotInfo;

public class Tasks {

	public static boolean canIType(JDA jda, MessageChannel channel) {
		boolean hasPerm = true;
		if(channel instanceof TextChannel)
			hasPerm = ((TextChannel) channel).checkPermission(jda.getSelfInfo(), Permission.MESSAGE_WRITE);
		return hasPerm;
	}

	public static void sendMessage(String msg, TextChannel channel) {
		if(Tasks.canIType(channel.getJDA(), channel)) {
			if(msg.contains("@everyone"))
				msg = msg.replace("@everyone", "@\u180Eeveryone");
			channel.sendMessageAsync(msg, null);
		} //else
		//	Tasks.permissionError(e, Permission.MESSAGE_WRITE);
	}

	/*
	private static void permissionError(GuildMessageReceivedEvent e, Permission perm) {
		MessageChannel targetChannel;
		if(perm.equals(Permission.MESSAGE_WRITE))
			targetChannel = e.getAuthor().getPrivateChannel();
		else
			targetChannel = e.getChannel();

		Message error = new MessageBuilder().appendString("The bot does not have the permission ")
				.appendString(perm.toString(), Formatting.BLOCK)
				.appendString(" to execute the command ")
				.appendString(e.getMessage().getContent(), Formatting.BOLD)
				.build();

		targetChannel.sendMessageAsync(error, null);
	}
	 */

	public static boolean isDj(User author, Guild guild, TextChannel channel) {

		if(guild.getRolesForUser(author).stream().anyMatch(r -> r.getName().equalsIgnoreCase("dj"))
				|| author.getId().equals(BotInfo.getAuthorId()))
			return true;

		channel.sendMessageAsync(author.getAsMention() + ": You need to be a **DJ** to do that!", null);
		return false;
	}
}