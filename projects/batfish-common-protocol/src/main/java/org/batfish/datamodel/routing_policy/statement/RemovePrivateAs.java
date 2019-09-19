package org.batfish.datamodel.routing_policy.statement;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpRoute;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;

/** Implements the removal of private AS numbers from AS-paths. */
@ParametersAreNonnullByDefault
public final class RemovePrivateAs extends Statement {
  public enum When {
    /** Remove any private AS numbers in the AS-Path. */
    ALWAYS,
    /** Remove private AS numbers only if the AS-Path has no public ASNs. */
    ONLY_IF_NO_PUBLIC,
    /** Remove private AS numbers only if the AS-Path has at least one public ASN. */
    ONLY_IF_PUBLIC,
  }

  public enum Where {
    /** Remove all private AS numbers. */
    ALL,
    /** Remove only leading private AS numbers. */
    ONLY_AT_FRONT,
  }

  public enum How {
    /** Remove the AS entry. */
    REMOVE,
    /** Replace with the local ASN. */
    REPLACE_WITH_LOCAL_AS,
  }

  public RemovePrivateAs(When when, How how, Where where) {
    _when = when;
    _how = how;
    _where = where;
  }

  private static boolean inputPathContainsPublicAs(AsPath path) {
    for (AsSet set : path.getAsSets()) {
      if (asSetIsPublicAs(set)) {
        return true;
      }
    }
    return false;
  }

  private static boolean asSetIsPublicAs(AsSet set) {
    // todo: what happens for an as-set with more than one property? for now assume any.
    return set.stream().anyMatch(as -> !AsPath.isPrivateAs(as));
  }

  @VisibleForTesting
  AsPath applyTo(AsPath input, @Nullable AsSet localAs) {
    assert _how != How.REPLACE_WITH_LOCAL_AS || localAs != null;

    if (_when == When.ONLY_IF_PUBLIC) {
      if (!inputPathContainsPublicAs(input)) {
        return input;
      }
    } else if (_when == When.ONLY_IF_NO_PUBLIC) {
      if (inputPathContainsPublicAs(input)) {
        return input;
      }
    } else {
      assert _when == When.ALWAYS;
    }

    List<AsSet> inputAsSets = input.getAsSets();
    ImmutableList.Builder<AsSet> outputPath = ImmutableList.builder();
    int i = 0;
    while (i < inputAsSets.size()) {
      AsSet set = inputAsSets.get(i);
      if (asSetIsPublicAs(set)) {
        if (_where == Where.ONLY_AT_FRONT) {
          break;
        }
        outputPath.add(set);
      } else if (_how == How.REPLACE_WITH_LOCAL_AS) {
        outputPath.add(localAs);
      }
      ++i;
    }
    outputPath.addAll(inputAsSets.subList(i, inputAsSets.size()));
    return AsPath.of(outputPath.build());
  }

  @Override
  public Result execute(Environment environment) {
    BgpSessionProperties props = environment.getBgpSessionProperties();
    checkState(props != null, "Expected BGP session properties");
    BgpRoute.Builder<?, ?> bgpRouteBuilder = (BgpRoute.Builder<?, ?>) environment.getOutputRoute();
    AsSet localAs = null;
    if (_how == How.REPLACE_WITH_LOCAL_AS) {
      localAs = AsSet.of(props.getHeadAs());
    }

    AsPath newPath = applyTo(bgpRouteBuilder.getAsPath(), localAs);
    bgpRouteBuilder.setAsPath(newPath);
    if (environment.getWriteToIntermediateBgpAttributes()) {
      BgpRoute.Builder<?, ?> ir = environment.getIntermediateBgpAttributes();
      ir.setAsPath(applyTo(ir.getAsPath(), localAs));
    }

    Result result = new Result();
    return result;
  }

  @JsonProperty(PROP_WHEN)
  @Nonnull
  public When getWhen() {
    return _when;
  }

  @JsonProperty(PROP_WHERE)
  @Nonnull
  public Where getWhere() {
    return _where;
  }

  @JsonProperty(PROP_HOW)
  @Nonnull
  public How getHow() {
    return _how;
  }

  @JsonCreator
  private static RemovePrivateAs jsonCreator(
      @Nullable @JsonProperty(PROP_WHEN) When when,
      @Nullable @JsonProperty(PROP_HOW) How how,
      @Nullable @JsonProperty(PROP_WHERE) Where where) {
    checkArgument(when != null, "%s must be provided", PROP_WHEN);
    checkArgument(how != null, "%s must be provided", PROP_HOW);
    checkArgument(where != null, "%s must be provided", PROP_WHERE);
    return new RemovePrivateAs(when, how, where);
  }

  @Override
  public <T, U> T accept(StatementVisitor<T, U> visitor, U arg) {
    return visitor.visitRemovePrivateAs(this, arg);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof RemovePrivateAs)) {
      return false;
    }
    RemovePrivateAs that = (RemovePrivateAs) o;
    return _when == that._when && _how == that._how && _where == that._where;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_when, _how, _where);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("when", _when)
        .add("how", _how)
        .add("where", _where)
        .toString();
  }

  private static final String PROP_HOW = "how";
  private static final String PROP_WHEN = "when";
  private static final String PROP_WHERE = "where";

  @Nonnull private final How _how;
  @Nonnull private final When _when;
  @Nonnull private final Where _where;
}
