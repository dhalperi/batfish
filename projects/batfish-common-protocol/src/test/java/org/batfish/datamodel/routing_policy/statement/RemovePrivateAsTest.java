package org.batfish.datamodel.routing_policy.statement;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.routing_policy.statement.RemovePrivateAs.How;
import org.batfish.datamodel.routing_policy.statement.RemovePrivateAs.When;
import org.batfish.datamodel.routing_policy.statement.RemovePrivateAs.Where;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of {@link RemovePrivateAs}. */
@RunWith(JUnit4.class)
public class RemovePrivateAsTest {
  @Test
  public void testWhere() {
    AsPath onlyPrivate = AsPath.ofSingletonAsSets(64512L, 65534L, 4200000000L, 4294967294L);
    AsPath publicAtFront = AsPath.ofSingletonAsSets(1L, 2L, 3L, 64512L);
    AsPath publicAtEnd = AsPath.ofSingletonAsSets(64512L, 1L, 2L, 3L);
    AsPath publicInMiddle = AsPath.ofSingletonAsSets(64512L, 1L, 2L, 3L, 64513L);

    {
      RemovePrivateAs rpa = new RemovePrivateAs(When.ALWAYS, How.REMOVE, Where.ALL);
      assertThat(rpa.applyTo(onlyPrivate, null), equalTo(AsPath.empty()));
      assertThat(rpa.applyTo(publicAtFront, null), equalTo(AsPath.ofSingletonAsSets(1L, 2L, 3L)));
      assertThat(rpa.applyTo(publicAtEnd, null), equalTo(AsPath.ofSingletonAsSets(1L, 2L, 3L)));
      assertThat(rpa.applyTo(publicInMiddle, null), equalTo(AsPath.ofSingletonAsSets(1L, 2L, 3L)));
    }
    {
      RemovePrivateAs rpa = new RemovePrivateAs(When.ALWAYS, How.REMOVE, Where.ONLY_AT_FRONT);
      assertThat(rpa.applyTo(onlyPrivate, null), equalTo(AsPath.empty()));
      assertThat(
          rpa.applyTo(publicAtFront, null), equalTo(AsPath.ofSingletonAsSets(1L, 2L, 3L, 64512L)));
      assertThat(rpa.applyTo(publicAtEnd, null), equalTo(AsPath.ofSingletonAsSets(1L, 2L, 3L)));
      assertThat(
          rpa.applyTo(publicInMiddle, null), equalTo(AsPath.ofSingletonAsSets(1L, 2L, 3L, 64513L)));
    }
  }

  @Test
  public void testHow() {
    AsPath testPath = AsPath.ofSingletonAsSets(64512L, 1L, 2L, 3L, 64513L);
    AsSet localAs = AsSet.of(99L);
    {
      RemovePrivateAs rpa = new RemovePrivateAs(When.ALWAYS, How.REMOVE, Where.ALL);
      assertThat(rpa.applyTo(testPath, localAs), equalTo(AsPath.ofSingletonAsSets(1L, 2L, 3L)));
    }
    {
      RemovePrivateAs rpa = new RemovePrivateAs(When.ALWAYS, How.REPLACE_WITH_LOCAL_AS, Where.ALL);
      assertThat(
          rpa.applyTo(testPath, localAs), equalTo(AsPath.ofSingletonAsSets(99L, 1L, 2L, 3L, 99L)));
    }
  }

  @Test
  public void testWhen() {
    AsPath onlyPrivate = AsPath.ofSingletonAsSets(64512L, 65534L, 4200000000L, 4294967294L);
    AsPath publicInMiddle = AsPath.ofSingletonAsSets(64512L, 1L, 2L, 3L, 64513L);
    {
      RemovePrivateAs rpa = new RemovePrivateAs(When.ALWAYS, How.REMOVE, Where.ALL);
      assertThat(rpa.applyTo(onlyPrivate, null), equalTo(AsPath.empty()));
      assertThat(rpa.applyTo(publicInMiddle, null), equalTo(AsPath.ofSingletonAsSets(1L, 2L, 3L)));
    }
    {
      RemovePrivateAs rpa = new RemovePrivateAs(When.ONLY_IF_NO_PUBLIC, How.REMOVE, Where.ALL);
      assertThat(rpa.applyTo(onlyPrivate, null), equalTo(AsPath.empty()));
      assertThat(rpa.applyTo(publicInMiddle, null), equalTo(publicInMiddle));
    }
    {
      RemovePrivateAs rpa =
          new RemovePrivateAs(When.ONLY_IF_NON_EMPTY_AFTER, How.REMOVE, Where.ALL);
      assertThat(rpa.applyTo(onlyPrivate, null), equalTo(onlyPrivate));
      assertThat(rpa.applyTo(publicInMiddle, null), equalTo(AsPath.ofSingletonAsSets(1L, 2L, 3L)));
    }
  }
}
