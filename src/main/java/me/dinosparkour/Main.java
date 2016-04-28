package me.dinosparkour;

import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.managers.AudioManager;
import net.dv8tion.jda.player.MusicPlayer;
import net.dv8tion.jda.player.source.AudioSource;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main {

    static final String AUTHOR_ID = "98457903660269568";
    static final Map<AudioSource, SongInfo> musicQueue = new HashMap<>();
    private static final float DEFAULT_VOLUME = 0.25f;
    static String prefix;

    public static void main(String[] args) throws LoginException, InterruptedException, IOException {

        JSONObject config = Configurator.getConfig();
        prefix = config.getString("prefix");
        new JDABuilder()
                .setBotToken(config.getString("token"))
                .addListener(new EvalCommand())
                .addListener(new Listener())
                .buildAsync();
    }

    static MusicPlayer createPlayer(AudioManager am) {
        MusicPlayer myPlayer = new MusicPlayer() {
            @Override
            public void stop() {
                super.stop();
                am.closeAudioConnection();
                musicQueue.remove(super.getPreviousAudioSource());
            }

            @Override
            public void playNext(boolean b) {
                super.playNext(b);
                super.setVolume(DEFAULT_VOLUME);

                musicQueue.remove(super.getPreviousAudioSource());
                AudioSource src = super.getCurrentAudioSource();
                if (src == null)
                    am.closeAudioConnection();
                else {
                    VoiceChannel vChan = musicQueue.get(src).getVoiceChannel();
                    if (vChan == null)
                        playNext(b);
                    else if (vChan != am.getConnectedChannel())
                        am.moveAudioConnection(vChan);
                }
            }

            @Override
            public void play() {
                super.play();
                super.setVolume(DEFAULT_VOLUME);

                VoiceChannel vChan = musicQueue.get(super.getCurrentAudioSource()).getVoiceChannel();
                if (vChan == null)
                    super.skipToNext();
                else {
                    if (am.isConnected())
                        am.moveAudioConnection(vChan);
                    else
                        am.openAudioConnection(vChan);
                }
            }
        };
        myPlayer.setVolume(DEFAULT_VOLUME);
        return myPlayer;
    }
}