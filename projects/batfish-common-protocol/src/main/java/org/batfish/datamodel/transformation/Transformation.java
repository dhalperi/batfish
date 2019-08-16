package org.batfish.datamodel.transformation;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import javax.annotation.ParametersAreNonnullByDefault;

/** A representation of a packet transformation. */
@ParametersAreNonnullByDefault
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public abstract class Transformation implements Serializable {
  public abstract <T> T accept(TransformationVisitor<T> visitor);
}
