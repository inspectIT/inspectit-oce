package rocks.inspectit.ocelot.core.instrumentation.actions.bound;

import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import rocks.inspectit.ocelot.bootstrap.instrumentation.IGenericAction;
import rocks.inspectit.ocelot.core.instrumentation.config.model.GenericActionConfig;
import rocks.inspectit.ocelot.core.instrumentation.injection.InjectedClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

abstract class AbstractDynamicBoundGenericAction extends BoundGenericAction {

    /**
     * A template containing the already assigned constant arguments for this generic action.
     * As the same {@link AbstractDynamicBoundGenericAction} instance could potentially be used by multiple threads,
     * this array needs to be copied before the dynamicAssignments can be performed.
     */
    private final Object[] argumentsTemplate;

    /**
     * An array containing (a) the index of the addition input to assign and (b) a function for defining the value.
     * The index corresponds to the index of the parameter in {@link GenericActionConfig#getAdditionalArgumentTypes()}.
     * Therefore the index corresponds to the position in the additionalArgumetns array with which the
     * {@link IGenericAction#execute(Object[], Object, Object, Throwable, Object[])} function is called.
     */
    private Pair<Integer, Function<ExecutionContext, Object>>[] dynamicAssignments;

    AbstractDynamicBoundGenericAction(String callName, GenericActionConfig actionConfig,
                                      InjectedClass<?> action, Map<String, Object> constantAssignments,
                                      Map<String, Function<ExecutionContext, Object>> dynamicAssignments) {
        super(callName, actionConfig, action);

        // the sorted additionalArgumentTypes map defines the number and the order of the additional input
        // parameters the generic action expects
        // therefore we can already reserve the exact amount of space needed for the argumentsTemplate
        int numArgs = actionConfig.getAdditionalArgumentTypes().size();
        argumentsTemplate = new Object[numArgs];

        List<Pair<Integer, Function<ExecutionContext, Object>>> dynamicAssignmentsWithIndices = new ArrayList<>();

        //we now loop over the additionalArgumentTypes map and remember the index of the corresponding parameter
        //If the parameter is defined through a constant assignment we simply place it in the argumentsTemplate at the
        //index of the parameter.
        //if the parameter is defined through a dynamic assignment we cannot directly store the value already in the template.
        //Instead we remember the index and the function used to perform the assignment in dynamicAssignments.

        int idx = 0;
        for (String argName : actionConfig.getAdditionalArgumentTypes().keySet()) {
            if (constantAssignments.containsKey(argName)) {
                argumentsTemplate[idx] = constantAssignments.get(argName);
            } else if (dynamicAssignments.containsKey(argName)) {
                dynamicAssignmentsWithIndices.add(Pair.of(idx, dynamicAssignments.get(argName)));
            } else {
                //should never occur as this is validated by config validations
                throw new RuntimeException("Unassigned argument!");
            }
            idx++;
        }
        this.dynamicAssignments = dynamicAssignmentsWithIndices.toArray(new Pair[0]);
    }

    Object[] buildAdditionalArguments(ExecutionContext context) {
        Object[] args = Arrays.copyOf(argumentsTemplate, argumentsTemplate.length);

        for (val assignment : dynamicAssignments) {
            args[assignment.getLeft()] = assignment.getRight().apply(context);
        }
        return args;
    }
}
