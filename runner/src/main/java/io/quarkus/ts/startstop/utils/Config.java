package io.quarkus.ts.startstop.utils;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

public class Config {


    public static  App loadAppDefinition(String appYamlFilePath) {
        try (InputStream appsYamlIs = new FileInputStream(new File(appYamlFilePath))) {

            if (appsYamlIs == null) {
                throw new IllegalStateException("Config file: ".concat(appYamlFilePath).concat(" not found"));
            }

            return Config.loadAppDefinition(appsYamlIs);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static  App loadAppDefinition(InputStream inputStream) {

        App loadedApp = null;

        try {
            Yaml yaml = new Yaml(new Constructor(App.class));

            loadedApp = yaml.load(inputStream);

        } catch (YAMLException yamlException) {
            throw new IllegalStateException(yamlException);
        }

        return loadedApp;
    }

    public static Map<String, App> loadAppDefinitions(String resourceName) {
        try (InputStream appsYamlIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {

            if (appsYamlIs == null) {
                throw new IllegalStateException("Config file: ".concat(resourceName).concat(" not found"));
            }

            return Config.loadAppDefinitions(appsYamlIs);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Map<String, App> loadAppDefinitions(InputStream inputStream) {

        Map<String, App> loadedApps = new TreeMap<>();

        try {
            Yaml yaml = new Yaml(new Constructor(App.class));

            for (Object app : yaml.loadAll(inputStream)) {
                loadedApps.put(((App)app).getName(), (App) app);
            }
        } catch (YAMLException yamlException) {
            throw new IllegalStateException(yamlException);
        }

        return loadedApps;
    }

}
