package rocks.inspectit.ocelot.agentcommand;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Represents a command to be executed by an inspecIT agent.
 */
@Data
@Getter
@AllArgsConstructor
public class AgentCommand {

    /**
     * The type of command of this instance.
     */
    private AgentCommandType commandType;

    /**
     * The id of the agent this command is meant for.
     */
    private String agentId;

    /**
     * The id of this command.
     */
    private UUID commandId;

    /**
     * Additional parameters for this command.
     */
    private List<Object> parameters;
}
