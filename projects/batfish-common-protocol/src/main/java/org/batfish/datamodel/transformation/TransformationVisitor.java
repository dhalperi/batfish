package org.batfish.datamodel.transformation;

/** A visitor of {@link Transformation} objects. */
public interface TransformationVisitor<T> {
  default T visit(Transformation transformation) {
    return transformation.accept(this);
  }

  T visitBranch(Branch branch);

  T visitComposite(Composite composite);
}
