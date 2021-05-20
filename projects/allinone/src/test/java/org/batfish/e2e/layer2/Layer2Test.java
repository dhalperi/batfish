package org.batfish.e2e.layer2;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Edge;
import org.batfish.datamodel.Topology;
import org.batfish.datamodel.collections.NodeInterfacePair;
import org.batfish.main.BatfishTestUtils;
import org.batfish.main.TestrigText;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Various unit tests of Layer1 + Layer2 + Layer3 functionality.
 *
 * <p>In this package to permit use of parsing instead of error-prone direct construction of VI.
 */
public class Layer2Test {
  @Rule public TemporaryFolder _folder = new TemporaryFolder();

  private Topology getLayer3Edges(
      @Nonnull String snapshot, @Nullable String nameOfBatfishFolderContainingL1, String... names)
      throws IOException {
    String prefix = "org/batfish/e2e/layer2/" + snapshot;
    String l1prefix =
        nameOfBatfishFolderContainingL1 == null
            ? null
            : prefix + '/' + nameOfBatfishFolderContainingL1;
    IBatfish batfish =
        BatfishTestUtils.getBatfishFromTestrigText(
            TestrigText.builder()
                .setConfigurationFiles(prefix, names)
                .setLayer1TopologyPrefix(l1prefix)
                .build(),
            _folder);
    return batfish.getTopologyProvider().getInitialLayer3Topology(batfish.getSnapshot());
  }

  /** When the entire l1 topology is present, the L3 edge comes up. */
  @Test
  public void testLayer3ConnectivityWithL1() throws IOException {
    Edge forward =
        new Edge(NodeInterfacePair.of("r1", "Ethernet1"), NodeInterfacePair.of("r2", "Ethernet1"));
    assertThat(
        getLayer3Edges("direct", "batfish", "r1", "r2").getEdges(),
        containsInAnyOrder(forward, forward.reverse()));
  }

  /** When the self-loop is missing from l1 topology, the L3 edge does not come up. */
  @Test
  public void testLayer3ConnectivityWithoutL1() throws IOException {
    assertThat(getLayer3Edges("direct", null).getEdges(), empty());
  }
}
