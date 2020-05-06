package io.quarkus.ts.startstop.utils;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class ITCommands extends Commands {

    private static final Logger LOGGER = Logger.getLogger(ITCommands.class.getName());

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
            generatorCmd.add("-DplatformVersion=" + getQuarkusPlatformVersion(Environment.getBaseDir()));
        } else if (flags.contains(TestFlags.QUARKUS_BOM)) {
            generatorCmd.add("-DplatformArtifactId=quarkus-bom");
        } else if (flags.contains(TestFlags.UNIVERSE_BOM)) {
            generatorCmd.add("-DplatformArtifactId=quarkus-universe-bom");
        } else if (flags.contains(TestFlags.UNIVERSE_PRODUCT_BOM)) {
            generatorCmd.add("-DplatformArtifactId=quarkus-universe-bom");
            generatorCmd.add("-DplatformGroupId=com.redhat.quarkus");
            generatorCmd.add("-DplatformVersion=" + getQuarkusPlatformVersion(Environment.getBaseDir()));
        }
        generatorCmd.add("-Dextensions=" + String.join(",", extensions));
        generatorCmd.add("-Dmaven.repo.local=" + repoDir);
        generatorCmd.add("--settings=" +  Environment.getBaseDir() + File.separator + Apps.GENERATED_SKELETON.dir + File.separator + "settings.xml");

        return Collections.unmodifiableList(generatorCmd);
    }



    public static String getCodeQuarkusURL() {
        String url = null;
        for (String p : new String[]{"CODE_QUARKUS_URL", "code.quarkus.url"}) {
            String env = System.getenv().get(p);
            if (StringUtils.isNotBlank(env)) {
                url = env;
                break;
            }
            String sys = System.getProperty(p);
            if (StringUtils.isNotBlank(sys)) {
                url = sys;
                break;
            }
        }
        if (url == null) {
            url = "https://code.quarkus.io";
            LOGGER.warn("Failed to detect code.quarkus.url/CODE_QUARKUS_URL env/sys props, defaulting to " + url);
            return url;
        }
        Matcher m = trailingSlash.matcher(url);
        if (m.find()) {
            url = m.replaceAll("");
        }
        return url;
    }

    /**
     * Download a zip file with an example project
     *
     * @param extensions         collection of extension codes, @See {@link io.quarkus.ts.startstop.utils.CodeQuarkusExtensions}
     * @param destinationZipFile path where the zip file will be written
     * @return the actual URL used for audit and logging purposes
     * @throws IOException
     */
    public static String download(Collection<CodeQuarkusExtensions> extensions, String destinationZipFile) throws IOException {
        String downloadURL = getCodeQuarkusURL() + "/api/download?s=" +
                extensions.stream().map(x -> x.shortId).collect(Collectors.joining("."));
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(
                new URL(downloadURL).openStream());
             FileChannel fileChannel = new FileOutputStream(destinationZipFile).getChannel()) {
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
        return downloadURL;
    }

    public static File unzip(String zipFilePath, String destinationDir) throws InterruptedException, IOException {
        ProcessBuilder pb;
        if (isThisWindows) {
            pb = new ProcessBuilder("powershell", "-c", "Expand-Archive", "-Path", zipFilePath, "-DestinationPath", destinationDir, "-Force");
        } else {
            pb = new ProcessBuilder("unzip", "-o", zipFilePath, "-d", destinationDir);
        }
        Map<String, String> env = pb.environment();
        env.put("PATH", System.getenv("PATH"));
        pb.directory(new File(destinationDir));
        pb.redirectErrorStream(true);
        File unzipLog = new File(zipFilePath + ".log");
        unzipLog.delete();
        pb.redirectOutput(ProcessBuilder.Redirect.to(unzipLog));
        Process p = pb.start();
        p.waitFor(3, TimeUnit.MINUTES);
        return unzipLog;
    }


    public static List<String> getGeneratorCommand(String[] baseCommand, List<CodeQuarkusExtensions> extensions) {
        return getGeneratorCommand( baseCommand, (String[]) extensions.toArray());
    }

}
