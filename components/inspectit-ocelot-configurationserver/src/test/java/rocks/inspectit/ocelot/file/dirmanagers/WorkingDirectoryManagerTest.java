package rocks.inspectit.ocelot.file.dirmanagers;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkingDirectoryManagerTest {

    private static final Path rootWorkDir = Paths.get("temp_test_workdir");
    private static final Path fmRoot = rootWorkDir.resolve("root");

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
        wdm.init();
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
                        Files.createDirectories(fmRoot.resolve(WorkingDirectoryManager.FILES_SUBFOLDER).resolve(pathArray[0]));
                        String newPaths = WorkingDirectoryManager.FILES_SUBFOLDER + "/" + pathArray[0];
                        Files.createFile(fmRoot.resolve(newPaths).resolve(pathArray[1]));
                    } else {
                        Files.createFile(fmRoot.resolve(WorkingDirectoryManager.FILES_SUBFOLDER).resolve(path));
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
            Files.createDirectories(fmRoot.resolve(WorkingDirectoryManager.FILES_SUBFOLDER).resolve(paths[0]));
            String finalPath = WorkingDirectoryManager.FILES_SUBFOLDER + "/" + paths[0];
            f = new File(String.valueOf(fmRoot.resolve(finalPath).resolve(paths[1])));

        } else {
            f = new File(String.valueOf(fmRoot.resolve(WorkingDirectoryManager.FILES_SUBFOLDER).resolve(path)));
        }
        FileWriter fw = new FileWriter(f);
        fw.write(content);
        fw.flush();
        fw.close();
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

            assertThat(wdm.readFile("name")).isEqualTo("content");
        }

        @Test
        void writeFileAndSubfolder() throws IOException {
            wdm.writeFile("configuration/dir/name", "content");

            assertThat(wdm.readFile("dir/name")).isEqualTo("content");
        }


    }

    @Nested
    public class ListFiles {
        @Test
        void listFilesTopLevel() throws IOException {
            setupTestFiles(false, "a", "b", "c");
            List<String> output = Arrays.asList("configuration/a", "configuration/b", "configuration/c");

            assertThat(wdm.listFiles("")).isEqualTo(output);
        }

        @Test
        void listFilesSubFolder() throws IOException {
            setupTestFiles(false, "directory/a");
            List<String> output = Arrays.asList("configuration/directory/a");

            assertThat(wdm.listFiles("")).isEqualTo(output);
        }

    }

    @Nested
    public class ReadAgentMappingFile {
        @Test
        void AgentMappingsContentIsDefined() throws IOException {
            createFileWithContent("../agent_mappings.yaml", "test");

            assertThat(wdm.readAgentMappingFile()).isEqualTo("test");
        }
    }

    @Nested
    public class WriteAgentMappingFile {
        @Test
        void initialWriting() throws IOException {
            wdm.writeAgentMappingFile("test");

            assertThat(wdm.readAgentMappingFile()).isEqualTo("test");
        }

        @Test
        void editContent() throws IOException {
            createFileWithContent("../agent_mappings.yaml", "test");
            wdm.writeAgentMappingFile("i do actually work!");

            assertThat(wdm.readAgentMappingFile()).isEqualTo("i do actually work!");
        }
    }
}
