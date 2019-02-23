package org.batfish.specifier.parboiled;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.IntegerComponentNameProvider;
import org.jgrapht.io.StringComponentNameProvider;
import org.parboiled.Rule;
import org.parboiled.matchers.CharIgnoreCaseMatcher;
import org.parboiled.matchers.CharMatcher;
import org.parboiled.matchers.CharRangeMatcher;
import org.parboiled.matchers.Matcher;

/** A utility class to generate the graph of a parboiled {@link Parser}. */
class GenerateParserGraph {
  public static void doIt(Rule r) {
    checkArgument(
        r instanceof Matcher, "Cannot generate the graph for a rule that is not also a matcher");

    Graph<String, DefaultEdge> graph =
        GraphTypeBuilder.<String, DefaultEdge>directed()
            .allowingSelfLoops(true)
            .allowingMultipleEdges(true)
            .edgeClass(DefaultEdge.class)
            .buildGraph();
    recurseBuilding(graph, (Matcher) r, new HashSet<>(), (Matcher) r);
    DOTExporter<String, DefaultEdge> exp =
        new DOTExporter<>(
            new IntegerComponentNameProvider<>(), new StringComponentNameProvider<>(), null);
    StringWriter sw = new StringWriter();
    exp.exportGraph(graph, sw);
    System.err.println(sw.toString());
  }

  private static boolean suppressNode(Matcher node) {
    return ImmutableSet.of(
                "fromStringLiteral",
                "AlphabetChar",
                "Digit",
                "FirstOf",
                "Sequence",
                "WhiteSpace",
                "ZeroOrMore")
            .contains(node.getLabel())
        || (node instanceof CharIgnoreCaseMatcher)
        || (node instanceof CharMatcher)
        || (node instanceof CharRangeMatcher)
        || (node.getLabel().contains("_Action"));
  }

  private static void recurseBuilding(
      Graph<String, DefaultEdge> graph, Matcher node, Set<Matcher> visited, Matcher parent) {
    if (!visited.add(node)) {
      // Already visited this node.
      return;
    }
    if (!suppressNode(node)) {
      parent = node;
      graph.addVertex(node.getLabel());
    }

    for (Matcher child : node.getChildren()) {
      if (!suppressNode(child)) {
        graph.addVertex(child.getLabel());
        graph.addEdge(parent.getLabel(), child.getLabel());
      }
      recurseBuilding(graph, child, visited, parent);
    }
  }

  public static void main(String[] args) {
    doIt(Grammar.FILTER_SPECIFIER.getExpression());
  }
}
