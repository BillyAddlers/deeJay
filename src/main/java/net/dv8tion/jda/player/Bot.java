/*
 *     Copyright 2016 Austin Keener
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.player;

import me.dinosparkour.Configurator;
import me.dinosparkour.EvalCommand;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.managers.AudioManager;
import net.dv8tion.jda.player.source.AudioInfo;
import net.dv8tion.jda.player.source.AudioSource;
import net.dv8tion.jda.player.source.AudioTimestamp;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Bot extends ListenerAdapter {
    public static final String AUTHOR_ID = "98457903660269568";
    private static final float DEFAULT_VOLUME = 0.25f;
    public static String prefix;

    public static void main(String[] args) throws LoginException, InterruptedException, IOException {
        JSONObject config = Configurator.getConfig();
        prefix = config.getString("prefix");

        new JDABuilder()
                .setBotToken(config.getString("token"))
                .addListener(new Bot())
                .addListener(new EvalCommand())
                .buildAsync();
    }

    private static boolean isDj(User author, Guild guild, TextChannel channel) {
        if (guild.getRolesForUser(author).stream().anyMatch(r -> r.getName().equalsIgnoreCase("dj")) || author.getId().equals(AUTHOR_ID))
            return true;

        channel.sendMessage(author.getAsMention() + ": You need to be a **DJ** to do that!");
        return false;
    }

    //Current commands
    // join (name)  - Joins a voice channel that has the provided name
    // leave        - Leaves the voice channel that the bot is currently in.
    // play         - Plays songs from the current queue. Starts playing again if it was previously paused
    // play [url]   - Adds a new song to the queue and starts playing if it wasn't playing already
    // pause        - Pauses audio playback
    // stop         - Completely stops audio playback, skipping the current song.
    // skip         - Skips the current song, automatically starting the next
    // nowplaying   - Prints information about the currently playing song (title, current time)
    // queue         - Lists the songs in the queue
    // volume [val] - Sets the volume of the MusicPlayer [0.0 - 1.0]
    // reset        - Completely resets the player, fixing all errors and clearing the queue.
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        String message = event.getMessage().getContent();
        AudioManager manager = event.getGuild().getAudioManager();
        MusicPlayer player;
        if (manager.getSendingHandler() == null) {
            player = new MusicPlayer();
            player.setVolume(DEFAULT_VOLUME);
            manager.setSendingHandler(player);
        } else {
            player = (MusicPlayer) manager.getSendingHandler();
        }

        if (message.startsWith(prefix + "volume ")) {
            if (!isDj(event.getAuthor(), event.getGuild(), event.getChannel()))
                return;

            float volume = Float.parseFloat(message.substring((prefix + "volume ").length()));
            volume = Math.min(1F, Math.max(0F, volume));
            player.setVolume(volume);
            event.getChannel().sendMessage("Volume was changed to: " + volume);
        }

        if (message.equals(prefix + "queue")) {
            List<AudioSource> queue = player.getAudioQueue();
            if (queue.isEmpty()) {
                event.getChannel().sendMessage("The queue is currently empty!");
                return;
            }


            MessageBuilder builder = new MessageBuilder();
            builder.appendString("__Current Queue.  Entries: " + queue.size() + "__\n");
            for (int i = 0; i < queue.size() && i < 10; i++) {
                AudioInfo info = queue.get(i).getInfo();
//                builder.appendString("**(" + (i + 1) + ")** ");
                if (info == null)
                    builder.appendString("*Could not get info for this song.*");
                else {
                    AudioTimestamp duration = info.getDuration();
                    builder.appendString("`[");
                    if (duration == null)
                        builder.appendString("N/A");
                    else
                        builder.appendString(duration.getTimestamp());
                    builder.appendString("]` " + info.getTitle() + "\n");
                }
            }

            boolean error = false;
            int totalSeconds = 0;
            for (AudioSource source : queue) {
                AudioInfo info = source.getInfo();
                if (info == null || info.getDuration() == null) {
                    error = true;
                    continue;
                }
                totalSeconds += info.getDuration().getTotalSeconds();
            }

            builder.appendString("\nTotal Queue Time Length: " + AudioTimestamp.fromSeconds(totalSeconds).getTimestamp());
            if (error)
                builder.appendString("`An error occurred calculating total time. Might not be completely valid.");
            event.getChannel().sendMessage(builder.build());
        }

        if (message.equals(prefix + "nowplaying")) {
            if (player.isPlaying()) {
                AudioTimestamp currentTime = player.getCurrentTimestamp();
                AudioInfo info = player.getCurrentAudioSource().getInfo();
                if (info.getError() == null) {
                    event.getChannel().sendMessage(
                            "**Playing:** " + info.getTitle() + "\n" +
                                    "**Time:**    [" + currentTime.getTimestamp() + " / " + info.getDuration().getTimestamp() + "]");
                } else {
                    event.getChannel().sendMessage(
                            "**Playing:** Info Error. Known source: " + player.getCurrentAudioSource().getSource() + "\n" +
                                    "**Time:**    [" + currentTime.getTimestamp() + " / (N/A)]");
                }
            } else {
                event.getChannel().sendMessage("The player is not currently playing anything!");
            }
        }

        //Start an audio connection with a VoiceChannel
        if (message.startsWith(prefix + "join")) {
            if (event.getGuild().getVoiceStatusOfUser(event.getJDA().getSelfInfo()).getChannel() != null) {
                event.getChannel().sendMessage("I'm already in a Voice Channel in this Guild!");
                return;
            }

            //Separates the name of the channel so that we can search for it
            String chanName = message.substring((prefix + "join").length()).trim();

            if (chanName.equals("")) {
                VoiceChannel authorChannel = event.getGuild().getVoiceStatusOfUser(event.getAuthor()).getChannel();
                if (authorChannel == null) {
                    event.getChannel().sendMessage("You are not in a voice channel!");
                    return;
                }
                manager.openAudioConnection(authorChannel);
            } else {
                //Scans through the VoiceChannels in this Guild, looking for one with a case-insensitive matching name.
                VoiceChannel channel = event.getGuild().getVoiceChannels().stream().filter(
                        vChan -> vChan.getName().toLowerCase().contains(chanName))
                        .findFirst().orElse(null);  //If there isn't a matching name, return null.
                if (channel == null) {
                    event.getChannel().sendMessage("There are no Voice Channels in this Guild that contain '" + chanName + "'");
                    return;
                }
                manager.openAudioConnection(channel);
            }
        }

        //Disconnect the audio connection with the VoiceChannel.
        if (message.equals(prefix + "leave")) {
            if (!isDj(event.getAuthor(), event.getGuild(), event.getChannel()))
                return;

            manager.closeAudioConnection();
        }

        if (message.equals(prefix + "skip")) {
            if (!isDj(event.getAuthor(), event.getGuild(), event.getChannel()))
                return;

            player.skipToNext();
            event.getChannel().sendMessage("Skipped the current song.");
        }


        if (message.equals(prefix + "reset")) {
            if (!isDj(event.getAuthor(), event.getGuild(), event.getChannel()))
                return;

            player.stop();
            player = new MusicPlayer();
            player.setVolume(DEFAULT_VOLUME);
            manager.setSendingHandler(player);
            event.getChannel().sendMessage("Music player has been completely reset.");
        }

        //Start playing audio with our FilePlayer. If we haven't created and registered a FilePlayer yet, do that.
        if (message.startsWith(prefix + "play")) {
            //If no URL was provided.
            if (message.equals(prefix + "play")) {
                if (player.isPlaying()) {
                    event.getChannel().sendMessage("Player is already playing!");
                    return;
                } else if (player.isPaused()) {
                    player.play();
                    event.getChannel().sendMessage("Playback as been resumed.");
                } else {
                    if (player.getAudioQueue().isEmpty())
                        event.getChannel().sendMessage("The current audio queue is empty! Add something to the queue first!");
                    else {
                        player.play();
                        event.getChannel().sendMessage("Player has started playing!");
                    }
                }
            } else if (message.startsWith(prefix + "play ")) {
                String msg = "";
                String url = message.substring((prefix + "play ").length());
                Playlist playlist = Playlist.getPlaylist(url);
                List<AudioSource> sources = new LinkedList(playlist.getSources());
//                AudioSource source = new RemoteSource(url);
//                AudioSource source = new LocalSource(new File(url));
//                AudioInfo info = source.getInfo();   //Preload the audio info.
                if (sources.size() > 1) {
                    event.getChannel().sendMessage("Found a playlist with **" + sources.size() + "** entries.\n" +
                            "Proceeding to gather information and queue sources. This may take some time...");
                    final MusicPlayer fPlayer = player;
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            for (Iterator<AudioSource> it = sources.iterator(); it.hasNext(); ) {
                                AudioSource source = it.next();
                                AudioInfo info = source.getInfo();
                                List<AudioSource> queue = fPlayer.getAudioQueue();
                                if (info.getError() == null) {
                                    queue.add(source);
                                    if (fPlayer.isStopped())
                                        fPlayer.play();
                                } else {
                                    event.getChannel().sendMessage("Error detected, skipping source. Error:\n" + info.getError());
                                    it.remove();
                                }
                            }
                            event.getChannel().sendMessage("Finished queuing provided playlist. Successfully queued **" + sources.size() + "** sources");
                        }
                    };
                    thread.start();
                } else {
                    event.getChannel().sendTyping();
                    AudioSource source = sources.get(0);
                    AudioInfo info = source.getInfo();
                    if (info.getError() == null) {
                        player.getAudioQueue().add(source);
                        msg += "The provided URL has been added the to queue";
                        if (player.isStopped()) {
                            player.play();
                            msg += " and the player will start playing shortly";
                        }
                        event.getChannel().sendMessage(msg + ".");
                    } else {
                        event.getChannel().sendMessage("There was an error while loading the provided URL.\n" +
                                "Error: " + info.getError());
                    }
                }
            }
        }

        if (message.equals(prefix + "pause")) {
            if (!isDj(event.getAuthor(), event.getGuild(), event.getChannel()))
                return;

            player.pause();
            event.getChannel().sendMessage("Playback has been paused.");
        }

        if (message.equals(prefix + "stop")) {
            if (!isDj(event.getAuthor(), event.getGuild(), event.getChannel()))
                return;

            player.stop();
            event.getChannel().sendMessage("Playback has been completely stopped.");
        }
    }
}