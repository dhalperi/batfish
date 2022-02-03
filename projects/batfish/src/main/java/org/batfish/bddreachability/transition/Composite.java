package org.batfish.bddreachability.transition;

import static com.google.common.base.Preconditions.checkArgument;
import static org.batfish.bddreachability.transition.Transitions.ZERO;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import net.sf.javabdd.BDD;

/** The functional composition of two or more transitions */
public final class Composite implements Transition {
  private final List<Transition> _transitions;

  Composite(Transition... transitions) {
    this(ImmutableList.copyOf(transitions));
  }

  Composite(List<Transition> transitions) {
    checkArgument(!transitions.isEmpty(), "Cannot compose 0 Transitions. Use Identity instead");
    checkArgument(
        transitions.size() != 1,
        "Cannot compose 1 Transition. Use that transition directly instead.");
    _transitions = ImmutableList.copyOf(transitions);
  }

  @Override
  public Transition inUniverse(BDD universe) {
    List<Transition> inner = new ArrayList<>(_transitions.size());
    boolean changed = false;
    for (Transition t : _transitions) {
      Transition tPrime = t.inUniverse(universe);
      if (tPrime == ZERO) {
        return tPrime;
      }
      inner.add(tPrime);
      changed |= tPrime != t;
    }
    if (!changed) {
      return this;
    }
    return Transitions.compose(inner.toArray(new Transition[0]));
  }

  public List<Transition> getTransitions() {
    return _transitions;
  }

  @Override
  public BDD transitForward(BDD bdd) {
    BDD result = bdd.id();
    for (int i = 0; i < _transitions.size() && !result.isZero(); i++) {
      BDD nextResult = _transitions.get(i).transitForward(result);
      result.free();
      result = nextResult;
    }
    return result;
  }

  @Override
  public BDD transitBackward(BDD bdd) {
    BDD result = bdd.id();
    for (int i = _transitions.size() - 1; i >= 0 && !result.isZero(); i--) {
      BDD nextResult = _transitions.get(i).transitBackward(result);
      result.free();
      result = nextResult;
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Composite)) {
      return false;
    }
    return _transitions.equals(((Composite) o)._transitions);
  }

  @Override
  public int hashCode() {
    return _transitions.hashCode();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(Composite.class).add("transitions", _transitions).toString();
  }
}
