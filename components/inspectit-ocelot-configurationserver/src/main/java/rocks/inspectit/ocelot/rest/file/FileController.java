package rocks.inspectit.ocelot.rest.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rocks.inspectit.ocelot.file.FileData;
import rocks.inspectit.ocelot.rest.util.RequestUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Controller for managing the configurations.
 */
@RestController
public class FileController extends FileBaseController {

    @Autowired
    private ObjectMapper objectMapper;

    @ApiOperation(value = "Write a file", notes = "Creates or overwrites a file with the provided text content")
    @ApiImplicitParam(name = "Path", type = "string", value = "The part of the url after /files/ defines the path to the file to write.")
    @PutMapping(value = "files/**")
    public void writeConfigurationFile(HttpServletRequest request,
                                       @ApiParam("If true, the request body is not parsed as json and is instead written directly to the result file.") @RequestParam(defaultValue = "false") boolean raw,
                                       @ApiParam(value = "The content to write, either raw or a json",
                                               examples = @Example(value = {
                                                       @ExampleProperty(mediaType = "application/json", value = "{ 'content' : 'This is the file content' }"),
                                                       @ExampleProperty(mediaType = "text/plain", value = "This is the file content")
                                               })
                                       )

                                       @RequestBody(required = false) String content) throws IOException, GitAPIException {
        String path = RequestUtil.getRequestSubPath(request);
        if (raw || content == null) {
            configurationFileManager.writeFile(path, content == null ? "" : content);
        } else {
            FileData data = objectMapper.readValue(content, FileData.class);
            configurationFileManager.writeFile(path, data.getContent());
        }
    }

    @ApiOperation(value = "Write a file", notes = "Creates or overwrites a file with the provided text content")
    @ApiImplicitParam(name = "Path", type = "string", value = "The part of the url after /files/ defines the path to the file to write.")
    @PutMapping(value = "specialfiles/**")
    public void writeSpecialFile(HttpServletRequest request,
                                 @ApiParam("If true, the request body is not parsed as json and is instead written directly to the result file.") @RequestParam(defaultValue = "false") boolean raw,
                                 @ApiParam(value = "The content to write, either raw or a json",
                                         examples = @Example(value = {
                                                 @ExampleProperty(mediaType = "application/json", value = "{ 'content' : 'This is the file content' }"),
                                                 @ExampleProperty(mediaType = "text/plain", value = "This is the file content")
                                         })
                                 )

                                 @RequestBody(required = false) String content) throws IOException, GitAPIException {
        String path = RequestUtil.getRequestSubPath(request);
        if (raw || content == null) {
            configurationFileManager.writeFile(path, content == null ? "" : content);
        } else {
            FileData data = objectMapper.readValue(content, FileData.class);
            configurationFileManager.writeFile(path, data.getContent());
        }
    }

    @ApiOperation(value = "Read a file", notes = "Returns the contents of the given file.")
    @ApiImplicitParam(name = "Path", type = "string", value = "The part of the url after /files/ defines the path to the file to read.")
    @ApiResponse(code = 200,
            message = "Ok",
            examples = @Example(value = {
                    @ExampleProperty(mediaType = "application/json", value = "{ 'content' : 'This is the file content' }"),
                    @ExampleProperty(mediaType = "text/plain", value = "This is the file content")
            }))
    @GetMapping(value = "files/**")
    public Object readConfigurationFile(HttpServletRequest request,
                                        @ApiParam("If true, the response body is not formatted as json and is instead the plain text content of the file.")
                                        @RequestParam(defaultValue = "false") boolean raw) throws IOException {
        String path = RequestUtil.getRequestSubPath(request);
        String content = configurationFileManager.readFile(path, false);
        if (raw) {
            return content;
        } else {
            return FileData.builder().content(content).build();
        }
    }

    @ApiOperation(value = "Read a file", notes = "Returns the contents of the given file.")
    @ApiImplicitParam(name = "Path", type = "string", value = "The part of the url after /files/ defines the path to the file to read.")
    @ApiResponse(code = 200,
            message = "Ok",
            examples = @Example(value = {
                    @ExampleProperty(mediaType = "application/json", value = "{ 'content' : 'This is the file content' }"),
                    @ExampleProperty(mediaType = "text/plain", value = "This is the file content")
            }))
    @GetMapping(value = "specialfiles/**")
    public Object readSpecialFile(HttpServletRequest request,
                                  @ApiParam("If true, the response body is not formatted as json and is instead the plain text content of the file.")
                                  @RequestParam(defaultValue = "false") boolean raw) throws IOException {
        String path = RequestUtil.getRequestSubPath(request);
        String content = configurationFileManager.readFile(path, true);
        if (raw) {
            return content;
        } else {
            return FileData.builder().content(content).build();
        }
    }

    @ApiOperation(value = "Delete a file", notes = "Deletes the given file")
    @ApiImplicitParam(name = "Path", type = "string", value = "The part of the url after /files/ defines the path to the file to delete.")
    @DeleteMapping(value = "files/**")
    public void deleteFile(HttpServletRequest request) throws IOException, GitAPIException {
        String path = RequestUtil.getRequestSubPath(request);
        configurationFileManager.deleteFile(path);
    }
}
