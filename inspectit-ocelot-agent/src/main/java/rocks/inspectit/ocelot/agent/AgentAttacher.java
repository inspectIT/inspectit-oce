package rocks.inspectit.ocelot.agent;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Class for attaching the agent to a running JVM using jattach.
 */
public class AgentAttacher {

    /**
     * Agent jar path.
     */
    private static final String AGENT_PATH = new File(AgentAttacher.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getAbsolutePath();

    /**
     * The OS name.
     */
    private static final String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);

    /**
     * Attaches the agent to the JVM with the given PID.
     *
     * @param pid             the PID of the JVM to attach the agent
     * @param agentProperties properties passed to the agent represented as a JSON string
     */
    public static void attach(int pid, String agentProperties) {
        System.out.println("Attaching inspectIT Ocelot agent to process " + pid);

        File jattachFile = null;
        try {
            jattachFile = exportJattach();
            System.out.println("Exported jattach to: " + jattachFile);

            attachAgent(jattachFile, pid, agentProperties);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jattachFile != null) {
                jattachFile.delete();
                jattachFile.getParentFile().delete();
            }
        }
    }

    /**
     * Attaches the agent to the JVM with the given PID using jattach.
     *
     * @param jattachFile     the jattach binary
     * @param pid             the PID of the JVM to attach the agent
     * @param agentProperties properties passed to the agent represented as a JSON string
     */
    private static void attachAgent(File jattachFile, int pid, String agentProperties) throws InterruptedException, IOException {
        List<String> commandList = new ArrayList<>(Arrays.asList(jattachFile.toString(), String.valueOf(pid), "load", "instrument", "false"));

        if (agentProperties != null) {
            // preventing excessive escaping on windows
            if (isWindows()) {
                agentProperties = agentProperties.replace("\"", "\\\"");
            }
            commandList.add(AGENT_PATH + "=" + agentProperties);
        } else {
            commandList.add(AGENT_PATH);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commandList)
                .directory(jattachFile.getParentFile())
                .redirectErrorStream(true)
                .inheritIO();

        System.out.println("Executing command: " + processBuilder.command());

        System.out.println("###################################");
        System.out.println("## JATTACH START ##################");
        System.out.println("# # # # # # # # # # # # # # # # # #");

        Process attachProcess = processBuilder.start();
        int exitVal = attachProcess.waitFor();

        System.out.println("# # # # # # # # # # # # # # # # # #");
        System.out.println("## JATTACH END ####################");
        System.out.println("###################################");

        if (exitVal == 0) {
            System.out.println("Agent successfully attached!");
        } else {
            System.out.println("Agent could not be attached!");
        }
    }

    /**
     * Returns the file name of the jattach binary. The name differs depending on the operating system.
     *
     * @return the file name of the jattach binary
     */
    private static String getJattachFileName() {
        if (isWindows()) {
            return "jattach.exe";
        } else if (isMacOS()) {
            return "jattach-macos";
        } else if (isAlpineLinux()) {
            return "jattach-alpine";
        } else if (isUnix()) {
            return "jattach";
        } else {
            throw new RuntimeException("Operating system could not be recognized.");
        }
    }

    /**
     * Returns whether the underlying operating system is Windows.
     */
    private static boolean isWindows() {
        return OS.contains("win");
    }

    /**
     * Returns whether the underlying operating system is MacOS.
     */
    private static boolean isMacOS() {
        return OS.contains("mac");
    }

    /**
     * Returns whether the underlying operating system is Linux.
     */
    private static boolean isUnix() {
        return OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0;
    }

    /**
     * Returns whether the underlying operating system is Alpine Linux.
     */
    private static boolean isAlpineLinux() {
        return isUnix() && new File("/etc/alpine-release").exists();
    }

    /**
     * Exports the jattach binary bundled with the agent to a temporary file for execution.
     *
     * @return the absolute file of the exported jattach binary
     */
    private static File exportJattach() throws IOException {
        String jattachName = getJattachFileName();
        InputStream stream = AgentAttacher.class.getResourceAsStream("/jattach/" + jattachName);

        Path tempDirecotry = Files.createTempDirectory("ocelot");
        Path targetPath = Paths.get(tempDirecotry.toString(), jattachName);

        Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);

        File jattachFile = targetPath.toFile();

        boolean executable = jattachFile.setExecutable(true);
        if (!executable) {
            throw new RuntimeException("Could not set executable flag to " + jattachFile + ". Please check permissions.");
        }

        return jattachFile;
    }
}
