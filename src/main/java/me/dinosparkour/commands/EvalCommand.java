/*
 * (C) Copyright 2016 Dinos
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

package me.dinosparkour.commands;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Arrays;
import java.util.List;

public class EvalCommand extends Command {

    private final ScriptEngine engine;

    public EvalCommand() {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            engine.eval("var imports = new JavaImporter(java.io, java.lang, java.util, Packages.net.dv8tion.jda.core, "
                    + "Packages.net.dv8tion.jda.core.entities, Packages.net.dv8tion.jda.core.managers);");
        } catch (ScriptException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void executeCommand(String[] args, MessageReceivedEvent e, Command.MessageSender chat) {
        String allArgs = e.getMessage().getContent();
        if (allArgs.contains(" ")) {
            allArgs = allArgs.substring(allArgs.indexOf(' ')).trim();
        }

        engine.put("e", e);
        engine.put("event", e);
        engine.put("api", e.getJDA());
        engine.put("jda", e.getJDA());
        engine.put("chat", chat);
        engine.put("channel", e.getChannel());
        engine.put("author", e.getAuthor());
        engine.put("member", e.getMember());
        engine.put("message", e.getMessage());
        engine.put("guild", e.getGuild());
        engine.put("input", allArgs);
        engine.put("selfUser", e.getJDA().getSelfUser());
        engine.put("selfMember", e.getGuild() == null ? null : e.getGuild().getSelfMember());
        engine.put("mentionedUsers", e.getMessage().getMentionedUsers());
        engine.put("mentionedRoles", e.getMessage().getMentionedRoles());
        engine.put("mentionedChannels", e.getMessage().getMentionedChannels());

        Object out;
        try {
            out = engine.eval("(function() { with (imports) {\n" + allArgs + "\n} })();");
        } catch (Exception ex) {
            chat.sendMessage("**Exception**: ```\n" + ex.getLocalizedMessage() + "```");
            return;
        }

        String outputS;
        if (out == null) {
            outputS = "`Task executed without errors.`";
        } else {
            outputS = "Output: ```\n" + out.toString().replace("`", "\\`") + "\n```";
        }

        if (e.getJDA().getStatus() != JDA.Status.SHUTDOWN) {
            chat.sendMessage(outputS);
        } else {
            System.exit(0);
        }
    }

    @Override
    public List<String> getAlias() {
        return Arrays.asList("eval", "evaluate", "exec", "execute");
    }

    @Override
    public boolean allowsPrivate() {
        return true;
    }

    @Override
    public boolean authorExclusive() {
        return true;
    }
}