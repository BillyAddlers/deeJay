package me.dinosparkour.utilities;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigHandler {

    private static File config = new File("config.json");

    public static JSONObject getConfig() {
        if(!config.exists()) {
            try {
                ConfigHandler.create();

            } catch (IOException ex) {
                ex.printStackTrace();
            }

            System.out.println("Created a configuration file. Please fill the login credentials!");
            System.exit(0);
        }

        JSONObject object = null;
        try {
            object = ConfigHandler.load();

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return object;
    }

    private static void create() throws IOException {
        Files.write(Paths.get(config.getPath()),
                new JSONObject()
                        .put("email", "")
                        .put("password", "")
                        .put("prefix", "!")
                        .toString(4).getBytes());
    }

    private static JSONObject load() throws IOException {
        JSONObject object = new JSONObject(new String(Files.readAllBytes(Paths.get(config.getPath())), "UTF-8"));

        if(object.has("email") && object.has("password") && object.has("prefix"))
            return object;

        ConfigHandler.create();
        System.err.println("The config file was missing a value! [Either email/password/prefix]");
        System.out.println("Regenerating the file from scratch..");
        System.exit(1);

        return null;
    }
}