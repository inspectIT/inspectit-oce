package rocks.inspectit.ocelot.rest.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rocks.inspectit.ocelot.IntegrationTestBase;
import rocks.inspectit.ocelot.file.FileInfo;
import rocks.inspectit.ocelot.file.versioning.model.WorkspaceDiff;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DirectoryControllerIntTest extends IntegrationTestBase {

    @Nested
    class ListContents {

        @Test
        public void emptyResponse() {
            ResponseEntity<FileInfo[]> result = authRest.getForEntity("/api/v1/directories/", FileInfo[].class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEmpty();
        }

        @Test
        public void validResponse() {
            // create test files
            authRest.exchange("/api/v1/files/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<FileInfo[]> result = authRest.getForEntity("/api/v1/directories/", FileInfo[].class);

            FileInfo[] resultBody = result.getBody();
            assertThat(resultBody)
                    .hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType, FileInfo::getChildren)
                    .contains(tuple("file.yml", FileInfo.Type.FILE, null));
        }


        @Test
        public void listLiveWorkspace() {
            authRest.exchange("/api/v1/files/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<FileInfo[]> result = authRest.getForEntity("/api/v1/directories/", FileInfo[].class);

            FileInfo[] resultBody = result.getBody();
            assertThat(resultBody)
                    .hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType, FileInfo::getChildren)
                    .contains(tuple("file.yml", FileInfo.Type.FILE, null));

            ResponseEntity<FileInfo[]> resultLive = authRest.getForEntity("/api/v1/directories?version=live", FileInfo[].class);

            FileInfo[] resultBodyLive = resultLive.getBody();
            assertThat(resultBodyLive)
                    .hasSize(0);
        }

        @Test
        public void listFileFromWorkspace() {
            authRest.exchange("/api/v1/files/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<FileInfo[]> result = authRest.getForEntity("/api/v1/directories/", FileInfo[].class);

            FileInfo[] resultBody = result.getBody();
            assertThat(resultBody)
                    .hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType, FileInfo::getChildren)
                    .contains(tuple("file.yml", FileInfo.Type.FILE, null));

            ResponseEntity<Optional> result2 = authRest.getForEntity("/api/v1/files/file.yml", Optional.class);

            Optional<String> resultBody2 = result2.getBody();
            assertThat(resultBody2.toString()).contains("content=");
        }

        @Test
        public void listFileFromLiveWorkspace() {

            authRest.exchange("/api/v1/files/file.yml", HttpMethod.PUT, null, Void.class);

            ResponseEntity<FileInfo[]> result = authRest.getForEntity("/api/v1/directories/", FileInfo[].class);

            FileInfo[] resultBody = result.getBody();
            assertThat(resultBody)
                    .hasSize(1)
                    .extracting(FileInfo::getName, FileInfo::getType, FileInfo::getChildren)
                    .contains(tuple("file.yml", FileInfo.Type.FILE, null));

            ResponseEntity<Optional> result1 = authRest.getForEntity("/api/v1/files/file.yml?version=live/", Optional.class);

            Optional<String> resultBodyLive = result1.getBody();
            assertThat(resultBodyLive.toString()).contains("status=INTERNAL_SERVER_ERROR");
        }
    }

    @Nested
    class CreateNewDirectory {

        @Test
        public void noDirectorySpecified() {
            ResponseEntity<Void> result = authRest.exchange("/api/v1/directories/", HttpMethod.PUT, null, Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        public void createDirectory() {
            ResponseEntity<Void> result = authRest.exchange("/api/v1/directories/new_dir", HttpMethod.PUT, null, Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

            Path new_dir = Paths.get(settings.getWorkingDirectory(), "files", "new_dir");
            assertThat(new_dir).exists();
        }
    }

    @Nested
    class DeleteDirectory {

        @Test
        public void noDirectorySpecified() {
            ResponseEntity<Void> result = authRest.exchange("/api/v1/directories/", HttpMethod.DELETE, null, Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        public void deleteDirectory() {
            // create test directory with files
            authRest.exchange("/api/v1/files/root/target_dir/file.yml", HttpMethod.PUT, null, Void.class);

            assertThat(Paths.get(settings.getWorkingDirectory(), "files", "root", "target_dir", "file.yml")).exists();

            ResponseEntity<Void> result = authRest.exchange("/api/v1/directories/root/target_dir", HttpMethod.DELETE, null, Void.class);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(Paths.get(settings.getWorkingDirectory(), "files", "root")).exists();
            assertThat(Paths.get(settings.getWorkingDirectory(), "files", "root", "target_dir")).doesNotExist();
        }
    }
}