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

package me.dinosparkour;

import me.dinosparkour.commands.EvalCommand;
import me.dinosparkour.commands.MusicCommand;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;

import javax.security.auth.login.LoginException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bot {

    public static void main(String[] args) throws LoginException, RateLimitedException {
        new JDABuilder(AccountType.BOT)
                .addEventListener(new EvalCommand()) // Register the author-exclusive eval command
                .addEventListener(new MusicCommand()) // Register all music related subcommands
                .setToken(Info.TOKEN) // Set the Authentication Token
                .setBulkDeleteSplittingEnabled(false) // Performance reasons
                .setEventManager(new ThreadedEventManager()) // Allow for simultaneous command processing
                .buildAsync(); // Finally establish a connection to Discord's servers!
    }

    private static class ThreadedEventManager extends InterfacedEventManager {
        private final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

        @Override
        public void handle(Event e) {
            threadPool.submit(() -> super.handle(e));
        }
    }
}