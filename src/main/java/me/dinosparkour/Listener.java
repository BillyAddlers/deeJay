package me.dinosparkour;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.managers.AudioManager;
import net.dv8tion.jda.player.MusicPlayer;
import net.dv8tion.jda.player.Playlist;
import net.dv8tion.jda.player.source.AudioInfo;
import net.dv8tion.jda.player.source.AudioSource;
import net.dv8tion.jda.player.source.AudioTimestamp;
import net.dv8tion.jda.player.source.RemoteSource;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static me.dinosparkour.Main.AUTHOR_ID;
import static me.dinosparkour.Main.musicQueue;

class Listener extends ListenerAdapter {

    private static final int SKIP_VOTES_REQUIRED = 4;
    private static final Set<MusicPlayer> multiPlayers = new HashSet<>();
    private static final Set<String> playlistLoader = new HashSet<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

    private static boolean noDjRole(User author, TextChannel channel) {
        return !(channel.getGuild().getRolesForUser(author).stream().anyMatch(r ->
                r.getName().equalsIgnoreCase("dj"))
                || author.getId().equals(AUTHOR_ID));
    }

    private static boolean isCurrentDj(MusicPlayer player, User author) {
        return !musicQueue.isEmpty() && musicQueue.get(player.getCurrentAudioSource()).getAuthor() == author;
    }

    private static boolean isIdle(MusicPlayer player, TextChannel channel) {
        if (!player.isPlaying()) {
            channel.sendMessage("The bot isn't playing music in this guild!");
            return true;
        }
        return false;
    }

    private static boolean isInNoChannel(User author, TextChannel channel) {
        VoiceChannel vChan = channel.getGuild().getVoiceStatusOfUser(author).getChannel();
        if (vChan == null) {
            channel.sendMessage("You are not in a voice channel!");
            return true;
        }
        return false;
    }

    private static void addSingleSource(AudioSource src, MusicPlayer player, Message message) {
        TextChannel channel = (TextChannel) message.getChannel();
        Guild guild = channel.getGuild();
        User author = message.getAuthor();
        AudioInfo srcInfo = src.getInfo();
        if (srcInfo.getError() == null) {
            if (channel.checkPermission(channel.getJDA().getSelfInfo(), Permission.MESSAGE_MANAGE))
                message.deleteMessage();

            player.getAudioQueue().add(src);
            musicQueue.put(src, new SongInfo(author, guild));

            channel.sendMessage("```\n" + srcInfo.getTitle() + " has been added to the queue!\n=> Requested by "
                    + author.getUsername().replace("`", "\\`") + "#" + author.getDiscriminator()
                    + " - Position: [" + (player.getAudioQueue().size() - 1) + "]\n```");

            if (!player.isPlaying())
                player.play();
        } else
            channel.sendMessage("```" + srcInfo.getError() + "```");
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
        if (!command.equalsIgnoreCase("music"))
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
                if (noDjRole(author, channel) && !isCurrentDj(player, author)) {
                    channel.sendMessage(author.getAsMention() + ": You need to be a **DJ** to do that!");
                    return;
                }

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
            case "current":
            case "playing":
            case "nowplaying":
                if (isIdle(player, channel))
                    return;

                AudioTimestamp currTime = player.getCurrentTimestamp();
                AudioInfo info = player.getCurrentAudioSource().getInfo();
                channel.sendMessage("**Song:** " + info.getTitle() + "\n"
                        + (info.getError() != null ? "" :
                        "**Time:**   [ " + currTime.getTimestamp() + " / " + info.getDuration().getTimestamp() + " ]"));
                break;

            case "forceskip":
                if (isIdle(player, channel))
                    return;

                if (noDjRole(author, channel) && !isCurrentDj(player, author)) {
                    channel.sendMessage(author.getAsMention() + ": You need to be a **DJ** to do that!");
                    return;
                }

                player.skipToNext();
                channel.sendMessage("Skipped current song.");
                break;

            case "skip":
                if (isIdle(player, channel))
                    return;

                SongInfo s = musicQueue.get(player.getCurrentAudioSource());
                if (isCurrentDj(player, author)) {
                    channel.sendMessage("The DJ has decided to skip!");
                    player.skipToNext();
                } else {
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
                am.closeAudioConnection();
                am.setSendingHandler(player);
                musicQueue.entrySet().stream().filter(entry -> entry.getValue().getGuildId().equals(guild.getId())).forEach(musicQueue::remove);
                channel.sendMessage("Completely reset the music player.");
                break;

            case "pause":
                if (isIdle(player, channel))
                    return;

                if (noDjRole(author, channel) && !isCurrentDj(player, author)) {
                    channel.sendMessage(author.getAsMention() + ": You need to be a **DJ** to do that!");
                    return;
                }

                player.pause();
                channel.sendMessage("Paused the player.");
                break;

            case "stop":
                if (isIdle(player, channel))
                    return;

                if (noDjRole(author, channel) && !isCurrentDj(player, author)) {
                    channel.sendMessage(author.getAsMention() + ": You need to be a **DJ** to do that!");
                    return;
                }

                player.stop();
                channel.sendMessage("Stopped the player.");
                break;

            case "multiqueue":
                if (noDjRole(author, channel)) {
                    channel.sendMessage(author.getAsMention() + ": You need to be a **DJ** to do that!");
                    return;
                }

                if (inputArgs.contains(" ")) {
                    channel.sendMessage("Invalid value!");
                    return;
                }

                switch (inputArgs.toLowerCase()) {
                    case "":
                        StringBuilder string = new StringBuilder("Multiqueue now set to ");
                        if (multiPlayers.contains(player)) {
                            multiPlayers.remove(player);
                            channel.sendMessage(string.append("**false**.").toString());
                        } else {
                            multiPlayers.add(player);
                            channel.sendMessage(string.append("**true**.").toString());
                        }
                        break;

                    case "1":
                    case "yes":
                    case "true":
                    case "allow":
                        if (multiPlayers.contains(player))
                            channel.sendMessage("Multiqueue is already enabled!");
                        else {
                            multiPlayers.add(player);
                            channel.sendMessage("Multiqueue now set to **true**.");
                        }
                        break;

                    case "0":
                    case "no":
                    case "false":
                    case "deny":
                        if (!multiPlayers.contains(player))
                            channel.sendMessage("Multiqueue is already disabled!");
                        else {
                            multiPlayers.remove(player);
                            channel.sendMessage("Multiqueue now set to **false**.");
                        }
                        break;

                    default:
                        channel.sendMessage("Invalid value!");
                        break;
                }
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
                    if (isInNoChannel(author, channel))
                        return;

                    if (musicQueue.entrySet().stream().anyMatch(entry -> entry.getValue().getAuthor() == author) && !multiPlayers.contains(player)) {
                        channel.sendMessage("Multiqueue is disabled! You may only have 1 song queued at all times.");
                        return;
                    }

                    channel.sendTyping();
                    AudioSource src = new RemoteSource(inputArgs);
                    if (src.getInfo().getError() != null)
                        channel.sendMessage("An error occurred!```" + src.getInfo().getError() + "```");
                    else if (src.getInfo().isLive())
                        channel.sendMessage("Cannot play livestreams! Sorry for the inconvenience.");
                    else
                        addSingleSource(src, player, message);
                }
                break;

            case "playlist":
                if (isInNoChannel(author, channel))
                    return;

                if (!multiPlayers.contains(player)) {
                    channel.sendMessage("This feature requires multiqueue to be enabled!\n"
                            + "\nPlease use `" + prefix + "music multiqueue true`");
                    return;
                }

                if (inputArgs.equals("")) {
                    channel.sendMessage("Please select a playlist using `" + prefix + "music playlist (url)`");
                    return;
                }

                Message playlistStatus = channel.sendMessage("*Processing playlist..*");
                Playlist playlist = Playlist.getPlaylist(inputArgs);
                List<AudioSource> sources = new LinkedList<>(playlist.getSources());

                if (sources.size() <= 1) {
                    AudioSource src = new RemoteSource(inputArgs);
                    addSingleSource(src, player, message);
                } else {
                    if (playlistLoader.contains(guild.getId())) {
                        playlistStatus.updateMessage("The player is already loading a playlist for this guild! Please be patient..");
                        return;
                    }

                    playlistLoader.add(guild.getId());
                    playlistStatus.updateMessage("*Attempting to load " + sources.size() + " songs, this may take a while..*");

                    final MusicPlayer fPlayer = player;
                    threadPool.submit(() -> {
                        sources.stream().forEachOrdered(audioSource -> {
                            AudioInfo audioInfo = audioSource.getInfo();
                            if (audioInfo.isLive()) {
                                channel.sendMessage("Cannot play livestreams! Sorry for the inconvenience.");
                                return;
                            }

                            List<AudioSource> audioQueue = fPlayer.getAudioQueue();
                            if (audioInfo.getError() == null) {
                                musicQueue.put(audioSource, new SongInfo(author, guild));
                                audioQueue.add(audioSource);
                                if (fPlayer.isStopped())
                                    fPlayer.play();
                            } else
                                channel.sendMessage("Detected error, skipping 1 source. Error:```" + audioInfo.getError() + "```");
                        });
                        playlistLoader.remove(guild.getId());
                    });
                }
                break;

            case "":
            case "help":
            case "commands":
                channel.sendMessage("```\n"
                        + prefix + "music\n"
                        + "    -> play (url)\n"
                        + "    -> playlist (playlist url) - [Requires multiqueue to be enabled]\n"
                        + "    -> volume [0.0 - 1.0] - [Current Song Requester/DJ only]\n"
                        + "    -> queue\n"
                        + "    -> skip\n"
                        + "    -> nowplaying\n"
                        + "    -> pause - [DJ only]\n"
                        + "    -> stop - [DJ only]\n"
                        + "    -> forceskip - [DJ only]\n"
                        + "    -> multiqueue (true/false) - [DJ only]\n```");
                break;
        }
    }
}