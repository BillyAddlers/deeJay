package me.dinosparkour.voice;

import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;

class Song {

    private User user;
    private String url;
    private Guild guild;
    private TextChannel textChannel;
    private VoiceChannel voiceChannel;

    Song(User author, String input, Guild guild, TextChannel channel, VoiceChannel vChan) {
        this.user = author;
        this.url = input;
        this.guild = guild;
        this.textChannel = channel;
        this.voiceChannel = vChan;
    }

    User getDj() {
        return user;
    }

    String getUrl() {
        return url;
    }

    Guild getGuild() {
        return guild;
    }

    TextChannel getTextChannel() {
        return textChannel;
    }

    VoiceChannel getVoiceChannel() {
        return voiceChannel;
    }
}