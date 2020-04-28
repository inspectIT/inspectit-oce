package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.FileChangedEvent;
import rocks.inspectit.ocelot.file.FileInfo;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Concrete implementation to access and modify files in the server's working directory.
 */
@Slf4j
@Component
public class WorkingDirectoryAccessor extends AbstractWorkingDirectoryAccessor {

    /**
     * Event publisher to trigger events when files are being modified.
     */
    private ApplicationEventPublisher eventPublisher;

    /**
     * The base path of the working directory.
     */
    private Path workingDirectory;

    @Autowired
    public WorkingDirectoryAccessor(InspectitServerSettings config, ApplicationEventPublisher eventPublisher) {
        this.workingDirectory = Paths.get(config.getWorkingDirectory()).toAbsolutePath().normalize();
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    private void init() throws IOException {
        Files.createDirectories(workingDirectory);
    }

    /**
     * Fires a new {@link FileChangedEvent}.
     */
    private void fireFileChangeEvent() {
        FileChangedEvent event = new FileChangedEvent(this);
        eventPublisher.publishEvent(event);
    }

    /**
     * Resolve the given path in relation to the current working directory.
     *
     * @param path the relative path
     * @return {@link Path} representing the given path string
     */
    private Path resolve(String path) {
        if (StringUtils.isEmpty(path)) {
            return workingDirectory;
        } else {
            return workingDirectory.resolve(path).normalize();
        }
    }

    @Override
    protected synchronized Optional<byte[]> readFile(String path) {
        Path targetPath = resolve(path);

        if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Files.readAllBytes(targetPath));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    protected synchronized List<FileInfo> listFiles(String path) {
        Path targetPath = resolve(path);

        if (!Files.exists(targetPath)) {
            return Collections.emptyList();
        }

        try (Stream<Path> files = Files.list(targetPath)) {
            List<FileInfo> result = new ArrayList<>();

            files.forEach(childPath -> {
                boolean isDirectory = Files.isDirectory(childPath);
                String fileName = childPath.getFileName().toString();

                FileInfo.FileInfoBuilder builder = FileInfo.builder()
                        .name(fileName)
                        .type(isDirectory ? FileInfo.Type.DIRECTORY : FileInfo.Type.FILE);

                if (isDirectory) {
                    String nestedPath = path + File.separator + fileName;
                    builder.children(listFiles(nestedPath));
                }

                result.add(builder.build());
            });

            return result;
        } catch (IOException e) {
            log.error("Exception while listing files in path '" + path + "'.", e);
            return Collections.emptyList();
        }
    }

    @Override
    protected synchronized void createDirectory(String path) throws IOException {
        Path targetDirectory = resolve(path);

        if (Files.exists(targetDirectory)) {
            throw new FileAlreadyExistsException("Directory already exists: " + targetDirectory);
        }

        Files.createDirectories(targetDirectory);

        fireFileChangeEvent();
    }

    @Override
    protected synchronized void writeFile(String path, String content) throws IOException {
        Path targetFile = resolve(path);

        if (Files.exists(targetFile) && Files.isDirectory(targetFile)) {
            throw new IOException("Cannot write file because target is already a directory: " + targetFile);
        }

        FileUtils.forceMkdir(targetFile.getParent().toFile());
        Files.write(targetFile, content.getBytes(FILE_ENCODING));

        fireFileChangeEvent();
    }

    @Override
    protected synchronized void move(String sourcePath, String targetPath) throws IOException {
        Path source = resolve(sourcePath);
        Path target = resolve(targetPath);

        FileUtils.forceMkdir(target.getParent().toFile());

        if (Files.isDirectory(source)) {
            FileUtils.moveDirectory(source.toFile(), target.toFile());
        } else {
            FileUtils.moveFile(source.toFile(), target.toFile());
        }

        fireFileChangeEvent();
    }

    @Override
    protected synchronized void delete(String path) throws IOException {
        Path targetPath = resolve(path);

        if (!Files.exists(targetPath)) {
            throw new FileNotFoundException("Path cannot be deleted because it does not exist: " + targetPath);
        } else if (Files.isDirectory(targetPath)) {
            FileUtils.deleteDirectory(targetPath.toFile());
        } else if (Files.isRegularFile(targetPath)) {
            Files.delete(targetPath);
        } else {
            throw new AccessDeniedException("'" + targetPath + "' could not be deleted.");
        }

        fireFileChangeEvent();
    }

    @Override
    protected boolean exists(String path) {
        Path targetPath = resolve(path);
        return Files.exists(targetPath);
    }

    @Override
    protected boolean isDirectory(String path) {
        Path targetPath = resolve(path);
        return Files.isDirectory(targetPath);
    }

    @Override
    protected String verifyPath(String relativeBasePath, String relativePath) {
        Path path = Paths.get(relativePath);

        if (path.isAbsolute()) {
            throw new IllegalArgumentException("Path must be relative: " + path);
        }

        Path basePath = workingDirectory.resolve(relativeBasePath);
        Path resolvedPath = basePath.resolve(path).normalize();

        if (!resolvedPath.startsWith(basePath)) {
            throw new IllegalArgumentException("User path escapes the base path: " + path);
        }

        return resolvedPath.toString();
    }
}
