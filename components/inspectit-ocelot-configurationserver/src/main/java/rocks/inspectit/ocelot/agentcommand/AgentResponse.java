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
public class AgentResponse {

        /**
         * The id of this command.
         */
        private UUID commandId;

        /**
         * The payload this command returned.
         */
        private List<Object> payload;

}
