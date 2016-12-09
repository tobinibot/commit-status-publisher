package jetbrains.buildServer.commitPublisher;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.systemProblems.*;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NonNls;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author anton.zamolotskikh, 13/09/16.
 */

@Test
public class CommitStatusPublisherProblemsTest extends BaseServerTestCase {

  private final static String FEATURE_1 = "PUBLISH_BUILD_FEATURE_1";
  private final static String FEATURE_2 = "PUBLISH_BUILD_FEATURE_2";

  private CommitStatusPublisherProblems myProblems;
  private SystemProblemNotificationEngine myProblemEngine;
  private CommitStatusPublisher myPublisher;
  private PublisherLogger myLogger;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myLogger = new PublisherLogger();
    myProblemEngine = myFixture.getSingletonService(SystemProblemNotificationEngine.class);
    myProblems = new CommitStatusPublisherProblems(myProblemEngine);
    myPublisher = new MockPublisher("PUBLISHER1", myBuildType, FEATURE_1, Collections.<String, String>emptyMap(), myProblems);
  }

  public void must_add_and_delete_problems() {
    myProblems.reportProblem("Some problem description", myPublisher, "Build description", null, null, myLogger);
    then(myProblemEngine.getProblems(myBuildType).size()).isEqualTo(1);
    String lastLogged = myLogger.popLast();
    then(lastLogged.contains("Some problem description"));
    then(lastLogged.contains(myPublisher.getId()));
    then(lastLogged.contains("Build description"));
    myProblems.clearProblem(myPublisher);
    then(myProblemEngine.getProblems(myBuildType).size()).isEqualTo(0);
  }

  public void must_clear_obsolete_problems() {
    final String PUB1_P1 = "First issue of publisher 1";
    final String PUB2_P1 = "First issue of publisher 2";
    final String PUB2_P2 = "Second issue of publisher 2";
    CommitStatusPublisher publisher2 = new MockPublisher("PUBLISHER2", myBuildType, FEATURE_2, Collections.<String, String>emptyMap(), myProblems);

    myProblems.reportProblem(PUB2_P1, publisher2, "Build description", null, null, myLogger);
    myProblems.reportProblem(PUB1_P1, myPublisher, "Build description", null, null, myLogger);
    myProblems.reportProblem(PUB2_P2, publisher2, "Build description", null, null, myLogger);
    Collection<SystemProblemEntry> problems = myProblemEngine.getProblems(myBuildType);
    then(problems.size()).isEqualTo(2);
    for (SystemProblemEntry pe: problems) {
      String description;
      description = pe.getProblem().getDescription();
      if(!description.contains(PUB1_P1))
        then(description).contains(PUB2_P2);
    }
    myProblems.clearObsoleteProblems(myBuildType, Collections.singletonList(FEATURE_1));
    Collection<SystemProblemEntry> remainingProblems = myProblemEngine.getProblems(myBuildType);
    then(remainingProblems.size()).isEqualTo(1);
    then(remainingProblems.iterator().next().getProblem().getDescription()).contains(PUB1_P1);
  }

  private class PublisherLogger extends Logger {

    private Stack<String> entries = new Stack<String>();

    String popLast() {
      return entries.pop();
    }

    @Override
    public boolean isDebugEnabled() {
      return false;
    }

    @Override
    public void debug(@NonNls final String message) {
      entries.push("DEBUG: " + message);
    }

    @Override
    public void debug(@NonNls final String message, final Throwable t) {
      debug(message);
    }

    @Override
    public void error(@NonNls final String message, final Throwable t, @NonNls final String... details) {
      entries.push("ERROR: " + message);
    }

    @Override
    public void info(@NonNls final String message) {
      entries.push("INFO: " + message);
    }

    @Override
    public void info(@NonNls final String message, final Throwable t) {
      info(message);
    }

    @Override
    public void warn(@NonNls final String message, final Throwable t) {
      entries.push("WARN: " + message);
    }

    @Override
    public void setLevel(final Level level) { }
  }

}
