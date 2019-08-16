package org.batfish.datamodel.transformation;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A sequence of packet {@link Transformation transformations} that are executed unconditionally.
 */
@ParametersAreNonnullByDefault
public final class Composite extends Transformation {
  private static final String PROP_TRANSFORMATIONS = "transformations";

  private final @Nonnull List<Transformation> _transformations;

  public Composite(@Nonnull Transformation... transformations) {
    this(Arrays.asList(transformations));
  }

  public Composite(@Nonnull Iterable<Transformation> transformations) {
    _transformations = ImmutableList.copyOf(transformations);
  }

  @JsonCreator
  private static Composite jsonCreator(
      @JsonProperty(PROP_TRANSFORMATIONS) List<Transformation> transformations) {
    return new Composite(firstNonNull(transformations, ImmutableList.of()));
  }

  /** A list of transformations to apply in sequence. */
  @JsonProperty(PROP_TRANSFORMATIONS)
  @Nonnull
  public List<Transformation> getTransformations() {
    return _transformations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Composite)) {
      return false;
    }
    Composite that = (Composite) o;
    return _transformations.equals(that._transformations);
  }

  @Override
  public int hashCode() {
    return _transformations.hashCode();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("transformations", _transformations).toString();
  }

  @Override
  public <T> T accept(TransformationVisitor<T> visitor) {
    return visitor.visitComposite(this);
  }
}
