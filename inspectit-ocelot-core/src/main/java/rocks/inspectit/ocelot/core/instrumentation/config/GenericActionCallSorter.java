package rocks.inspectit.ocelot.core.instrumentation.config;

import com.google.common.annotations.VisibleForTesting;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import rocks.inspectit.ocelot.core.config.model.instrumentation.actions.ActionCallSettings;
import rocks.inspectit.ocelot.core.instrumentation.config.model.ActionCallConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class encapsulates the logic how action calls are ordered.
 * The approach is that first a dependency graph is generated.
 * In this dependency graph the nodes are the data-keys and the action calls writing them,
 * the edges indicate a dependency between data keys and the corresponding action calls.
 * The edges are direct and an edge from data_A to data_B reads as
 * "The action writing data_B must be executed before the action of data_A"
 */
@Component
public class GenericActionCallSorter {

    /**
     * Orders the given set of action calls based on their inter-dependencies.
     *
     * @param callCollection the calls to sort
     * @return the sorted calls, meaning that the actions should be executed in the order in which they appear in the list
     * @throws CyclicDataDependencyException if the action calls have a cyclic dependency and therefore cannot be ordered
     */
    public List<ActionCallConfig> orderActionCalls(Collection<ActionCallConfig> callCollection) throws CyclicDataDependencyException {

        Map<String, ActionCallConfig> calls =
                callCollection.stream().collect(Collectors.toMap(ActionCallConfig::getName, c -> c));

        Set<String> dataKeys = calls.keySet();
        Map<String, Set<String>> dependencyGraph = new HashMap<>();
        calls.forEach((dataKey, actionCall) -> collectDependencies(dataKey, actionCall, dependencyGraph));

        //filter out data keys which are not written
        val filteredDependencyGraph = dependencyGraph.entrySet().stream()
                .filter(e -> dataKeys.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return getInTopologicalOrder(filteredDependencyGraph).stream()
                .map(calls::get)
                .collect(Collectors.toList());
    }

    private void collectDependencies(String dataKey, ActionCallConfig actionCall, Map<String, Set<String>> dependencyGraph) {
        ActionCallSettings callSettings = actionCall.getCallSettings();

        //this data action uses the following data keys as input arguments, therefore depends on them
        val dataInputs = callSettings.getDataInput().values();
        Set<String> executeAfter = new HashSet<>(dataInputs);

        //the data referenced in conditions also counts as implicit dependencies
        addIfNotBlank(callSettings.getOnlyIfFalse(), executeAfter);
        addIfNotBlank(callSettings.getOnlyIfTrue(), executeAfter);
        addIfNotBlank(callSettings.getOnlyIfNull(), executeAfter);
        addIfNotBlank(callSettings.getOnlyIfNotNull(), executeAfter);

        //data action calls can explicitly define that they want to be executed before other data is written
        Set<String> executeBefore = callSettings.getBefore().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        //we want the data key in the graph even if it has no dependencies
        dependencyGraph.computeIfAbsent(dataKey, (x) -> new HashSet<>());
        executeAfter.stream()
                //implicit after-dependencies are overridden by explicit before dependencies
                //e.g. if a call uses data_xy as input and also defines before: {data_xy: true},
                // the "after"-dependency to data_xy does not count
                .filter(data -> !executeBefore.contains(data))
                .forEach(afterData ->
                        dependencyGraph.get(dataKey).add(afterData));
        executeBefore
                .forEach(beforeData ->
                        dependencyGraph.computeIfAbsent(beforeData, (x) -> new HashSet<>())
                                .add(dataKey));
    }

    private static void addIfNotBlank(String value, Collection<? super String> sink) {
        if (!StringUtils.isEmpty(value)) {
            sink.add(value);
        }
    }

    /**
     * Topologically sorts the given data keys using a depth-first search.
     * Topological order means that the dependencies of a data appear prior to the data itself.
     *
     * @param dependencyGraph a map mapping each data key to the data keys he depends on. This map defines the the dependency graph.
     *                        The map is allowed to contain dependencies to data keys which are not part of dataKeys, those are simply ignored.
     * @return a sorted list containing all data keys
     * @throws CyclicDataDependencyException if there is a cyclic dependency, topological sorting is not possible
     */
    @VisibleForTesting
    List<String> getInTopologicalOrder(Map<String, Set<String>> dependencyGraph) throws CyclicDataDependencyException {
        StringStack result = new StringStack();
        //we remember which nodes we visited via a "stack-trace" to detect dependency cycles
        StringStack stackTrace = new StringStack();

        for (String dataKey : dependencyGraph.keySet()) {
            putInTopologicalOrder(result, dataKey, dependencyGraph, stackTrace);
        }
        return result.getList();
    }

    private void putInTopologicalOrder(StringStack result, String current, Map<String, Set<String>> dependencyGraph, StringStack stackTrace) throws CyclicDataDependencyException {

        if (result.contains(current)) {
            return; //this key has already been processed
        }

        stackTrace.push(current);
        for (String dependency : dependencyGraph.get(current)) {
            //ignore dependencies to itself
            //ignore dependencies to data which is not part of the graph
            if (!dependency.equals(current) && dependencyGraph.containsKey(dependency)) {
                if (!stackTrace.contains(dependency)) {
                    putInTopologicalOrder(result, dependency, dependencyGraph, stackTrace);
                } else {
                    ArrayList<String> stackTraceList = stackTrace.getList();
                    int idx = stackTraceList.indexOf(dependency);
                    throw new CyclicDataDependencyException(stackTraceList.subList(idx, stackTraceList.size()));
                }
            }
        }
        result.push(current);
        stackTrace.removeTop();
    }

    static class StringStack {
        @Getter
        private ArrayList<String> list = new ArrayList<>();

        private HashSet<String> set = new HashSet<>();

        void push(String str) {
            list.add(str);
            set.add(str);
        }

        void removeTop() {
            String str = list.remove(list.size() - 1);
            set.remove(str);
        }

        boolean contains(String str) {
            return set.contains(str);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class CyclicDataDependencyException extends Exception {
        /**
         * A list of data keys representing the dependency cycle.
         */
        private List<String> dependencyCycle;
    }
}
