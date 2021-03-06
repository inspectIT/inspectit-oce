package rocks.inspectit.ocelot.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

/**
 * Settings for connecting the configuration server to remote Git repositories.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RemoteConfigurationsSettings {

    /**
     * Whether remote Git repositories should be used for configuration management.
     */
    @Builder.Default
    private boolean enabled = false;

    /**
     * Whether the current live branch should be pushed during startup.
     */
    @Builder.Default
    private boolean pushAtStartup = true;

    /**
     * The remote Git repository which will be used to fetch workspace-configurations from.
     */
    @Valid
    private RemoteRepositorySettings sourceRepository;

    /**
     * The remote Git repository which will be used to push live-configurations to.
     */
    @Valid
    private RemoteRepositorySettings targetRepository;
}
