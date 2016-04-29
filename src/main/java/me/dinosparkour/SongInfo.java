package me.dinosparkour;

import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;

import java.util.ArrayList;

class SongInfo {

    private static final ArrayList<String> skips = new ArrayList<>();
    private final User author;
    private final Guild guild;

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

    String getGuildId() {
        return guild.getId();
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