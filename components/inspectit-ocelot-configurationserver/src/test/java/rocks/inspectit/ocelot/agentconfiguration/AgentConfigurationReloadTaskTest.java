package rocks.inspectit.ocelot.agentconfiguration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;
import rocks.inspectit.ocelot.mappings.model.AgentMapping;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentConfigurationReloadTaskTest {

    @InjectMocks
    AgentConfigurationReloadTask reloadTask;

    @Mock
    FileManager fileManager;

    @Nested
    class LoadConfigForMapping {

        @Test
        void noSourcesSpecified() throws IOException {
            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .build());

            assertThat(result).isEmpty();
        }


        @Test
        void nonExistingSourcesSpecified() throws IOException {
            doReturn(false).when(fileManager).exists("a.yml");
            doReturn(false).when(fileManager).exists("some/folder");

            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("a.yml")
                            .source("/some/folder")
                            .build());

            assertThat(result).isEmpty();
        }


        @Test
        void nonYamlIgnored() throws IOException {
            doReturn(true).when(fileManager).exists(any());
            doReturn(false).when(fileManager).isDirectory(any());
            doReturn("").when(fileManager).readSpecialFile(any(), anyBoolean());

            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("a.yml")
                            .source("b.YmL")
                            .source("c.yaml")
                            .source("d.txt")
                            .build());

            assertThat(result).isEmpty();
            verify(fileManager).readSpecialFile("a.yml", true);
            verify(fileManager).readSpecialFile("b.YmL", true);
            verify(fileManager).readSpecialFile("c.yaml", true);

            verify(fileManager, never()).readSpecialFile("d.txt", true);
        }


        @Test
        void leadingSlashesInSourcesRemoved() throws IOException {
            doReturn(false).when(fileManager).exists("a.yml");

            lenient().doThrow(new RuntimeException()).when(fileManager).exists(startsWith("/"));

            reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("/a.yml")
                            .build());

            verify(fileManager).exists(eq("a.yml"));
        }


        @Test
        void priorityRespected() throws IOException {

            doReturn(true).when(fileManager).exists(any());

            doReturn(true).when(fileManager).isDirectory("folder");
            doReturn(false).when(fileManager).isDirectory("z.yml");

            doReturn(Arrays.asList(
                    FileInfo.builder()
                            .type(FileInfo.Type.FILE)
                            .name("b.yml")
                            .build(),
                    FileInfo.builder()
                            .type(FileInfo.Type.FILE)
                            .name("a.yml")
                            .build(),
                    FileInfo.builder()
                            .type(FileInfo.Type.FILE)
                            .name("somethingelse")
                            .build()

            )).when(fileManager).listSpecialFiles("folder", true, true);

            doReturn("{ val1: z}").when(fileManager).readSpecialFile("z.yml", true);
            doReturn("{ val1: a, val2: a}").when(fileManager).readSpecialFile("folder/a.yml", true);
            doReturn("{ val1: b, val2: b, val3: b}").when(fileManager).readSpecialFile("folder/b.yml", true);


            String result = reloadTask.loadConfigForMapping(
                    AgentMapping.builder()
                            .source("/z.yml")
                            .source("/folder")
                            .build());


            assertThat(result).isEqualTo("{val1: z, val2: a, val3: b}\n");
            verify(fileManager, never()).readSpecialFile("folder/somethingelse", false);
        }
    }
}
