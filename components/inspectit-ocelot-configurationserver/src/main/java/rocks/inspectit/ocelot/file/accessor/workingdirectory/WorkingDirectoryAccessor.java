package rocks.inspectit.ocelot.file.accessor.workingdirectory;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.file.FileChangedEvent;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileInfoVisitor;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Concrete implementation to access and modify files in the server's working directory.
 */
@Slf4j
public class WorkingDirectoryAccessor extends AbstractWorkingDirectoryAccessor {

    /**
     * Event publisher to trigger events when files are being modified.
     */
    private ApplicationEventPublisher eventPublisher;

    private ReadWriteLock workingDirectoryLock;
    /**
     * The base path of the working directory.
     */
    private Path workingDirectory;

    public WorkingDirectoryAccessor(ReadWriteLock workingDirectoryLock, Path workingDirectory, ApplicationEventPublisher eventPublisher) {
        this.workingDirectoryLock = workingDirectoryLock;
        this.workingDirectory = workingDirectory;
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
    protected byte[] readFile(String path) throws IOException {
        workingDirectoryLock.readLock().lock();
        try {
            Path targetPath = resolve(path);

            if (!Files.exists(targetPath)) {
                throw new FileNotFoundException("File '" + path + "' does not exist.");
            }

            if (Files.isDirectory(targetPath)) {
                throw new IllegalArgumentException("The specified '" + path + "' is not a file but directory.");
            }

            return Files.readAllBytes(targetPath);
        } finally {
            workingDirectoryLock.readLock().unlock();
        }
    }

    @Override
    protected List<FileInfo> listFiles(String path) {
        workingDirectoryLock.readLock().lock();
        try {
            Path targetPath = resolve(path);

            if (!Files.exists(targetPath)) {
                return Collections.emptyList();
            }

            FileInfoVisitor fileInfoVisitor = new FileInfoVisitor();

            Files.walkFileTree(targetPath, fileInfoVisitor);

            return fileInfoVisitor.getFileInfos();
        } catch (IOException e) {
            log.error("Exception while listing files in path '{}'.", path, e);
            return Collections.emptyList();
        } finally {
            workingDirectoryLock.readLock().unlock();
        }
    }

    @Override
    protected void createDirectory(String path) throws IOException {
        workingDirectoryLock.writeLock().lock();
        try {
            Path targetDirectory = resolve(path);

            if (Files.exists(targetDirectory)) {
                throw new FileAlreadyExistsException("Directory already exists: " + targetDirectory);
            }

            Files.createDirectories(targetDirectory);

            fireFileChangeEvent();
        } finally {
            workingDirectoryLock.writeLock().unlock();
        }
    }

    @Override
    protected void writeFile(String path, String content) throws IOException {
        workingDirectoryLock.writeLock().lock();
        try {
            Path targetFile = resolve(path);

            if (Files.exists(targetFile) && Files.isDirectory(targetFile)) {
                throw new IOException("Cannot write file because target is already a directory: " + targetFile);
            }

            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, content.getBytes(FILE_ENCODING));

            fireFileChangeEvent();
        } finally {
            workingDirectoryLock.writeLock().unlock();
        }
    }

    @Override
    protected void move(String sourcePath, String targetPath) throws IOException {
        workingDirectoryLock.writeLock().lock();
        try {
            Path source = resolve(sourcePath);
            Path target = resolve(targetPath);

            FileUtils.forceMkdir(target.getParent().toFile());

            if (Files.isDirectory(source)) {
                FileUtils.moveDirectory(source.toFile(), target.toFile());
            } else {
                FileUtils.moveFile(source.toFile(), target.toFile());
            }

            fireFileChangeEvent();
        } finally {
            workingDirectoryLock.writeLock().unlock();
        }
    }

    @Override
    protected void delete(String path) throws IOException {
        workingDirectoryLock.writeLock().lock();
        try {
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
        } finally {
            workingDirectoryLock.writeLock().unlock();
        }
    }

    @Override
    protected boolean exists(String path) {
        workingDirectoryLock.readLock().lock();
        try {
            Path targetPath = resolve(path);
            return Files.exists(targetPath);
        } finally {
            workingDirectoryLock.readLock().unlock();
        }
    }

    @Override
    protected boolean isDirectory(String path) {
        workingDirectoryLock.readLock().lock();
        try {
            Path targetPath = resolve(path);
            return Files.isDirectory(targetPath);
        } finally {
            workingDirectoryLock.readLock().unlock();
        }
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
