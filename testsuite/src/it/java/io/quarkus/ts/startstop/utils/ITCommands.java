package io.quarkus.ts.startstop.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static io.quarkus.ts.startstop.utils.Commands.getQuarkusPlatformVersion;

public class ITCommands extends Commands {

    public static void confAppPropsForSkeleton(String appDir) throws IOException {
        // Config, see app-generated-skeleton/README.md
        String appPropsSrc = Environment.getBaseDir() + File.separator + Apps.GENERATED_SKELETON.dir + File.separator + "application.properties";
        String appPropsDst = appDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "application.properties";
        Files.copy(Paths.get(appPropsSrc),
                Paths.get(appPropsDst), StandardCopyOption.REPLACE_EXISTING);
    }

    public static List<String> getGeneratorCommand(Set<TestFlags> flags, String[] baseCommand, String[] extensions, String repoDir) {
        List<String> generatorCmd = new ArrayList<>();
        if (Commands.isThisWindows) {
            generatorCmd.add("cmd");
            generatorCmd.add("/C");
        }
        generatorCmd.addAll(Arrays.asList(baseCommand));
        if (flags.contains(TestFlags.PRODUCT_BOM)) {
            generatorCmd.add("-DplatformArtifactId=quarkus-product-bom");
            generatorCmd.add("-DplatformGroupId=com.redhat.quarkus");
            generatorCmd.add("-DplatformVersion=" + getQuarkusPlatformVersion());
        } else if (flags.contains(TestFlags.QUARKUS_BOM)) {
            generatorCmd.add("-DplatformArtifactId=quarkus-bom");
        } else if (flags.contains(TestFlags.UNIVERSE_BOM)) {
            generatorCmd.add("-DplatformArtifactId=quarkus-universe-bom");
        } else if (flags.contains(TestFlags.UNIVERSE_PRODUCT_BOM)) {
            generatorCmd.add("-DplatformArtifactId=quarkus-universe-bom");
            generatorCmd.add("-DplatformGroupId=com.redhat.quarkus");
            generatorCmd.add("-DplatformVersion=" + getQuarkusPlatformVersion());
        }
        generatorCmd.add("-Dextensions=" + String.join(",", extensions));
        generatorCmd.add("-Dmaven.repo.local=" + repoDir);
        generatorCmd.add("--settings=" +  Environment.getBaseDir() + File.separator + Apps.GENERATED_SKELETON.dir + File.separator + "settings.xml");

        return Collections.unmodifiableList(generatorCmd);
    }

}
