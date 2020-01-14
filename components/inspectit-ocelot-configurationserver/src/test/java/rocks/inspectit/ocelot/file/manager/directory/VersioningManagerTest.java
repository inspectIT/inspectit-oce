package rocks.inspectit.ocelot.file.manager.directory;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VersioningManagerTest {

    @InjectMocks
    VersioningManager versionController;

    @Mock
    private Git git;

    @Mock
    private Repository repo;

    @Mock
    private GitAuthor author;

    @Nested
    public class Commit {

        @Test
        void testCommit() throws GitAPIException {
            AddCommand mockAddCommand = mock(AddCommand.class, Answers.RETURNS_SELF);
            when(git.add()).thenReturn(mockAddCommand);
            CommitCommand mockCommitCommand = mock(CommitCommand.class, Answers.RETURNS_SELF);
            when(git.commit()).thenReturn(mockCommitCommand);
            when(author.getName()).thenReturn("test");
            when(author.getMail()).thenReturn("test");

            versionController.commit();

            verify(git).add();
            verify(mockAddCommand).addFilepattern(".");
            verify(mockAddCommand).call();
            verify(git).commit();
            verify(mockCommitCommand).setAll(true);
            verify(mockCommitCommand).setMessage(any());
            verify(mockCommitCommand).setAuthor("test", "test");
            verify(mockCommitCommand).call();
            verify(author).getName();
            verify(author).getMail();
            verifyNoMoreInteractions(mockAddCommand, mockCommitCommand, author);
        }

        @Test
        void throwsException() throws GitAPIException {
            AddCommand mockAddCommand = mock(AddCommand.class, Answers.RETURNS_SELF);
            when(git.add()).thenReturn(mockAddCommand);
            CommitCommand mockCommitCommand = mock(CommitCommand.class, Answers.RETURNS_SELF);
            when(git.commit()).thenReturn(mockCommitCommand);
            when(mockCommitCommand.call()).thenThrow(WrongRepositoryStateException.class);
            when(author.getName()).thenReturn("test");
            when(author.getMail()).thenReturn("test");

            assertThatExceptionOfType(GitAPIException.class)
                    .isThrownBy(() -> versionController.commit());

            verify(git).add();
            verify(mockAddCommand).addFilepattern(".");
            verify(mockAddCommand).call();
            verify(git).commit();
            verify(mockCommitCommand).setAll(true);
            verify(mockCommitCommand).setMessage(any());
            verify(mockCommitCommand).setAuthor("test", "test");
            verify(mockCommitCommand).call();
            verify(author).getName();
            verify(author).getMail();
            verifyNoMoreInteractions(mockAddCommand, mockCommitCommand, git, author);
        }
    }

    @Nested
    public class CommitFile {

        @Test
        void singleFileCommit() throws GitAPIException {
            AddCommand mockAddCommand = mock(AddCommand.class, Answers.RETURNS_SELF);
            when(git.add()).thenReturn(mockAddCommand);
            CommitCommand mockCommitCommand = mock(CommitCommand.class, Answers.RETURNS_SELF);
            when(git.commit()).thenReturn(mockCommitCommand);
            when(mockCommitCommand.setOnly(any())).thenReturn(mockCommitCommand);
            VersioningManager mockedController = spy(versionController);
            when(author.getName()).thenReturn("test");
            when(author.getMail()).thenReturn("test");

            mockedController.commitFile("configuration/a");

            verify(mockAddCommand).addFilepattern(".");
            verify(mockAddCommand).call();
            verify(git).commit();
            verify(mockCommitCommand).setMessage(any());
            verify(mockCommitCommand).setAuthor("test", "test");
            verify(mockCommitCommand).call();
            verify(author).getName();
            verify(author).getMail();
            verifyNoMoreInteractions(mockAddCommand, mockCommitCommand, git, author);
        }
    }

    @Nested
    public class GetAllCommits {

        @Test
        void containsCommits() throws IOException, GitAPIException {
            ObjectId mockObjectId = mock(ObjectId.class);
            when(repo.resolve("refs/heads/master")).thenReturn(mockObjectId);
            LogCommand mockLogCommand = mock(LogCommand.class);
            when(git.log()).thenReturn(mockLogCommand);
            when(mockLogCommand.add(mockObjectId)).thenReturn(mockLogCommand);
            RevCommit revCommit1 = mock(RevCommit.class);
            RevCommit revCommit2 = mock(RevCommit.class);
            RevCommit revCommit3 = mock(RevCommit.class);
            List<RevCommit> revCommits = Arrays.asList(revCommit1, revCommit2, revCommit3);
            when(mockLogCommand.call()).thenReturn(revCommits);

            List<ObjectId> output = versionController.getAllCommits();

            assertThat(output).isEqualTo(revCommits);
            verify(repo).resolve("refs/heads/master");
            verify(git).log();
            verifyNoMoreInteractions(repo, git, revCommit1, revCommit2, revCommit3, mockLogCommand, mockObjectId);
        }

        @Test
        void noCommits() throws IOException, GitAPIException {
            when(repo.resolve("refs/heads/master")).thenReturn(null);

            List<ObjectId> output = versionController.getAllCommits();

            assertThat(output).isEqualTo(Collections.EMPTY_LIST);
            verify(repo).resolve("refs/heads/master");
            verifyNoMoreInteractions(repo, git);

        }
    }

    @Nested
    public class GetCommitsOfFile {
        @Test
        void fileExists() throws IOException, GitAPIException {
            VersioningManager spyController = spy(versionController);
            ObjectId objectId = mock(ObjectId.class);
            List<ObjectId> objectIds = Arrays.asList(objectId);
            doReturn(objectIds).when(spyController).getAllCommits();
            doReturn(true).when(spyController).commitContainsPath(any(), any());

            List<ObjectId> output = spyController.getCommitsOfFile("test");

            assertThat(output).isEqualTo(objectIds);
            verify(spyController).getCommitsOfFile("test");
            verify(spyController).commitContainsPath(any(), any());
            verifyNoMoreInteractions(spyController, git, repo);
        }

        @Test
        void fileExistsNot() throws IOException, GitAPIException {
            VersioningManager spyController = spy(versionController);
            ObjectId mockObjectId = mock(ObjectId.class);
            List<ObjectId> objectIds = Arrays.asList(mockObjectId);
            doReturn(objectIds).when(spyController).getAllCommits();
            doReturn(false).when(spyController).commitContainsPath(any(), any());

            List<ObjectId> output = spyController.getCommitsOfFile("test");

            assertThat(output).isEqualTo(Collections.emptyList());
            verify(spyController).getAllCommits();
            verify(spyController).commitContainsPath(any(), any());
            verify(spyController).getCommitsOfFile("test");
            verifyNoMoreInteractions(spyController, git, repo, mockObjectId);
        }

        @Test
        void noCommits() throws IOException, GitAPIException {
            VersioningManager spyController = spy(versionController);
            List<ObjectId> objectIds = Collections.emptyList();
            doReturn(objectIds).when(spyController).getAllCommits();

            List<ObjectId> output = spyController.getCommitsOfFile("test");

            assertThat(output).isEqualTo(Collections.emptyList());
            verify(spyController).getAllCommits();
            verify(spyController).getCommitsOfFile("test");
            verifyNoMoreInteractions(spyController, git, repo);
        }
    }

    @Nested
    public class CommitContainsPath {
        @Test
        void commitContainsPath() throws IOException, GitAPIException {
            ObjectId mockObjectId = mock(ObjectId.class);
            LogCommand mockLogCommand = mock(LogCommand.class);
            when(git.log()).thenReturn(mockLogCommand);
            when(git.getRepository()).thenReturn(repo);
            when(repo.resolve(Constants.HEAD)).thenReturn(mockObjectId);
            when(mockLogCommand.add(any())).thenReturn(mockLogCommand);
            when(mockLogCommand.addPath(any())).thenReturn(mockLogCommand);
            RevCommit mockRevCommit = mock(RevCommit.class);
            List<RevCommit> logCommandCommits = Arrays.asList(mockRevCommit);
            doReturn(logCommandCommits).when(mockLogCommand).call();
            VersioningManager spyController = spy(versionController);

            boolean output = spyController.commitContainsPath("test", mockObjectId);

            assertThat(output).isEqualTo(true);
            verify(git).log();
            verify(git).getRepository();
            verify(repo).resolve(Constants.HEAD);
            verify(mockLogCommand).add(any());
            verify(mockLogCommand).addPath(any());
            verify(mockLogCommand).call();
            verify(spyController).commitContainsPath(any(), any());
            verifyNoMoreInteractions(git, repo, mockLogCommand, spyController);
        }

        @Test
        void commitContainsPathNot() throws IOException, GitAPIException {
            ObjectId mockObjectId = mock(ObjectId.class);
            ObjectId mockObjectId2 = ObjectId.fromString("f03171d4bd5f6813d0647f225f29dd680885ac82");
            LogCommand mockLogCommand = mock(LogCommand.class);
            when(git.log()).thenReturn(mockLogCommand);
            when(git.getRepository()).thenReturn(repo);
            when(repo.resolve(Constants.HEAD)).thenReturn(mockObjectId);
            when(mockLogCommand.add(any())).thenReturn(mockLogCommand);
            when(mockLogCommand.addPath(any())).thenReturn(mockLogCommand);
            RevCommit mockRevCommit = mock(RevCommit.class);
            List<RevCommit> logCommandCommits = Arrays.asList(mockRevCommit);
            doReturn(logCommandCommits).when(mockLogCommand).call();
            VersioningManager spyController = spy(versionController);

            boolean output = spyController.commitContainsPath("test", mockObjectId2);

            assertThat(output).isEqualTo(false);
            verify(git).log();
            verify(git).getRepository();
            verify(repo).resolve(Constants.HEAD);
            verify(mockLogCommand).add(any());
            verify(mockLogCommand).addPath(any());
            verify(mockLogCommand).call();
            verify(spyController).commitContainsPath(any(), any());
            verifyNoMoreInteractions(git, repo, mockLogCommand, spyController);
        }

        @Test
        void noCommits() throws GitAPIException, IOException {
            ObjectId mockObjectId = mock(ObjectId.class);
            LogCommand mockLogCommand = mock(LogCommand.class);
            when(git.log()).thenReturn(mockLogCommand);
            when(git.getRepository()).thenReturn(repo);
            when(repo.resolve(Constants.HEAD)).thenReturn(mockObjectId);
            when(mockLogCommand.add(any())).thenReturn(mockLogCommand);
            when(mockLogCommand.addPath(any())).thenReturn(mockLogCommand);
            RevCommit mockRevCommit = mock(RevCommit.class);
            doReturn(Collections.emptyList()).when(mockLogCommand).call();
            VersioningManager spyController = spy(versionController);

            boolean output = spyController.commitContainsPath("test", mockObjectId);

            assertThat(output).isEqualTo(false);
            verify(git).log();
            verify(git).getRepository();
            verify(repo).resolve(Constants.HEAD);
            verify(mockLogCommand).add(any());
            verify(mockLogCommand).addPath(any());
            verify(mockLogCommand).call();
            verify(spyController).commitContainsPath(any(), any());
            verifyNoMoreInteractions(git, repo, mockLogCommand, spyController);
        }
    }

    @Nested
    public class GetCommitById {
        @Test
        void hasCommit() throws IOException {
            ObjectId mockId = mock(ObjectId.class);
            RevWalk mockRevWalk = mock(RevWalk.class);
            RevCommit mockCommit = mock(RevCommit.class);
            VersioningManager spyController = spy(versionController);
            doReturn(mockRevWalk).when(spyController).getRevWalk();
            when(mockRevWalk.parseCommit(any())).thenReturn(mockCommit);

            RevCommit output = spyController.getCommitById(mockId);

            assertThat(output).isEqualTo(mockCommit);
            verify(spyController).getRevWalk();
            verify(mockRevWalk).close();
            verify(mockRevWalk).parseCommit(any());
            verify(spyController).getCommitById(mockId);
            verifyNoMoreInteractions(git, repo, mockRevWalk);
        }

        @Test
        void hasNoCommit() throws IOException {
            ObjectId mockId = mock(ObjectId.class);
            RevWalk mockRevWalk = mock(RevWalk.class);
            VersioningManager spyController = spy(versionController);
            doReturn(mockRevWalk).when(spyController).getRevWalk();
            when(mockRevWalk.parseCommit(any())).thenReturn(null);

            RevCommit output = spyController.getCommitById(mockId);

            assertThat(output).isEqualTo(null);
            verify(spyController).getRevWalk();
            verify(mockRevWalk).close();
            verify(mockRevWalk).parseCommit(any());
            verify(spyController).getCommitById(mockId);
            verifyNoMoreInteractions(git, repo, mockRevWalk);
        }
    }

    @Nested
    public class GetPathsOfCommit {
        @Test
        void hasPaths() throws IOException {
            RevCommit parentCommit = mock(RevCommit.class);
            RevCommit[] parentCommits = {parentCommit};
            RevCommit mockRevCommit = mock(RevCommit.class);
            RevWalk mockRevWalk = mock(RevWalk.class);
            RevTree mockRevTree = mock(RevTree.class);
            when(mockRevWalk.parseCommit(any())).thenReturn(mockRevCommit);
            DiffFormatter mockDiffFormatter = mock(DiffFormatter.class);
            DiffEntry mockDiffEntry = mock(DiffEntry.class);
            when(mockDiffEntry.getNewPath()).thenReturn("test");
            List<DiffEntry> scanOutput = Arrays.asList(mockDiffEntry);
            doReturn(scanOutput).when(mockDiffFormatter).scan(any(RevTree.class), any(RevTree.class));
            VersioningManager spyController = spy(versionController);
            doReturn(mockRevWalk).when(spyController).getRevWalk();
            doReturn(parentCommit).when(spyController).getParentOfRevCommit(any(), eq(0));
            doReturn(parentCommits).when(spyController).getParentsOfRevCommit(any());
            doReturn(mockDiffFormatter).when(spyController).getDiffFormatter();
            doReturn(mockRevTree).when(spyController).getRevtreeOfRevCommit(any());
            ObjectId mockObjectId = mock(ObjectId.class);
            when(repo.resolve(Constants.HEAD)).thenReturn(mockObjectId);

            List<String> output = spyController.getPathsOfCommit(mockObjectId);

            assertThat(output).isEqualTo(Arrays.asList("test"));
            verify(mockRevWalk, times(2)).parseCommit(any());
            verify(mockDiffEntry).getNewPath();
            verify(mockDiffFormatter).scan(mockRevTree, mockRevTree);
            verify(mockDiffFormatter).setRepository(any());
            verify(mockDiffFormatter).setDiffComparator(RawTextComparator.DEFAULT);
            verify(mockDiffFormatter).setDetectRenames(true);
            verify(spyController).getRevWalk();
            verify(spyController).getParentOfRevCommit(mockRevCommit, 0);
            verify(spyController).getParentsOfRevCommit(mockRevCommit);
            verify(spyController, times(2)).getRevtreeOfRevCommit(mockRevCommit);
            verify(spyController).getDiffFormatter();
            verify(spyController, times(2)).getRevtreeOfRevCommit(mockRevCommit);
            verify(spyController).getPathsOfCommit(mockObjectId);
            verify(repo).resolve(Constants.HEAD);
            verifyNoMoreInteractions(mockRevWalk, mockDiffEntry, mockDiffFormatter, spyController, repo);
        }

        @Test
        void hasNoPaths() throws IOException {
            RevCommit parentCommit = mock(RevCommit.class);
            RevCommit[] parentCommits = {parentCommit};
            RevCommit mockRevCommit = mock(RevCommit.class);
            RevWalk mockRevWalk = mock(RevWalk.class);
            RevTree mockRevTree = mock(RevTree.class);
            when(mockRevWalk.parseCommit(any())).thenReturn(mockRevCommit);
            DiffFormatter mockDiffFormatter = mock(DiffFormatter.class);
            List<DiffEntry> scanOutput = Collections.emptyList();
            doReturn(scanOutput).when(mockDiffFormatter).scan(any(RevTree.class), any(RevTree.class));
            VersioningManager spyController = spy(versionController);
            doReturn(mockRevWalk).when(spyController).getRevWalk();
            doReturn(parentCommit).when(spyController).getParentOfRevCommit(any(), eq(0));
            doReturn(parentCommits).when(spyController).getParentsOfRevCommit(any());
            doReturn(mockDiffFormatter).when(spyController).getDiffFormatter();
            doReturn(mockRevTree).when(spyController).getRevtreeOfRevCommit(any());
            ObjectId mockObjectId = mock(ObjectId.class);
            when(repo.resolve(Constants.HEAD)).thenReturn(mockObjectId);

            List<String> output = spyController.getPathsOfCommit(mockObjectId);

            assertThat(output).isEqualTo(Collections.emptyList());
            verify(mockRevWalk, times(2)).parseCommit(any());
            verify(mockDiffFormatter).scan(mockRevTree, mockRevTree);
            verify(mockDiffFormatter).setRepository(any());
            verify(mockDiffFormatter).setDiffComparator(RawTextComparator.DEFAULT);
            verify(mockDiffFormatter).setDetectRenames(true);
            verify(spyController).getRevWalk();
            verify(spyController).getParentOfRevCommit(mockRevCommit, 0);
            verify(spyController).getParentsOfRevCommit(mockRevCommit);
            verify(spyController, times(2)).getRevtreeOfRevCommit(mockRevCommit);
            verify(spyController).getDiffFormatter();
            verify(spyController, times(2)).getRevtreeOfRevCommit(mockRevCommit);
            verify(spyController).getPathsOfCommit(mockObjectId);
            verify(repo).resolve(Constants.HEAD);
            verifyNoMoreInteractions(mockRevWalk, mockDiffFormatter, spyController, repo);
        }
    }
}
