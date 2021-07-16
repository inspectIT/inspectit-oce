package rocks.inspectit.ocelot.file.versioning;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
import rocks.inspectit.ocelot.config.model.InspectitServerSettings;
import rocks.inspectit.ocelot.config.model.RemoteConfigurationsSettings;
import rocks.inspectit.ocelot.config.model.RemoteRepositorySettings;
import rocks.inspectit.ocelot.config.model.RemoteRepositorySettings.AuthenticationType;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Encapsulating the logic for interacting with the remote configuration repository.
 */
@Slf4j
@AllArgsConstructor
public class RemoteConfigurationManager {

    /**
     * The current server settings.
     */
    private final InspectitServerSettings settings;

    /**
     * The Git instance of the working directory.
     */
    private final Git git;

    /**
     * Updating the remote ref of the configuration repository in the current working directory Git. The remote ref is
     * created or, in case it already exists, updated.
     */
    public void updateRemoteRefs() throws GitAPIException {

        RemoteRepositorySettings sourceRepository = settings.getRemoteConfigurations().getSourceRepository();
        if (sourceRepository != null) {
            updateRemoteRef(sourceRepository);
        }

        RemoteRepositorySettings targetRepository = settings.getRemoteConfigurations().getTargetRepository();
        if (targetRepository != null) {
            updateRemoteRef(targetRepository);
        }
    }

    /**
     * Adds or updates a remote ref for the given {@link RemoteRepositorySettings}.
     *
     * @param repositorySettings the settings representing the ref to add
     */
    private void updateRemoteRef(RemoteRepositorySettings repositorySettings) throws GitAPIException {
        if (!hasConfigurationRemote(repositorySettings)) {
            log.info("Remote ref '{}' for remote configurations does not exists in the local Git, so it will be added.", repositorySettings
                    .getRemoteName());
            git.remoteAdd()
                    .setName(repositorySettings.getRemoteName())
                    .setUri(repositorySettings.getGitRepositoryUri())
                    .call();
        } else {
            log.debug("Remote ref '{}' for remote configurations exists in the local Git and will be updated.", repositorySettings
                    .getRemoteName());
            git.remoteSetUrl()
                    .setRemoteName(repositorySettings.getRemoteName())
                    .setRemoteUri(repositorySettings.getGitRepositoryUri())
                    .call();
        }
    }

    /**
     * @return Returns whether the remote ref for the configuration remote does already exist.
     */
    private boolean hasConfigurationRemote(RemoteRepositorySettings repositorySettings) {
        try {
            List<RemoteConfig> remotes = git.remoteList().call();
            return remotes.stream().anyMatch(remote -> remote.getName().equals(repositorySettings.getRemoteName()));
        } catch (GitAPIException e) {
            return false;
        }
    }

    /**
     * Pushes a specific local branch to the configured configuration remote ref using the specified branch name.
     *
     * @param localBranch      the local branch to push
     * @param targetRepository the settings for the repository to push to
     */
    public void pushBranch(Branch localBranch, RemoteRepositorySettings targetRepository) throws GitAPIException {
        if (targetRepository == null) {
            throw new IllegalArgumentException("The repository settings must not be null.");
        }

        RefSpec refSpec = new RefSpec(localBranch.getBranchName() + ":refs/heads/" + targetRepository.getBranchName());

        log.info("Pushing to remote '{}' [{}] with refspec '{}'. Using force-push: {}", targetRepository.getRemoteName(), targetRepository
                .getGitRepositoryUri(), refSpec.toString(), targetRepository.isUseForcePush());

        PushCommand pushCommand = git.push()
                .setRemote(targetRepository.getRemoteName())
                .setRefSpecs(refSpec)
                .setForce(targetRepository.isUseForcePush());

        if (targetRepository.getAuthenticationType() == AuthenticationType.PASSWORD) {
            authenticatePassword(pushCommand, targetRepository);
        } else if (targetRepository.getAuthenticationType() == AuthenticationType.PPK) {
            authenticatePpk(pushCommand, targetRepository);
        }

        Iterable<PushResult> pushResults = pushCommand.call();
        PushResult pushResult = pushResults.iterator().next();

        if (pushResult == null) {
            log.warn("Pushing of local branch '{}' may have failed. No push-result available.", localBranch);
        } else {
            RemoteRefUpdate remoteUpdate = pushResult.getRemoteUpdate("refs/heads/" + targetRepository.getBranchName());
            RemoteRefUpdate.Status status = remoteUpdate.getStatus();

            if (status == RemoteRefUpdate.Status.OK || status == RemoteRefUpdate.Status.UP_TO_DATE) {
                log.info("Pushing to remote repository '{}' was successful: {}", targetRepository.getRemoteName(), status);
            } else {
                log.error("Pushing to remote repository '{}' has been failed: {}", targetRepository.getRemoteName(), status);
            }
        }
    }

    /**
     * Injects a {@link CredentialsProvider} for executing a user-password authentication. This is used for HTTP(s)-remotes.
     *
     * @param pushCommand      the push command to authenticate
     * @param targetRepository the settings for the repository to push to
     */
    private void authenticatePassword(PushCommand pushCommand, RemoteRepositorySettings targetRepository) {
        String username = targetRepository.getUsername();
        String password = targetRepository.getPassword();
        UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
        pushCommand.setCredentialsProvider(credentialsProvider);
    }

    /**
     * Injects a session factory for creating SSH sessions. This allows the usage of private keys for connection authentication.
     *
     * @param pushCommand      the push command to authenticate
     * @param targetRepository the settings for the repository to push to
     */
    private void authenticatePpk(PushCommand pushCommand, RemoteRepositorySettings targetRepository) {
        SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);

                String privateKeyFile = targetRepository.getPrivateKeyFile();
                if (StringUtils.isNotBlank(privateKeyFile)) {
                    defaultJSch.addIdentity(privateKeyFile);
                }

                return defaultJSch;
            }

            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
            }
        };

        pushCommand.setTransportConfigCallback(transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        });
    }
}
