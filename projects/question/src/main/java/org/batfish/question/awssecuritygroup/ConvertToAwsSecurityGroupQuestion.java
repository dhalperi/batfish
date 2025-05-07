package org.batfish.question.awssecuritygroup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.questions.Question;
import org.batfish.specifier.AllFiltersFilterSpecifier;
import org.batfish.specifier.AllNodesNodeSpecifier;
import org.batfish.specifier.FilterSpecifier;
import org.batfish.specifier.NodeSpecifier;
import org.batfish.specifier.SpecifierFactories;

/**
 * A question that converts firewall access lists to AWS security group rules. Currently supports
 * Cisco NX-OS access lists.
 */
public class ConvertToAwsSecurityGroupQuestion extends Question {
  private static final String PROP_FILTERS = "filters";
  private static final String PROP_NODES = "nodes";

  private final @Nullable String _filters;
  private final @Nullable String _nodes;

  @JsonCreator
  public ConvertToAwsSecurityGroupQuestion(
      @JsonProperty(PROP_NODES) String nodes, @JsonProperty(PROP_FILTERS) String filters) {
    _nodes = nodes;
    _filters = filters;
  }

  @Override
  public boolean getDataPlane() {
    return false;
  }

  @JsonProperty(PROP_FILTERS)
  public @Nullable String getFilters() {
    return _filters;
  }

  @JsonIgnore
  public @Nonnull FilterSpecifier getFilterSpecifier() {
    return SpecifierFactories.getFilterSpecifierOrDefault(
        _filters, AllFiltersFilterSpecifier.INSTANCE);
  }

  @JsonIgnore
  public @Nonnull NodeSpecifier getNodeSpecifier() {
    return SpecifierFactories.getNodeSpecifierOrDefault(_nodes, AllNodesNodeSpecifier.INSTANCE);
  }

  @Override
  public String getName() {
    return "convertToAwsSecurityGroup";
  }

  @JsonProperty(PROP_NODES)
  public @Nullable String getNodes() {
    return _nodes;
  }
}
