package me.dinosparkour.main;

import me.dinosparkour.utilities.ConfigHandler;
import org.json.JSONObject;

public class BotInfo {

	private static JSONObject jsonObject = ConfigHandler.getConfig();
	private static String email = jsonObject.getString("email");
	private static String password = jsonObject.getString("password");
	private static String prefix = jsonObject.getString("prefix");

	public static String getAuthorId() {
		return "98457903660269568";
	}

	static String getEmail() {
		return BotInfo.email;
	}

	static String getPassword() {
		return BotInfo.password;
	}

	public static String getPrefix() {
		return BotInfo.prefix;
	}
}