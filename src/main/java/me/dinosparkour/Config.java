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

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class Config {

    private final File configFile = new File("config.json");
    private JSONObject configObject;

    Config() {
        if (!configFile.exists()) {
            create(); // If the config.json file doesn't exist, generate it.
            System.out.println("Created a config file. Please fill in the credentials.");
            System.exit(0);
        }

        JSONObject object = read(configFile);
        if (object.has("token") && object.has("prefix") && object.has("authorid")) {
            configObject = object;
        } else {
            create(); // If a value is missing, regenerate the config file.
            System.err.println("A value was missing in the config file! Regenerating..");
            System.exit(1);
        }
    }

    String getValue(String key) {
        return configObject == null ? null : configObject.get(key).toString();
    }

    private void create() {
        try {
            Files.write(Paths.get(configFile.getPath()),
                    new JSONObject()
                            .put("authorid", "")
                            .put("prefix", ".")
                            .put("token", "")
                            .toString(4)
                            .getBytes());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private JSONObject read(File file) {
        JSONObject obj = null;
        try {
            obj = new JSONObject(new String(Files.readAllBytes(Paths.get(file.getPath())), "UTF-8"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return obj;
    }
}