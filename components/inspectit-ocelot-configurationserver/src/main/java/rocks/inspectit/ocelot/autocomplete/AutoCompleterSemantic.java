package rocks.inspectit.ocelot.autocomplete;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import rocks.inspectit.ocelot.file.FileManager;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Slf4j
@Component
public class AutoCompleterSemantic implements AutoCompleter {

    /**
     * Predicate to check if a given file path ends with .yml or .yaml
     */
    private static final Predicate<String> HAS_YAML_ENDING = filePath -> filePath.toLowerCase().endsWith(".yml") || filePath.toLowerCase().endsWith(".yaml");

    @Autowired
    private FileManager fileManager;

    /**
     * If the given path ends in scopes, this method returns the scopes a user could possibly use
     * This is done by searching all currently saved .yml or .yaml files for declaration of scopes and returning them
     *
     * @param camelCasePath the path to which suggestions should be made, parsed into camelCase literals
     * @return all scopes that could be found
     */
    @Override
    public List<String> getSuggestions(List<String> camelCasePath) {
        return collectKeys(camelCasePath);
    }

    /**
     * This method lodas a yaml or .yml file in any given path
     *
     * @param path path of the yaml to load
     * @return the file as Object
     * @throws IOException
     */
    private Object loadYaml(String path) throws IOException {
        Yaml yaml = new Yaml();
        String src = fileManager.readFile(path);
        Object loadedYaml = yaml.load(src);
        return loadedYaml;
    }

    /**
     * Searches in the current directory for files with .yml or .yaml ending. Returns all paths to those files as
     * List of the type string
     *
     * @return A list of all found paths to .yml or .yaml files
     */
    List<String> getAllPaths() {
        try {
            return fileManager.getFilesInDirectory(null, true).stream()
                    .flatMap(f -> f.getAbsoluteFilePaths(""))
                    .filter(HAS_YAML_ENDING)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Extracts all keys from a given map that can be found in a given path
     *
     * @param map     the map one wants to extract the keys from
     * @param mapPath all keys leading to the wanted
     * @return
     */
    private Set extractKeys(HashMap map, List<String> mapPath) {
        HashMap currentMap = map;
        for (String path : mapPath) {
            if (!(currentMap.get(path) instanceof HashMap)) {
                return Collections.<String>emptySet();
            }
            currentMap = (HashMap) currentMap.get(path);
        }
        return currentMap.keySet();
    }

    /**
     * Extracts all keys from the hashmaps in a given list of objects
     *
     * @param values the list of objects where the keys should be extrated from
     * @param keys   the list to which the keys should be added
     * @param path   the path in which the keys should be searched
     */
    private void mergeObjectKeys(ArrayList<Object> values, ArrayList<String> keys, List<String> path) {
        for (Object o : values) {
            if (o instanceof HashMap) {
                addMapKeys((HashMap) o, keys, path);
            }
        }
    }

    private void addMapKeys(HashMap map, ArrayList<String> keys, List<String> path) {
        Arrays.stream(extractKeys(map, path).toArray()).filter(ps -> !keys.contains(ps)).forEach(ps -> keys.add((String) ps));
    }

    /**
     * Collects all keys present in a given path in all .yml and .yaml files in the directory
     *
     * @param givenPath the path one wants to extract the keys from
     * @return the found keys
     */
    private ArrayList<String> collectKeys(List<String> givenPath) {
        List<String> paths = getAllPaths();
        ArrayList<String> scopes = new ArrayList<>();
        for (String path : paths) {
            try {
                Object loadedObj = loadYaml(path);
                if (loadedObj instanceof HashMap) {
                    addMapKeys((HashMap) loadedObj, scopes, givenPath);
                }
                if (loadedObj instanceof ArrayList) {
                    mergeObjectKeys((ArrayList<Object>) loadedObj, scopes, givenPath);
                }
            } catch (IOException e) {
                log.warn("Error reading file with path: " + path + " Error message: " + e.getMessage());
            }
        }
        return scopes;
    }
}
