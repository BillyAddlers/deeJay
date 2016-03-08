package me.dinosparkour.main;

import org.json.JSONObject;

import me.dinosparkour.utilities.ConfigHandler;

public class BotInfo {

	private static String version = "3.8";
	private static String authorId = "98457903660269568";

	private static JSONObject jsonObject = ConfigHandler.getConfig();
	private static String email = jsonObject.getString("email");
	private static String password = jsonObject.getString("password");
	private static String prefix = jsonObject.getString("prefix");

	public static String getVersion() {
		String version = BotInfo.version;
		return version;
	}

	public static String getAuthorId() {
		String authorId = BotInfo.authorId;
		return authorId;
	}

	protected static String getEmail() {
		String email = BotInfo.email;
		return email;
	}

	protected static String getPassword() {
		String password = BotInfo.password;
		return password;
	}

	public static String getPrefix() {
		String prefix = BotInfo.prefix;
		return prefix;
	}
}