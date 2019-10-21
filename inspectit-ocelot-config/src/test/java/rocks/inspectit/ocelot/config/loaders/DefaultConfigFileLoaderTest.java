package rocks.inspectit.ocelot.config.loaders;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultConfigFileLoaderTest {

    @InjectMocks
    DefaultConfigFileLoader defaultConfigFileLoader;

    @Nested
    public class loadDefaultConfig {
        @Test
        void hasResources() throws IOException {
            Resource mockResource = mock(Resource.class);
            Resource[] mockResources = {mockResource};
            DefaultConfigFileLoader spyDefaultConfigFileLoader = spy(defaultConfigFileLoader);
            doReturn(mockResources).when(spyDefaultConfigFileLoader).getRessources(any());
            doReturn("prefix", "prefix/test1").when(spyDefaultConfigFileLoader).getPathFromResource(any());
            BufferedReader mockBufferedReader = mock(BufferedReader.class);
            when(mockBufferedReader.readLine()).thenReturn("testZeile1", "testZeile2", null);
            doReturn(mockBufferedReader).when(spyDefaultConfigFileLoader).getBufferedReader(any());
            spyDefaultConfigFileLoader.setDefaultFolderPath();
            HashMap<String, String> expected = new HashMap<>();
            expected.put("/test1", "testZeile1 testZeile2 ");

            HashMap<String, String> output = spyDefaultConfigFileLoader.loadDefaultConfig();

            assertThat(output).isEqualTo(expected);
            verify(spyDefaultConfigFileLoader, times(2)).getRessources(any());
            verify(spyDefaultConfigFileLoader, times(2)).getPathFromResource(any());
            verify(spyDefaultConfigFileLoader).getBufferedReader(any());
            verify(spyDefaultConfigFileLoader).loadDefaultConfig();
            verify(spyDefaultConfigFileLoader).setDefaultFolderPath();
            verify(mockBufferedReader, times(3)).readLine();
            verifyNoMoreInteractions(mockResource, spyDefaultConfigFileLoader, mockBufferedReader);
        }

        @Test
        void noResources() throws IOException {
            Resource[] mockResources = new Resource[0];
            DefaultConfigFileLoader spyDefaultConfigFileLoader = spy(defaultConfigFileLoader);
            doReturn(mockResources).when(spyDefaultConfigFileLoader).getRessources(any());
            BufferedReader mockBufferedReader = mock(BufferedReader.class);

            HashMap<String, String> output = spyDefaultConfigFileLoader.loadDefaultConfig();

            assertThat(output).isEqualTo(Collections.emptyMap());
            verify(spyDefaultConfigFileLoader).getRessources(any());
            verify(spyDefaultConfigFileLoader).loadDefaultConfig();
            verifyNoMoreInteractions(spyDefaultConfigFileLoader, mockBufferedReader);
        }
    }
}