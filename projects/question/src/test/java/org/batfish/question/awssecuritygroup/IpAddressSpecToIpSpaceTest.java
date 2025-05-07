package org.batfish.question.awssecuritygroup;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.representation.cisco_nxos.AddrGroupIpAddressSpec;
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.representation.cisco_nxos.LiteralIpAddressSpec;
import org.batfish.representation.cisco_nxos.ObjectGroup;
import org.batfish.representation.cisco_nxos.ObjectGroupIpAddress;
import org.batfish.representation.cisco_nxos.ObjectGroupIpAddressLine;
import org.junit.Test;

/** Tests for {@link IpAddressSpecToIpSpace}. */
public class IpAddressSpecToIpSpaceTest {

  @Test
  public void testVisitLiteralIpAddressSpec() {
    // Create a literal IP address spec with a specific IpSpace
    IpSpace ipSpace = IpWildcard.parse("192.168.1.0/24").toIpSpace();
    LiteralIpAddressSpec literalIpAddressSpec = new LiteralIpAddressSpec(ipSpace);

    // Create the visitor
    IpAddressSpecToIpSpace visitor = new IpAddressSpecToIpSpace(mock(CiscoNxosConfiguration.class));

    // Verify that the visitor returns the same IpSpace
    assertThat(literalIpAddressSpec.accept(visitor), equalTo(ipSpace));
  }

  @Test
  public void testVisitAddrGroupIpAddressSpec() {
    // Create a mock configuration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);

    // Create an address group
    ObjectGroupIpAddress ipAddressGroup = new ObjectGroupIpAddress("test-group");
    ipAddressGroup
        .getLines()
        .put(1L, new ObjectGroupIpAddressLine(1, IpWildcard.parse("192.168.1.0/24")));
    ipAddressGroup
        .getLines()
        .put(2L, new ObjectGroupIpAddressLine(2, IpWildcard.parse("10.0.0.0/8")));

    // Set up the mock to return our address group
    when(config.getObjectGroups()).thenReturn(ImmutableMap.of("test-group", ipAddressGroup));

    // Create an address group IP address spec
    AddrGroupIpAddressSpec addrGroupIpAddressSpec = new AddrGroupIpAddressSpec("test-group");

    // Create the visitor
    IpAddressSpecToIpSpace visitor = new IpAddressSpecToIpSpace(config);

    // Verify that the visitor returns an IpSpace that contains both networks
    IpSpace result = addrGroupIpAddressSpec.accept(visitor);

    // The result should contain both networks
    assertThat(
        result.containsIp(IpWildcard.parse("192.168.1.1").getIp(), ImmutableMap.of()),
        equalTo(true));
    assertThat(
        result.containsIp(IpWildcard.parse("10.1.1.1").getIp(), ImmutableMap.of()), equalTo(true));
    assertThat(
        result.containsIp(IpWildcard.parse("172.16.1.1").getIp(), ImmutableMap.of()),
        equalTo(false));
  }

  @Test
  public void testVisitAddrGroupIpAddressSpec_groupNotFound() {
    // Create a mock configuration with no object groups
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);
    when(config.getObjectGroups()).thenReturn(ImmutableMap.of());

    // Create an address group IP address spec with a non-existent group
    AddrGroupIpAddressSpec addrGroupIpAddressSpec =
        new AddrGroupIpAddressSpec("non-existent-group");

    // Create the visitor
    IpAddressSpecToIpSpace visitor = new IpAddressSpecToIpSpace(config);

    // Verify that the visitor returns null for non-existent group
    IpSpace result = addrGroupIpAddressSpec.accept(visitor);

    // The result should be null
    assertThat(result, nullValue());
  }

  @Test
  public void testToIpSpace_null() {
    // Verify that toIpSpace handles null address spec
    IpSpace result = IpAddressSpecToIpSpace.toIpSpace(mock(CiscoNxosConfiguration.class), null);

    // The result should be null
    assertThat(result, nullValue());
  }

  @Test
  public void testToIpSpace_wrongGroupType() {
    // Create a mock configuration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);

    // Create a non-IP address object group
    ObjectGroup nonIpGroup = mock(ObjectGroup.class);

    // Set up the mock to return our non-IP address group
    when(config.getObjectGroups()).thenReturn(ImmutableMap.of("test-group", nonIpGroup));

    // Create an address group IP address spec
    AddrGroupIpAddressSpec addrGroupIpAddressSpec = new AddrGroupIpAddressSpec("test-group");

    // Verify that toIpSpace handles wrong group type
    IpSpace result = IpAddressSpecToIpSpace.toIpSpace(config, addrGroupIpAddressSpec);

    // The result should be null
    assertThat(result, nullValue());
  }
}
