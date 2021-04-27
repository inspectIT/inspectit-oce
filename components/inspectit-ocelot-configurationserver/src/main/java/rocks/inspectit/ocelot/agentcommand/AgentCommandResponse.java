package rocks.inspectit.ocelot.agentcommand;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * Represents the Response of an agent after it recieved an {@link AgentCommand}.
 */
@Data
@Getter
@AllArgsConstructor
public class AgentCommandResponse {

    private String id;

    private AgentCommandType agentCommandType;

    private Object payload;

}
