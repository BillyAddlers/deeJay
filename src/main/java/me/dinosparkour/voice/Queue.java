package me.dinosparkour.voice;

import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.exceptions.PermissionException;

import java.util.ArrayList;
import java.util.List;

class Queue {

    private static List<Song> playlist = new ArrayList<>();

    static Song getCurrentSong() {
        return playlist.get(0);
    }

    static Song getSongAt(int x) throws IndexOutOfBoundsException {
        return playlist.get(x);
    }

    static Guild getCurrentGuild() {
        return playlist.get(0).getGuild();
    }

    static VoiceChannel getCurrentVoice() {
        return playlist.get(0).getVoiceChannel();
    }

    static TextChannel getCurrentText() {
        return playlist.get(0).getTextChannel();
    }

    static void addSong(Song s) throws PermissionException {
        playlist.add(s);
        s.getTextChannel().sendMessageAsync("Added to the Queue! [Position: **" + Queue.getSize() + "**]", null);
    }

    static void removeCurrentSong() {
        playlist.remove(0);
    }

    static boolean isEmpty() {
        return playlist.isEmpty();
    }

    static boolean isInLine(User dj) {
        return playlist.stream().anyMatch(s -> s.getDj().getId().equals(dj.getId()));
    }

    static int getSize() {
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