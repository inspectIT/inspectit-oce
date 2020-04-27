package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import lombok.extern.slf4j.Slf4j;
import rocks.inspectit.ocelot.file.accessor.AbstractFileAccessor;

import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
public abstract class AbstractWorkingDirectoryAccessor extends AbstractFileAccessor {

    public void createConfigurationDirectory(String directory) throws IOException {
        log.debug("Creating configuration directory: {}", directory);
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, directory);

        createDirectory(targetPath);
    }

    public void writeConfigurationFile(String file, String content) throws IOException {
        log.debug("Writing configuration file: {}", file);
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, file);

        writeFile(targetPath, content);
    }

    public void deleteConfiguration(String path) throws IOException {
        log.debug("Deleting configuration: {}", path);
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, path);

        if (Paths.get(path).normalize().toString().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete base directory: " + path);
        }

        delete(targetPath);
    }

    public void moveConfiguration(String source, String target) throws IOException {
        log.debug("Moving configuration: {} -> {}", source, target);
        String sourcePath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, source);
        String targetPath = verifyPath(CONFIGURATION_FILES_SUBFOLDER, target);

        move(sourcePath, targetPath);
    }

    public void writeAgentMappings(String content) throws IOException {
        log.debug("Writing agent mappings");
        writeFile(AGENT_MAPPINGS_FILE_NAME, content);
    }

    protected abstract void writeFile(String path, String content) throws IOException;

    protected abstract void createDirectory(String path) throws IOException;

    protected abstract void move(String sourcePath, String targetPath) throws IOException;

    protected abstract void delete(String path) throws IOException;
}
