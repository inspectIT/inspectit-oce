package rocks.inspectit.ocelot.file.manager.directory;

import org.apache.commons.io.FileExistsException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.StringUtils;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.manager.directory.WorkingDirectoryManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkingDirectoryManagerTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");
    private static final Path fmRoot = rootWorkDir.resolve("root");
    private static final String FILES_SUBFOLDER = "files";
    static final Charset ENCODING = StandardCharsets.UTF_8;

    @InjectMocks
    private WorkingDirectoryManager wdm;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    InspectitServerSettings config;

    @BeforeAll
    private static void setup() throws Exception {
        Files.createDirectories(rootWorkDir);
    }

    @AfterAll
    private static void clean() throws Exception {
        deleteDirectory(rootWorkDir);
    }

    @BeforeEach
    private void setupFileManager() throws Exception {
        when(config.getWorkingDirectory()).thenReturn(fmRoot.toString());
        wdm.workingDirRoot = Paths.get(config.getWorkingDirectory()).resolve(FILES_SUBFOLDER).toAbsolutePath().normalize();
    }

    @AfterEach
    private void cleanFileManager() throws Exception {
        deleteDirectory(fmRoot);
    }

    private static void setupTestFiles(boolean agentmapping, String... paths) {
        try {
            for (String path : paths) {
                if (!agentmapping) {
                    if (path.contains("/")) {
                        String[] pathArray = path.split("/");
                        Files.createDirectories(fmRoot.resolve("files/configuration").resolve(pathArray[0]));
                        String newPaths = "files/configuration" + "/" + pathArray[0];
                        Files.createFile(fmRoot.resolve(newPaths).resolve(pathArray[1]));
                    } else {
                        Files.createDirectories(fmRoot.resolve("files/configuration"));
                        Files.createFile(fmRoot.resolve("files/configuration").resolve(path));
                    }
                } else {
                    if (path.contains("/")) {
                        String[] pathArray = path.split("/");
                        Files.createDirectories(fmRoot.resolve(pathArray[0]));
                        String newPaths = pathArray[0];
                        Files.createFile(fmRoot.resolve(newPaths).resolve(pathArray[1]));
                    } else {
                        Files.createFile(fmRoot.resolve(path));
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private static void deleteDirectory(Path path) throws IOException {
        List<Path> files = Files.walk(path).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(files);
        for (Path f : files) {
            Files.delete(f);
        }
    }

    private static void createFileWithContent(String path, String content) throws IOException {
        File f;
        if (path.contains("/")) {
            String paths[] = path.split("/");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < paths.length - 1; i++) {
                builder.append(paths[i]).append("/");
            }
            Files.createDirectories(fmRoot.resolve("files").resolve(builder.toString()));
            String finalPath = "files" + "/" + builder + paths[paths.length - 1];
            f = new File(String.valueOf(fmRoot.resolve(finalPath)));

        } else {
            Files.createDirectories(fmRoot.resolve("files"));
            f = new File(String.valueOf(fmRoot.resolve("files").resolve(path).toAbsolutePath()));
        }
        FileWriter fw = new FileWriter(f);
        fw.write(content);
        fw.flush();
        fw.close();
    }

    private static void setupTestFiles(String... paths) {
        try {
            for (String path : paths) {
                if (!path.contains("=")) {
                    Files.createDirectories(fmRoot.resolve(FILES_SUBFOLDER).resolve(path));
                } else {
                    String[] splitted = path.split("=");
                    Path file = fmRoot.resolve(FILES_SUBFOLDER).resolve(splitted[0]);
                    Files.createDirectories(file.getParent());
                    String content = splitted.length > 1 ? splitted[1] : "";
                    Files.write(file, content.getBytes(ENCODING));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String readFile(String path) throws IOException {
        Path file = fmRoot.resolve(FILES_SUBFOLDER).resolve(path);
        return new String(Files.readAllBytes(file), ENCODING);
    }

    public synchronized List<FileInfo> getFilesInDirectory(String path, boolean recursive) throws IOException {
        Path dir;
        if (StringUtils.isEmpty(path)) {
            dir = fmRoot;
        } else {
            dir = fmRoot.resolve(path);
        }
        try (Stream<Path> files = Files.list(dir)) {
            List<FileInfo> result = new ArrayList<>();
            for (Path child : files.collect(Collectors.toList())) {
                boolean isDirectory = Files.isDirectory(child);
                FileInfo.FileInfoBuilder builder = FileInfo.builder()
                        .name(child.getFileName().toString())
                        .type(isDirectory ? FileInfo.Type.DIRECTORY : FileInfo.Type.FILE);
                if (isDirectory && recursive) {
                    builder.children(getFilesInDirectory(fmRoot.relativize(child.normalize())
                            .toString()
                            .replace(child.getFileSystem().getSeparator(), "/"), true));
                }
                result.add(builder.build());
            }
            return result;
        }
    }


    @Nested
    public class ReadFile {
        @Test
        void readTopLevelFile() throws IOException {
            createFileWithContent("testFile", "testContent");

            assertThat(wdm.readFile("testFile")).isEqualTo("testContent");
        }

        @Test
        void readSubfolderFile() throws IOException {
            createFileWithContent("directory/testFile", "testContent");

            assertThat(wdm.readFile("directory/testFile")).isEqualTo("testContent");
        }
    }

    @Nested
    public class WriteFile {
        @Test
        void writeTopLevelFile() throws IOException {
            wdm.writeFile("configuration/name", "content");

            assertThat(readFile("configuration/name")).isEqualTo("content");
        }

        @Test
        void writeFileAndSubfolder() throws IOException {
            wdm.writeFile("configuration/dir/name", "content");

            assertThat(readFile("configuration/dir/name")).isEqualTo("content");
        }
    }

    @Nested
    class Move {

        @Test
        void renameFile() throws Exception {
            setupTestFiles("top", "foo=foo");

            wdm.move("foo", "bar");

            assertThat(wdm.listFiles("", true))
                    .hasSize(2)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("top");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.FILE);
                        assertThat(f.getName()).isEqualTo("bar");
                    });
            assertThat(readFile("bar")).isEqualTo("foo");
        }

        @Test
        void renameFolder() throws Exception {
            setupTestFiles("top", "foo/sub/something=text");

            wdm.move("foo/sub", "foo/bar");

            assertThat(wdm.listFiles("", true))
                    .hasSize(2)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("top");
                        assertThat(f.getChildren()).isEmpty();
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("foo");
                        assertThat(f.getChildren())
                                .hasSize(1)
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("bar");
                                    assertThat(f2.getChildren())
                                            .hasSize(1)
                                            .anySatisfy((f3) -> {
                                                assertThat(f3.getType()).isEqualTo(FileInfo.Type.FILE);
                                                assertThat(f3.getName()).isEqualTo("something");
                                            });
                                });
                    });
            assertThat(readFile("foo/bar/something")).isEqualTo("text");
        }

        @Test
        void moveDirectoryWithContents() throws Exception {
            setupTestFiles("top", "foo/sub/something=text", "foo/sub/a/b");

            wdm.move("foo/sub", "sub");

            assertThat(wdm.listFiles("", true))
                    .hasSize(3)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("top");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("foo");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("sub");
                        assertThat(f.getChildren())
                                .hasSize(2)
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("a");
                                    assertThat(f2.getChildren())
                                            .hasSize(1)
                                            .anySatisfy((f3) -> {
                                                assertThat(f3.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                                assertThat(f3.getName()).isEqualTo("b");
                                                assertThat(f3.getChildren()).isEmpty();
                                            });
                                })
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.FILE);
                                    assertThat(f2.getName()).isEqualTo("something");
                                });
                    });
            assertThat(readFile("sub/something")).isEqualTo("text");
        }

        @Test
        void moveFile() throws Exception {
            setupTestFiles("file=");

            wdm.move("file", "a/b/file");

            assertThat(wdm.listFiles("", true))
                    .hasSize(1)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("a");
                        assertThat(f.getChildren())
                                .hasSize(1)
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("b");
                                    assertThat(f2.getChildren())
                                            .hasSize(1)
                                            .anySatisfy((f3) -> {
                                                assertThat(f3.getType()).isEqualTo(FileInfo.Type.FILE);
                                                assertThat(f3.getName()).isEqualTo("file");
                                            });
                                });
                    });
            assertThat(readFile("a/b/file")).isEqualTo("");
        }

        @Test
        void moveNonExistingFile() throws Exception {
            setupTestFiles("file=");

            assertThatThrownBy(() -> wdm.move("someFile", "anotherFile"))
                    .isInstanceOf(FileNotFoundException.class);
        }

        @Test
        void moveOntoExistingFile() throws Exception {
            setupTestFiles("file=", "someFile=");

            assertThatThrownBy(() -> wdm.move("someFile", "file"))
                    .isInstanceOf(FileExistsException.class);
        }

        @Test
        void verifyDirOutsideWorkdirNotMoveable() {
            setupTestFiles("top");

            assertThatThrownBy(() -> wdm.move(null, "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("/", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("./", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("../myfile", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("top/../../myfile", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("../../myfile", "sub"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void verifyCannotMoveOutsideOfWorkDir() {
            setupTestFiles("top");

            assertThatThrownBy(() -> wdm.move("top", null))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("top", ""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("top", "/"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("top", "./"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("top", "../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("top", "top/../../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.move("top", "../../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }


    @Nested
    class DeleteFile {

        @Test
        void deleteFileInRootDirect() throws Exception {
            setupTestFiles("topA=foo", "topB");

            wdm.deleteFile("topA");

            assertThat(getFilesInDirectory("", true))
                    .noneSatisfy((f) ->
                            assertThat(f.getName()).isEqualTo("topA")
                    );
        }

        @Test
        void deleteFileInRootIndirect() throws Exception {
            setupTestFiles("topA=foo", "topB");

            wdm.deleteFile("topB/.././topA");

            assertThat(getFilesInDirectory("", true))
                    .noneSatisfy((f) ->
                            assertThat(f.getName()).isEqualTo("topA")
                    );
        }

        @Test
        void deleteEmptyFile() throws Exception {
            setupTestFiles("topA/subA/myFile=", "topA/subB");

            wdm.deleteFile("topA/subA/myFile");

            List<FileInfo> helpMe = getFilesInDirectory("", true);
            assertThat(getFilesInDirectory("files/", true))
                    .hasSize(1)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topA");
                        assertThat(f.getChildren())
                                .hasSize(2)
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("subA");
                                    assertThat(f2.getChildren()).isEmpty();
                                })
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("subB");
                                    assertThat(f2.getChildren()).isEmpty();
                                });
                    });
        }

        @Test
        void deleteNonExistingFile() {
            setupTestFiles("topA/subA", "topB");

            assertThatThrownBy(() -> wdm.deleteFile("topC"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void deleteDirectory() {
            setupTestFiles("topA", "topB");

            assertThatThrownBy(() -> wdm.deleteFile("topA"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void verifyDirOutsideWorkdirNotDeletable() {
            setupTestFiles("top");

            assertThatThrownBy(() -> wdm.deleteFile(null))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.deleteFile(""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.deleteFile("./"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.deleteFile("../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.deleteFile("top/../../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.deleteFile("../../myfile"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    class CreateDirectory {

        @Test
        void createDirInRootDirect() throws Exception {
            setupTestFiles("topA", "topB");

            wdm.createDirectory("myDir");

            assertThat(getFilesInDirectory("files/", true))
                    .hasSize(3)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("myDir");
                    });
        }

        @Test
        void createDirInRootIndirect() throws Exception {
            setupTestFiles("topA", "topB");

            wdm.createDirectory("topB/.././myDir");

            assertThat(getFilesInDirectory("files/", true))
                    .hasSize(3)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("myDir");
                    });
        }

        @Test
        void createDirInSubFolder() throws Exception {
            setupTestFiles("topA", "topB");

            wdm.createDirectory("topA/subA/subB");

            assertThat(getFilesInDirectory("files/", true))
                    .hasSize(2)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topA");
                        assertThat(f.getChildren())
                                .hasSize(1)
                                .anySatisfy((f2) -> {
                                    assertThat(f2.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                    assertThat(f2.getName()).isEqualTo("subA");
                                    assertThat(f2.getChildren())
                                            .hasSize(1)
                                            .anySatisfy((f3) -> {
                                                assertThat(f3.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                                                assertThat(f3.getName()).isEqualTo("subB");
                                                assertThat(f3.getChildren()).isEmpty();
                                            });
                                });
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    });
        }

        @Test
        void createDirOnExistingFile() {
            setupTestFiles("topA=content");

            assertThatThrownBy(() -> wdm.createDirectory("topA"))
                    .isInstanceOf(IOException.class);
        }

        @Test
        void createDirBeneathExistingFile() {
            setupTestFiles("topA=content");

            assertThatThrownBy(() -> wdm.createDirectory("topA/subDir"))
                    .isInstanceOf(IOException.class);
        }

        @Test
        void verifyDirOutsideWorkdirNotCreateable() {
            setupTestFiles("top");

            assertThatThrownBy(() -> wdm.createDirectory(null))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.createDirectory(""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.createDirectory("./"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.createDirectory("../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.createDirectory("top/../../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.createDirectory("../../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        void deleteDirInRootDirect() throws Exception {
            setupTestFiles("topA", "topB");

            wdm.deleteDirectory("topA");

            assertThat(getFilesInDirectory("files/", true))
                    .hasSize(1)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    });
        }

        @Test
        void deleteDirInRootIndirect() throws Exception {
            setupTestFiles("topA", "topB");

            wdm.deleteDirectory("topB/../topA");

            assertThat(getFilesInDirectory("files/", true))
                    .hasSize(1)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    });
        }

        @Test
        void deleteDirInSubFolderWithContents() throws Exception {
            setupTestFiles("topA/subA/subB/somedir", "topA/subA/somefile=foo", "topB");

            wdm.deleteDirectory("topA/subA");

            assertThat(getFilesInDirectory("files/", true))
                    .hasSize(2)
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topA");
                    })
                    .anySatisfy((f) -> {
                        assertThat(f.getType()).isEqualTo(FileInfo.Type.DIRECTORY);
                        assertThat(f.getName()).isEqualTo("topB");
                    });
        }

        @Test
        void deleteNonExistingDir() {
            setupTestFiles("topA/subA", "topB");

            assertThatThrownBy(() -> wdm.deleteDirectory("topC"))
                    .isInstanceOf(NotDirectoryException.class);
        }

        @Test
        void deleteFile() {
            setupTestFiles("topA=i_am_a_file", "topB");

            assertThatThrownBy(() -> wdm.deleteDirectory("topA"))
                    .isInstanceOf(NotDirectoryException.class);
        }

        @Test
        void verifyDirOutsideWorkdirNotDeleteable() {
            setupTestFiles("top");

            assertThatThrownBy(() -> wdm.deleteDirectory(null))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.deleteDirectory(""))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.deleteDirectory("./"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.deleteDirectory("../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.deleteDirectory("top/../../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> wdm.deleteDirectory("../../mydir"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}
