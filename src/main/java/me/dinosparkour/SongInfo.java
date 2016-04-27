package me.dinosparkour;

import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;

import java.util.ArrayList;

class SongInfo {

    private static ArrayList<String> skips = new ArrayList<>();
    private User author;
    private Guild guild;

    SongInfo(User author, Guild guild) {
        this.author = author;
        this.guild = guild;
    }

    User getAuthor() {
        return author;
    }

    VoiceChannel getVoiceChannel() {
        return guild.getVoiceStatusOfUser(author).getChannel();
    }

    Guild getGuild() {
        return guild;
    }

    int getVotes() {
        return skips.size();
    }

    void voteSkip(User u) {
        skips.add(u.getId());
    }

    boolean hasVoted(User u) {
        return skips.stream().filter(uId -> u.getId().equals(uId)).findAny().orElse(null) != null;
    }
}