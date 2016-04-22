/*
 *     Copyright 2016 Austin Keener
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

package net.dv8tion.jda.player.hooks;


import net.dv8tion.jda.player.hooks.events.PlayerEvent;

import java.util.LinkedList;
import java.util.List;

public class PlayerEventManager {
    private final List<PlayerEventListener> listeners = new LinkedList<>();

    public PlayerEventManager() {
    }

    public void register(PlayerEventListener listener) {
        if (listeners.contains(listener))
            throw new IllegalArgumentException("Attempted to register a listener that is already registered");
        listeners.add(listener);
    }

    public void unregister(PlayerEventListener listener) {
        listeners.remove(listener);
    }

    public void handle(PlayerEvent event) {
        List<PlayerEventListener> listenerCopy = new LinkedList<>(listeners);
        for (PlayerEventListener listener : listenerCopy) {
            try {
                listener.onEvent(event);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
}
