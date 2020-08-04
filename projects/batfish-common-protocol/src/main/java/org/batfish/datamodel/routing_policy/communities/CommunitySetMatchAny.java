package org.batfish.datamodel.routing_policy.communities;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Matches a {@link CommunitySet} if it is matched by any of the provided {@link
 * CommunitySetMatchExpr}s. If empty, never matches.
 */
public final class CommunitySetMatchAny extends CommunitySetMatchExpr {

  public static CommunitySetMatchExpr create(Collection<CommunitySetMatchExpr> exprs) {
    checkArgument(!exprs.isEmpty(), "Require at least one statement to match.");
    if (exprs.size() == 1) {
      return Iterables.getOnlyElement(exprs);
    }
    return new CommunitySetMatchAny(exprs);
  }

  @VisibleForTesting
  CommunitySetMatchAny(Iterable<CommunitySetMatchExpr> exprs) {
    _exprs = ImmutableSet.copyOf(exprs);
  }

  public @Nonnull Set<CommunitySetMatchExpr> getExprs() {
    return _exprs;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof CommunitySetMatchAny)) {
      return false;
    }
    return _exprs.equals(((CommunitySetMatchAny) obj)._exprs);
  }

  @Override
  public int hashCode() {
    return _exprs.hashCode();
  }

  @Override
  public <T, U> T accept(CommunitySetMatchExprVisitor<T, U> visitor, U arg) {
    return visitor.visitCommunitySetMatchAny(this, arg);
  }

  private static final String PROP_EXPRS = "exprs";

  @JsonCreator
  private static @Nonnull CommunitySetMatchAny create(
      @JsonProperty(PROP_EXPRS) @Nullable Iterable<CommunitySetMatchExpr> exprs) {
    return new CommunitySetMatchAny(ImmutableSet.copyOf(firstNonNull(exprs, ImmutableSet.of())));
  }

  private final @Nonnull Set<CommunitySetMatchExpr> _exprs;

  @JsonProperty(PROP_EXPRS)
  private @Nonnull List<CommunitySetMatchExpr> getExprsSorted() {
    // ordered for refs
    return ImmutableList.copyOf(_exprs);
  }
}
