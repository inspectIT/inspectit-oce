package rocks.inspectit.ocelot.core.plugins;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.asm.ClassReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.bootstrap.instrumentation.DoNotInstrumentMarker;
import rocks.inspectit.ocelot.config.model.InspectitConfig;
import rocks.inspectit.ocelot.config.model.plugins.PluginSettings;
import rocks.inspectit.ocelot.core.config.InspectitEnvironment;
import rocks.inspectit.ocelot.core.config.PropertySourcesChangedEvent;
import rocks.inspectit.ocelot.core.config.util.PropertyUtils;
import rocks.inspectit.ocelot.sdk.ConfigurablePlugin;
import rocks.inspectit.ocelot.sdk.OcelotPlugin;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This component scans the path specified by inspectit.plugins.path for Jar-files.
 * These files will then be scanned for classes having the {@link OcelotPlugin} annotation, which are then loaded.
 * This component is also responsible for keeping plugin configurations up-to-date.
 * If the configuration changes {@link ConfigurablePlugin#update(InspectitConfig, Object)} will be invoked.
 * <p>
 * This component only performs the scanning at startup time, meaning
 * that dynamically adding or removing plugins is currently not supported.
 */
@Component
@Slf4j
public class PluginLoader {

    /**
     * We add the combination of all default settings of loaded plugins as an additional property source to {@link InspectitEnvironment}.
     * This is the name of this property source.
     */
    private static final String PROPERTY_SOURCE_NAME = "Plugin Default Configurations";

    @Autowired
    private InspectitEnvironment env;

    /**
     * The list of loaded plugins.
     */
    @VisibleForTesting
    List<LoadedPlugin> plugins = new ArrayList<>();

    /**
     * Scans the path configured by {@link PluginSettings#getPath()} for Jars.
     * These jars are scannend for classes having the {@link OcelotPlugin} annotation.
     * The corresponding plugins are then loaded and initialized.
     */
    @PostConstruct
    private void scanAndLoadPlugins() {
        String directory = env.getCurrentConfig().getPlugins().getPath();
        if (!StringUtils.isBlank(directory)) {
            log.info("Scanning '{}' for plugins", directory);

            Properties defaultConfigurations = new Properties();

            try {
                Collection<File> pluginsJars = FileUtils.listFiles(new File(directory), new String[]{"jar"}, true);
                for (File jar : pluginsJars) {
                    try {
                        loadPluginJar(jar, defaultConfigurations);
                    } catch (Throwable t) {
                        log.error("Error loading plugins from jar '{}'", jar.toString(), t);
                    }
                }
            } catch (Exception e) {
                log.error("Error scanning for plugins", e);
            }
            env.updatePropertySources(ps ->
                    ps.addLast(new PropertiesPropertySource(PROPERTY_SOURCE_NAME, defaultConfigurations))
            );
            updatePlugins();
        }
    }

    @PreDestroy
    void destroy() {
        for (LoadedPlugin plugin : plugins) {
            try {
                plugin.destroy();
            } catch (Throwable t) {
                log.error("Destroying plugin {} threw Exception!", plugin.getName(), t);
            }
        }
    }

    @EventListener(PropertySourcesChangedEvent.class)
    @VisibleForTesting
    void updatePlugins() {
        for (LoadedPlugin plugin : plugins) {
            try {
                plugin.updateConfiguration(env);
            } catch (Throwable t) {
                log.error("updating plugin {} threw Exception!", plugin.getName(), t);
            }
        }
    }

    /**
     * Loads the given jar file into an isolated classloader.
     * All classes with the {@link OcelotPlugin} annotation are found
     * and {@link #initializePlugin(Class, Properties)} is invoked for each.
     *
     * @param pluginFile            the jar file to load
     * @param defaultConfigurations the Properties to place the default configurations in
     */
    private void loadPluginJar(File pluginFile, Properties defaultConfigurations) throws Exception {
        ClassLoader pluginLoader = new PluginClassLoader(pluginFile.toURI().toURL());
        List<Class> list = getAnnotatedClasses(pluginFile);
        list.stream()
                .filter(cl -> cl.getClassLoader() == pluginLoader)
                .forEach(cl -> initializePlugin(cl, defaultConfigurations));
    }

    /**
     * Returns all classes in a File with {@link OcelotPlugin} annotations.
     *
     * @param pluginFile The file in which the classes should be searched in.
     * @return A list containing all classes annotated with {@link OcelotPlugin} in the given file.
     */
    private List<Class> getAnnotatedClasses(File pluginFile) throws IOException, ClassNotFoundException {
        ArrayList<Class> list = new ArrayList<>();
        JarFile pluginJar = new JarFile(pluginFile);

        String pathToJar = pluginFile.getAbsolutePath();
        Enumeration<JarEntry> entries = pluginJar.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (isClass(entry)) {
                if (isOcelotPlugin(pluginJar.getInputStream(entry))) {
                    list.add(loadClassFromJarEntry(entry, pathToJar));
                }
            }
        }
        return list;
    }

    /**
     * Checks if a given InputStream contains an {@link OcelotPlugin} annotation.
     *
     * @param inputStream The InputStream which should be checked.
     * @return True if the InputStream is annotated an OcelotPlugin.
     */
    private boolean isOcelotPlugin(InputStream inputStream) throws IOException {
        OcelotPluginAnnotationVisitor annotationVisitor = new OcelotPluginAnnotationVisitor();
        new ClassReader(inputStream).accept(annotationVisitor, 0);
        return annotationVisitor.hasOcelotPluginAnnotation;
    }

    /**
     * Loads a class from a JarEntry Object.
     *
     * @param entry     The entry which should be loaded.
     * @param pathToJar The path to the jar the entry is contained in.
     * @return The class object of the given JarEntry. Returns null if the given entry is not a class.
     */
    private Class<?> loadClassFromJarEntry(JarEntry entry, String pathToJar) throws MalformedURLException, ClassNotFoundException {
        if (isClass(entry)) {
            String className = entry.getName().substring(0, entry.getName().length() - 6);
            className = className.replace('/', '.');
            URLClassLoader classLoader = new PluginClassLoader(new URL("jar:file:" + pathToJar + "!/"));
            return classLoader.loadClass(className);
        }
        return null;
    }

    /**
     * Checks if a given entry is a class file.
     *
     * @param entry The entry to check.
     * @return Returns true if the given entry is a class file.
     */
    private boolean isClass(JarEntry entry) {
        return !entry.isDirectory() && entry.getName().endsWith(".class");
    }

    /**
     * Instantiates a plugin from the given class.
     *
     * @param pluginClass           the plugins class
     * @param defaultConfigurations the output properties object to which default configurations of the given plugin are loaded.
     */
    @VisibleForTesting
    void initializePlugin(Class<?> pluginClass, Properties defaultConfigurations) {
        try {
            if (ConfigurablePlugin.class.isAssignableFrom(pluginClass)) {

                OcelotPlugin pluginInfo = pluginClass.getAnnotation(OcelotPlugin.class);

                try {
                    ConfigurablePlugin<?> plugin = (ConfigurablePlugin<?>) pluginClass.getConstructor().newInstance();
                    plugins.add(new LoadedPlugin(plugin, pluginInfo.value()));

                    String defaultConfigYml = pluginInfo.defaultConfig();
                    if (!StringUtils.isEmpty(defaultConfigYml)) {
                        try {
                            Properties result = PropertyUtils.readYamlFiles(new ClassPathResource(defaultConfigYml, pluginClass));
                            defaultConfigurations.putAll(result);
                        } catch (Exception e) {
                            log.error("Error loading default config file {} of plugin {}", defaultConfigYml, pluginClass.getName(), e);
                        }
                    }
                    log.info("Plugin '{}' loaded!", pluginInfo.value());
                } catch (NoSuchMethodException e) {
                    log.error("The plugin {} does not have a public default constructor!", pluginClass.getName());
                }

            } else {
                log.error("The plugin {} does not implement {}!", pluginClass.getName(), ConfigurablePlugin.class.getName());
            }
        } catch (Throwable t) {
            log.error("Error initializing plugin {}", pluginClass.getName());
        }
    }


    /**
     * Simple classloader which just is marked with the {@link DoNotInstrumentMarker}.
     */
    private static class PluginClassLoader extends URLClassLoader implements DoNotInstrumentMarker {

        public PluginClassLoader(URL jarFile) {
            super(new URL[]{jarFile}, PluginLoader.class.getClassLoader());
        }
    }
}
