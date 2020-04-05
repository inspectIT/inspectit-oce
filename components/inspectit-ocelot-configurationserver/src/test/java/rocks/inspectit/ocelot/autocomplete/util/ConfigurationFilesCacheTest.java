package rocks.inspectit.ocelot.autocomplete.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.FileManager;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfigurationFilesCacheTest {

    @InjectMocks
    ConfigurationFilesCache configurationFilesCache;

    @Mock
    FileManager fileManager;

    @Nested
    public class GetAllPaths {
        @Test
        public void getYamlPaths() throws IOException {
            FileInfo mockFileInfo = Mockito.mock(FileInfo.class);
            Stream<String> streamA = Stream.of("path/a.yml");
            when(mockFileInfo.getAbsoluteFilePaths(any())).thenReturn(streamA);
            FileInfo mockFileInfo2 = Mockito.mock(FileInfo.class);
            Stream<String> streamB = Stream.of("path/b.yaml");
            when(mockFileInfo2.getAbsoluteFilePaths(any())).thenReturn(streamB);
            List<FileInfo> mockInfoList = Arrays.asList(mockFileInfo, mockFileInfo2);
            when(fileManager.getFilesInDirectory("", true)).thenReturn(mockInfoList);

            List<String> output = configurationFilesCache.getAllPaths();

            assertThat(output.size()).isEqualTo(2);
            assertThat(output.contains("path/a.yml")).isTrue();
            assertThat(output.contains("path/b.yaml")).isTrue();
        }
    }

    @Nested
    public class LoadFiles {
        @Test
        public void testYamlLoadingMap() throws IOException {
            String yamlContent1 = "i am a:\n        - test\n        - yaml";
            String yamlContent2 = "so:\n    am: i";
            when(fileManager.readFile(any())).thenReturn(yamlContent1, yamlContent2);
            FileInfo mockFileInfo1 = mock(FileInfo.class);
            when(mockFileInfo1.getAbsoluteFilePaths("")).thenReturn(Stream.of("a.yaml"));
            FileInfo mockFileInfo2 = mock(FileInfo.class);
            when(mockFileInfo2.getAbsoluteFilePaths("")).thenReturn(Stream.of("b.yaml"));
            when(fileManager.getFilesInDirectory("", true)).thenReturn(Arrays.asList(mockFileInfo1, mockFileInfo2));
            List<String> list = Arrays.asList("test", "yaml");
            LinkedHashMap<String, Object> firstElement = new LinkedHashMap<>();
            firstElement.put("i am a", list);
            LinkedHashMap<String, Object> secondElement = new LinkedHashMap<>();
            LinkedHashMap<String, Object> map2 = new LinkedHashMap<>();
            map2.put("am", "i");
            secondElement.put("so", map2);

            Collection<Object> beforeLoading = configurationFilesCache.getParsedConfigurationFiles();
            configurationFilesCache.loadFiles();
            Collection<Object> output = configurationFilesCache.getParsedConfigurationFiles();

            assertThat(output).isNotEqualTo(beforeLoading);
            assertThat(output.contains(firstElement)).isTrue();
            assertThat(output.contains(secondElement)).isTrue();
        }

        @Test
        public void exceptionOnLoading() throws IOException {
            configurationFilesCache.loadFiles();
            Collection<Object> before = configurationFilesCache.getParsedConfigurationFiles();
            when(fileManager.readFile(any())).thenThrow(new IOException());
            FileInfo mockFileInfo = Mockito.mock(FileInfo.class);
            Stream<String> streamA = Stream.of("wrong.path");
            when(mockFileInfo.getAbsoluteFilePaths(any())).thenReturn(streamA);
            List<FileInfo> mockInfoList = Collections.singletonList(mockFileInfo);
            when(fileManager.getFilesInDirectory("", true)).thenReturn(mockInfoList);

            configurationFilesCache.loadFiles();
            Collection<Object> output = configurationFilesCache.getParsedConfigurationFiles();

            assertThat(before).isEqualTo(output);
        }
    }

    @Nested
    public class GetParsedConfigurationFiles {
        @Test
        public void loadsYamlFile() throws IOException {
            FileInfo mockFileInfo = Mockito.mock(FileInfo.class);
            Stream<String> streamA = Stream.of("path/a.yaml");
            when(mockFileInfo.getAbsoluteFilePaths(any())).thenReturn(streamA);
            List<FileInfo> mockInfoList = Collections.singletonList(mockFileInfo);
            when(fileManager.getFilesInDirectory("", true)).thenReturn(mockInfoList);
            when(fileManager.readFile(any())).thenReturn("mock:");
            HashMap<String, Object> excpectedHashMap = new HashMap<>();
            excpectedHashMap.put("mock", null);

            configurationFilesCache.loadFiles();
            Collection<Object> output = configurationFilesCache.getParsedConfigurationFiles();

            assertThat(output).contains(excpectedHashMap);
        }

        @Test
        public void ignoresNonYamlFile() throws IOException {
            FileInfo mockFileInfo = Mockito.mock(FileInfo.class);
            Stream<String> streamA = Stream.of("path/a.xml");
            when(mockFileInfo.getAbsoluteFilePaths(any())).thenReturn(streamA);
            List<FileInfo> mockInfoList = Collections.singletonList(mockFileInfo);
            when(fileManager.getFilesInDirectory("", true)).thenReturn(mockInfoList);
            when(fileManager.readFile(any())).thenReturn("mock:");
            HashMap<String, Object> excpectedHashMap = new HashMap<>();
            excpectedHashMap.put("mock", null);

            configurationFilesCache.loadFiles();
            Collection<Object> output = configurationFilesCache.getParsedConfigurationFiles();

            assertThat(output).doesNotContain(excpectedHashMap);
        }
    }

    @Nested
    public class GetFiles {
        @Test
        public void loadFiles() throws IOException {
            FileInfo mockFileInfo = Mockito.mock(FileInfo.class);
            Stream<String> streamA = Stream.of("path/a.yml");
            when(mockFileInfo.getAbsoluteFilePaths(any())).thenReturn(streamA);
            FileInfo mockFileInfo2 = Mockito.mock(FileInfo.class);
            Stream<String> streamB = Stream.of("path/b.xml");
            when(mockFileInfo2.getAbsoluteFilePaths(any())).thenReturn(streamB);
            List<FileInfo> mockInfoList = Arrays.asList(mockFileInfo, mockFileInfo2);
            when(fileManager.getFilesInDirectory("", true)).thenReturn(mockInfoList);
            when(fileManager.readFile(any())).thenAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    String input = (String) args[0];
                    if (input.equals("path/a.yml")) {
                        return ("a");
                    }
                    if (input.equals("path/b.xml")) {
                        return ("b");
                    }
                    return "error";
                }
            });

            configurationFilesCache.loadFiles();
            HashMap<String, String> output = configurationFilesCache.getFiles();

            assertThat(output).containsKey("path/a.yml");
            assertThat(output).containsKey("path/b.xml");
            assertThat(output.get("path/a.yml")).isEqualTo("a");
            assertThat(output.get("path/b.xml")).isEqualTo("b");
            assertThat(output).doesNotContainValue("error");
        }
    }
}

