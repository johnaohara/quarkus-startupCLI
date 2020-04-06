package io.quarkus.ts.startstop;

import io.quarkus.ts.startstop.utils.App;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

public class YamlTest {

    private static final String YAML_DOC = "" +
            "name:  app-jax-rs-minimal\n" +
            "dir: app-jax-rs-minimal\n" +
            "modes:\n" +
            "  - jvm\n" +
            "validationUrls:\n" +
            "  http://localhost:8080: Hello from a simple JAX-RS app.\n" +
            "  http://localhost:8080/data/hello: Hello World\n" +
            "whitelistLogLines:\n" +
            "  - \"maven-error-diagnostics\"\n" +
            "  - \"errorprone\"\n" +
            "thresholds:\n" +
            "  linux.jvm.time.to.first.ok.request.threshold.ms: 2000\n" +
            "  linux.jvm.RSS.threshold.kB: 380000\n" +
            "  linux.native.time.to.first.ok.request.threshold.ms: 35\n" +
            "  linux.native.RSS.threshold.kB: 90000\n" +
            "  windows.jvm.time.to.first.ok.request.threshold.ms: 2000\n" +
            "  windows.jvm.RSS.threshold.kB: 4000";

    @Test
    public void loadAsAppTest() {

        App yamlApp = loadAsApp(YAML_DOC);
        System.out.println("");
        System.out.println(yamlApp);

    }

    @Test
    public void loadAsObjectTest() {

        Object yaml = loadAsObject(YAML_DOC);
        System.out.println("");
        System.out.println(yaml);

    }

    private App loadAsApp(String yamlDoc){

        Yaml yaml = new Yaml(new Constructor(App.class));

        System.out.println(yamlDoc);

        return yaml.load(yamlDoc);
    }

    private App loadStreamAsApp(InputStream inputStream){
        Yaml yaml = new Yaml(new Constructor(App.class));

        return yaml.load(inputStream);

    }

    private Object loadAsObject(String yamlDoc){
        Yaml yaml = new Yaml();

        System.out.println(yamlDoc);

        return yaml.load(yamlDoc);

    }

    @Test
    public void testMultiLineWhitelistPattern(){

        final String yaml_resource = "multiLinePattern.yaml";

        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(yaml_resource);

        if ( inputStream != null) {
            App yamlApp = loadStreamAsApp(inputStream);
            System.out.println("");
            System.out.println(yamlApp);
        } else {
            Assertions.fail("Failed to load resource: ".concat(yaml_resource));
        }

    }
}
