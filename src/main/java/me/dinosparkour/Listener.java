package me.dinosparkour;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.managers.AudioManager;
import net.dv8tion.jda.player.MusicPlayer;
import net.dv8tion.jda.player.source.AudioInfo;
import net.dv8tion.jda.player.source.AudioSource;
import net.dv8tion.jda.player.source.AudioTimestamp;
import net.dv8tion.jda.player.source.RemoteSource;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;

import static me.dinosparkour.Main.AUTHOR_ID;
import static me.dinosparkour.Main.musicQueue;

class Listener extends ListenerAdapter {

    private static final int SKIP_VOTES_REQUIRED = 4;

    private static boolean isDj(User author, TextChannel channel, MusicPlayer player) {
        Guild guild = channel.getGuild();
        if (guild.getRolesForUser(author).stream().anyMatch(r ->
                r.getName().equalsIgnoreCase("dj"))
                || author.getId().equals(AUTHOR_ID)
                || musicQueue.get(player.getCurrentAudioSource()).getAuthor() == author)
            return true;

        channel.sendMessage(author.getAsMention() + ": You need to be a **DJ** to do that!");
        return false;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        JDA jda = e.getJDA();
        Guild guild = e.getGuild();
        TextChannel channel = e.getChannel();
        Message message = e.getMessage();
        String msgContent = message.getRawContent();
        User author = e.getAuthor();
        AudioManager am = guild.getAudioManager();

        String prefix = Main.prefix;

        /*
         * Ignore the message if:
         * 1) it doesn't start with our prefix
         * 2) it was sent by a bot
         * 3) it was sent by ourselves
         */
        if (!msgContent.startsWith(prefix) || author.isBot() || author == jda.getSelfInfo())
            return;

        String command = msgContent.split("\\s+")[0].substring(prefix.length());

        //Specifically listen for the music command
        if (!command.equals("music"))
            return;

        String input = msgContent.substring(prefix.length() + command.length()).trim();

        MusicPlayer player;
        if (am.getSendingHandler() == null) {
            player = Main.createPlayer(am);
            am.setSendingHandler(player);
        } else
            player = (MusicPlayer) am.getSendingHandler();

        String arg0 = input.split("\\s+")[0];
        String inputArgs = input.substring(arg0.length()).trim();
        switch (arg0.toLowerCase()) {
            case "volume":
                if (!isDj(author, channel, player))
                    return;

                if (inputArgs.equals("")) {
                    channel.sendMessage("Current volume: " + player.getVolume());
                } else {
                    if (!NumberUtils.isNumber(inputArgs)) {
                        channel.sendMessage("Please enter a valid value!");
                        return;
                    }

                    float newVol = Float.parseFloat(inputArgs);
                    if (newVol <= 1f) {
                        player.setVolume(newVol);
                        channel.sendMessage("Volume set to " + newVol);
                    } else
                        channel.sendMessage("That's too loud! Maximum volume is 1.00");
                }
                break;

            case "queue":
                List<AudioSource> queue = player.getAudioQueue();
                if (queue.isEmpty()) {
                    channel.sendMessage("The queue is currently empty");
                    return;
                }

                StringBuilder sb = new StringBuilder("__Information about the Queue__ (Entries: " + queue.size() + ")\n\n");
                for (int i = 0; i < queue.size() && i < 10; i++) {
                    AudioInfo info = queue.get(i).getInfo();
                    if (info != null) {
                        AudioTimestamp dur = info.getDuration();
                        sb.append("`[").append(dur == null ? "N/A" : dur.getTimestamp())
                                .append("]` ").append(info.getTitle()).append("\n");
                    }
                }

                boolean error = false;
                int totalSeconds = 0;
                for (AudioSource src : queue) {
                    AudioInfo info = src.getInfo();
                    if (info == null || info.getDuration() == null)
                        error = true;

                    assert info != null;
                    totalSeconds += info.getDuration().getTotalSeconds();
                }

                sb.append("\nTotal Queue Duration: ").append(AudioTimestamp.fromSeconds(totalSeconds).getTimestamp()).append(" minutes.");
                if (error)
                    sb.append("`An error occurred while calculating total time, please notice that it might not be completely valid.`");
                channel.sendMessage(sb.toString());
                break;

            case "now":
            case "playing":
            case "nowplaying":
                if (!player.isPlaying())
                    channel.sendMessage("The bot isn't playing music in this guild!");
                else {
                    AudioTimestamp currTime = player.getCurrentTimestamp();
                    AudioInfo info = player.getCurrentAudioSource().getInfo();
                    channel.sendMessage("**Song:** " + info.getTitle() + "\n"
                            + (info.getError() != null ? "" :
                            "**Time:**   [ " + currTime.getTimestamp() + " / " + info.getDuration().getTimestamp() + " ]"));
                }
                break;

            //TODO !music join/leave

            case "forceskip":
                if (!isDj(author, channel, player))
                    return;

                player.skipToNext();
                channel.sendMessage("Skipped current song.");
                break;

            case "skip":
                SongInfo s = musicQueue.get(player.getCurrentAudioSource());
                if (author == s.getAuthor())
                    player.skipToNext();
                else {
                    if (s.hasVoted(author)) {
                        channel.sendMessage("You have already voted to skip this song!");
                        return;
                    }

                    s.voteSkip(author);
                    int voteCount = s.getVotes();
                    if (voteCount == SKIP_VOTES_REQUIRED) {
                        player.skipToNext();
                        channel.sendMessage("Skipping to the next song.");
                    } else
                        channel.sendMessage(author.getUsername().replace("`", "\\`") + " has voted to skip the song! **" + voteCount + "/" + SKIP_VOTES_REQUIRED + "**");
                }
                break;

            case "reset":
                if (!author.getId().equals(Main.AUTHOR_ID))
                    return;

                player.stop();
                player = Main.createPlayer(am);
                am.setSendingHandler(player);
                channel.sendMessage("Completely reset the music player.");
                break;

            case "pause":
                if (!isDj(author, channel, player))
                    return;

                player.pause();
                channel.sendMessage("Paused the player.");
                break;

            case "stop":
                if (!isDj(author, channel, player))
                    return;

                player.stop();
                channel.sendMessage("Stopped the player.");
                break;

            case "play":
                if (inputArgs.equals(""))
                    if (player.isPlaying())
                        channel.sendMessage("The bot is already playing music!");
                    else if (player.isPaused()) {
                        player.play();
                        channel.sendMessage("Resumed the player.");
                    } else {
                        if (player.getAudioQueue().isEmpty())
                            channel.sendMessage("The queue is empty! Add a song with `" + prefix + "music play (url)`");
                        else {
                            player.play();
                            channel.sendMessage("Started the player.");
                        }

                    }
                else {
                    VoiceChannel vChan = guild.getVoiceStatusOfUser(author).getChannel();
                    if (vChan == null) {
                        channel.sendMessage("You are not in a voice channel!");
                        return;
                    }

                    Message status = channel.sendMessage("*Processing audio source..*");
                    //TODO add playlists
                    AudioSource src = new RemoteSource(inputArgs);
                    AudioInfo info = src.getInfo();
                    if (info.getError() == null) {
                        musicQueue.put(src, new SongInfo(author, guild));
                        player.getAudioQueue().add(src);
                        status.updateMessage("*The requested song has been added to the queue!*  " +
                                "**Position: [" + (player.getAudioQueue().size() - 1) + "]**");
                        if (!player.isPlaying())
                            player.play();
                    } else
                        status.updateMessage("```" + info.getError() + "```");
                }
                break;

            case "":
            case "help":
            case "commands":
                channel.sendMessage("```\n"
                        + prefix + "music\n"
                        + "    -> play (url)\n"
                        + "    -> volume [0.0 - 1.0]\n"
                        + "    -> queue\n"
                        + "    -> skip\n"
                        + "    -> nowplaying\n"
                        + "    -> pause - [DJ only]\n"
                        + "    -> stop - [DJ only]\n"
                        + "    -> forceskip - [DJ only]\n```");
                break;
        }
    }
}