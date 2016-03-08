package me.dinosparkour.main;

import java.util.concurrent.*;

import javax.script.*;

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

import me.dinosparkour.main.BotInfo;
import me.dinosparkour.utilities.Tasks;

//Credits to DV8FromTheWorld and Almighty Alpaca
public class EvalCommand extends ListenerAdapter {

	private ScriptEngine engine;

	public EvalCommand() {
		engine = new ScriptEngineManager().getEngineByName("nashorn");
		try {
			engine.eval("var imports = new JavaImporter(java.io, java.lang, java.util);");

		} catch (ScriptException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent e) {

		if(!e.getAuthor().getId().equals(BotInfo.getAuthorId())
				|| !e.getMessage().getContent().startsWith(BotInfo.getPrefix() + "eval"))
			return;

		TextChannel channel = e.getChannel();
		String message = e.getMessage().getContent();
		String prefix = BotInfo.getPrefix();
		String input = message.replaceFirst(prefix + "eval", "");

		engine.put("jda", e.getJDA());
		engine.put("e", e);
		engine.put("message", e.getMessage());
		engine.put("guild", e.getGuild());
		engine.put("channel", e.getChannel());
		engine.put("author", e.getAuthor());
		engine.put("input", input);

		ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
		ScheduledFuture<Object> future = service.schedule(new Callable<Object>() {

			@Override
			public Object call() throws Exception {
				Object out = null;
				out = engine.eval(
						"(function() {" +
								"with (imports) {\n" + input + "\n}" +
						"})();");

				if(out != null)
					sendMessage(out.toString(), channel);

				return null;
			}                                                                           
		}, 0, TimeUnit.MILLISECONDS);

		Thread script = new Thread("eval code"){
			@Override
			public void run() {
				try {
					future.get(3, TimeUnit.SECONDS);

				} catch (TimeoutException  ex) {
					future.cancel(true);
					sendMessage("Your task exceeds the time limit!", channel);

				} catch (ExecutionException | InterruptedException  ex) {
					String cause = ex.getMessage();
					if(cause != null && channel.checkPermission(e.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE))
						sendMessage("```" + cause + "```", channel);
				}
			}
		};
		script.start();
	}

	private void sendMessage(String msg, TextChannel channel) {
		Tasks.sendMessage(msg, channel);
	}
}