package rocks.inspectit.ocelot.config.loaders;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * This class is used to load the default configs present in the resource folder.
 */
@Component
public class DefaultConfigFileLoader {
    /**
     * The encoding used to decode the content of the loaded files.
     */
    static final Charset ENCODING = StandardCharsets.UTF_8;

    /**
     * This String resembles the classpaths that are searched to get the default config files.
     */
    static final String CLASSPATH_REGEX = "classpath:rocks/inspectit/ocelot/config/default/**/*.yml";

    /**
     * This String resembles the classpath of the default config folder. It is used to get the full path of the default
     * config folder, which is removed from the retrieved files' paths.
     */
    static final String DEFAULT_FOLDER_PATH = "classpath:rocks/inspectit/ocelot/config/default";

    /**
     * This String stores the path to the default folder. It is used to remove this sequence of the paths of the loaded
     * files.
     */
    private String defaultFolderPath = "";

    /**
     * Default constructor that sets the defaultFolderPath when an instance of this object is created.
     */
    public DefaultConfigFileLoader() throws IOException {
        setDefaultFolderPath();
    }

    /**
     * This method sets the defaultFolder path.
     */
    public void setDefaultFolderPath() throws IOException {
        Resource[] resources = getRessources(DEFAULT_FOLDER_PATH);
        if (resources.length != 0) {
            defaultFolderPath = getPathFromResource(resources[0]);
        }
    }

    /**
     * This method loads all default config files present in the resource directory of the config project.
     * The files are returned in a map. The keys are the path of the file, and the values are the the file's content.
     * The path to the file is cleaned. The whole section leading to the /default folder is removed.
     *
     * @return A Map containing pairs of file paths and contents, both as String.
     */
    public HashMap<String, String> loadDefaultConfig() throws IOException {
        HashMap<String, String> configMap = new HashMap<>();
        Resource[] resources = getRessources(CLASSPATH_REGEX);
        for (Resource resource : resources) {
            configMap.put(cleanRessourcePath(readAndCleanResourcePath(resource)), readResourceContent(resource));
        }
        return configMap;
    }

    /**
     * This method takes a resource instance as parameter and returns it's content.
     *
     * @param resource The resource instance the content should be returned from.
     * @return The content of the resource.
     */
    private String readResourceContent(Resource resource) throws IOException {
        BufferedReader bf = getBufferedReader(resource);
        StringBuilder stringBuilder = new StringBuilder();
        String s = bf.readLine();
        while (s != null) {
            stringBuilder.append(s);
            stringBuilder.append(" ");
            s = bf.readLine();
        }
        return stringBuilder.toString();
    }

    /**
     * Takes a resource instance as parameter and returns it's cleaned path. The path is cleaned by removing the
     * part of the up to the /default folder.
     *
     * @param resource The resource the path should be returned from.
     * @return The cleaned path of the resource.
     */
    private String readAndCleanResourcePath(Resource resource) throws IOException {
        String resourcePath = getPathFromResource(resource);
        return cleanRessourcePath(resourcePath);
    }

    /**
     * Takes a String as parameter, cleans and returns it. In the cleaning process, the part of the path leading to
     * the /default folder, including /default is removed. e.G. /path/to/default/myDefaultSubfolder/myFile.yml would
     * be returned as /myDefaultSubfolder/myFile.yml.
     *
     * @param path The path which should be cleaned as String.
     * @return The cleaned String.
     */
    private String cleanRessourcePath(String path) throws IOException {
        String pathWithoutPathToConfig = path.replaceFirst(defaultFolderPath, "");
        return pathWithoutPathToConfig;

    }

    /**
     * Takes a path as parameter and returns the resource found in the path.
     *
     * @param path the path to the resource that should be loaded.
     * @return the loaded resource.
     */
    Resource[] getRessources(String path) throws IOException {
        return new PathMatchingResourcePatternResolver(DefaultConfigFileLoader.class.getClassLoader())
                .getResources(path);
    }

    /**
     * Takes a resource instance as parameter and returns an instance of BufferedReader. The BufferedReader is created
     * with the resource's InputStream and the encoding defined in the ENCODING variable.
     *
     * @param resource the resource which should be used to create the instance of BufferedReader
     * @return a BufferedReader instance with the resource's InputStream.
     */
    BufferedReader getBufferedReader(Resource resource) throws IOException {
        return new BufferedReader(new InputStreamReader(resource.getInputStream(), ENCODING));
    }

    /**
     * Takes a resource instance and returns the path of it's URL as String.
     *
     * @param resource the resource the path should be retrieved from.
     * @return the path of the resource's URL as String.
     */
    String getPathFromResource(Resource resource) throws IOException {
        return resource.getURL().getPath();
    }
}
