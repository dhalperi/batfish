package org.batfish.question.awssecuritygroup;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.batfish.datamodel.IntegerSpace;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.SubRange;
import org.batfish.question.awssecuritygroup.AwsSecurityGroupRule.Protocol;
import org.batfish.representation.cisco_nxos.ActionIpAccessListLine;
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.representation.cisco_nxos.IcmpOptions;
import org.batfish.representation.cisco_nxos.IpAddressSpec;
import org.batfish.representation.cisco_nxos.LiteralPortSpec;
import org.batfish.representation.cisco_nxos.TcpOptions;
import org.batfish.representation.cisco_nxos.UdpOptions;
import org.junit.Test;

/** Tests for {@link CiscoNxosToAwsSecurityGroupConverter}. */
public class CiscoNxosToAwsSecurityGroupConverterTest {

  private static final String LINE_NAME = "Test line";

  /** Creates a LiteralIpAddressSpec with the specified CIDR. */
  private IpAddressSpec createLiteralIpAddressSpec(String cidr) {
    return new org.batfish.representation.cisco_nxos.LiteralIpAddressSpec(
        Prefix.parse(cidr).toIpSpace());
  }

  @Test
  public void testConvertTcpLine() {
    // Create a vendor-specific access list with a TCP line
    org.batfish.representation.cisco_nxos.IpAccessList accessList =
        new org.batfish.representation.cisco_nxos.IpAccessList("test-acl");

    // Create a TCP line with port 80
    IpAddressSpec srcAddressSpec = createLiteralIpAddressSpec("192.168.1.0/24");
    IpAddressSpec dstAddressSpec = createLiteralIpAddressSpec("0.0.0.0/0");

    TcpOptions tcpOptions =
        TcpOptions.builder().setDstPortSpec(new LiteralPortSpec(IntegerSpace.of(80))).build();

    ActionIpAccessListLine line =
        ActionIpAccessListLine.builder()
            .setAction(LineAction.PERMIT)
            .setProtocol(IpProtocol.TCP)
            .setSrcAddressSpec(srcAddressSpec)
            .setDstAddressSpec(dstAddressSpec)
            .setL4Options(tcpOptions)
            .setLine(10)
            .setText(LINE_NAME)
            .build();

    accessList.getLines().put(10L, line);

    // Create a mock CiscoNxosConfiguration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);
    when(config.getIpAccessLists()).thenReturn(ImmutableMap.of("test-acl", accessList));

    // Convert the access list
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify the converted rule
    assertThat(rules, hasSize(1));
    AwsSecurityGroupRule rule = rules.get(0);
    assertThat(rule.getProtocol(), equalTo(Protocol.TCP));
    assertThat(rule.getSourceCidr(), equalTo("192.168.1.0/24"));
    assertThat(rule.getFromPort(), equalTo(80));
    assertThat(rule.getToPort(), equalTo(80));
    assertThat(rule.getDescription(), equalTo(LINE_NAME));
  }

  @Test
  public void testConvertTcpLinePortRange() {
    // Create a vendor-specific access list with a TCP line with port range
    org.batfish.representation.cisco_nxos.IpAccessList accessList =
        new org.batfish.representation.cisco_nxos.IpAccessList("test-acl");

    // Create a TCP line with port range 8000-9000
    IpAddressSpec srcAddressSpec = createLiteralIpAddressSpec("192.168.1.0/24");
    IpAddressSpec dstAddressSpec = createLiteralIpAddressSpec("0.0.0.0/0");

    TcpOptions tcpOptions =
        TcpOptions.builder()
            .setDstPortSpec(new LiteralPortSpec(IntegerSpace.of(new SubRange(8000, 9000))))
            .build();

    ActionIpAccessListLine line =
        ActionIpAccessListLine.builder()
            .setAction(LineAction.PERMIT)
            .setProtocol(IpProtocol.TCP)
            .setSrcAddressSpec(srcAddressSpec)
            .setDstAddressSpec(dstAddressSpec)
            .setL4Options(tcpOptions)
            .setLine(10)
            .setText(LINE_NAME)
            .build();

    accessList.getLines().put(10L, line);

    // Create a mock CiscoNxosConfiguration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);
    when(config.getIpAccessLists()).thenReturn(ImmutableMap.of("test-acl", accessList));

    // Convert the access list
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify the converted rule
    assertThat(rules, hasSize(1));
    AwsSecurityGroupRule rule = rules.get(0);
    assertThat(rule.getProtocol(), equalTo(Protocol.TCP));
    assertThat(rule.getSourceCidr(), equalTo("192.168.1.0/24"));
    assertThat(rule.getFromPort(), equalTo(8000));
    assertThat(rule.getToPort(), equalTo(9000));
    assertThat(rule.getDescription(), equalTo(LINE_NAME));
  }

  @Test
  public void testConvertUdpLine() {
    // Create a vendor-specific access list with a UDP line
    org.batfish.representation.cisco_nxos.IpAccessList accessList =
        new org.batfish.representation.cisco_nxos.IpAccessList("test-acl");

    // Create a UDP line with port 53
    IpAddressSpec srcAddressSpec = createLiteralIpAddressSpec("192.168.1.0/24");
    IpAddressSpec dstAddressSpec = createLiteralIpAddressSpec("0.0.0.0/0");

    UdpOptions udpOptions =
        UdpOptions.builder().setDstPortSpec(new LiteralPortSpec(IntegerSpace.of(53))).build();

    ActionIpAccessListLine line =
        ActionIpAccessListLine.builder()
            .setAction(LineAction.PERMIT)
            .setProtocol(IpProtocol.UDP)
            .setSrcAddressSpec(srcAddressSpec)
            .setDstAddressSpec(dstAddressSpec)
            .setL4Options(udpOptions)
            .setLine(10)
            .setText(LINE_NAME)
            .build();

    accessList.getLines().put(10L, line);

    // Create a mock CiscoNxosConfiguration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);
    when(config.getIpAccessLists()).thenReturn(ImmutableMap.of("test-acl", accessList));

    // Convert the access list
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify the converted rule
    assertThat(rules, hasSize(1));
    AwsSecurityGroupRule rule = rules.get(0);
    assertThat(rule.getProtocol(), equalTo(Protocol.UDP));
    assertThat(rule.getSourceCidr(), equalTo("192.168.1.0/24"));
    assertThat(rule.getFromPort(), equalTo(53));
    assertThat(rule.getToPort(), equalTo(53));
    assertThat(rule.getDescription(), equalTo(LINE_NAME));
  }

  @Test
  public void testConvertIcmpLine() {
    // Create a vendor-specific access list with an ICMP line
    org.batfish.representation.cisco_nxos.IpAccessList accessList =
        new org.batfish.representation.cisco_nxos.IpAccessList("test-acl");

    // Create an ICMP line with type 8 (echo) and code 0
    IpAddressSpec srcAddressSpec = createLiteralIpAddressSpec("192.168.1.0/24");
    IpAddressSpec dstAddressSpec = createLiteralIpAddressSpec("0.0.0.0/0");

    IcmpOptions icmpOptions = new IcmpOptions(8, 0);

    ActionIpAccessListLine line =
        ActionIpAccessListLine.builder()
            .setAction(LineAction.PERMIT)
            .setProtocol(IpProtocol.ICMP)
            .setSrcAddressSpec(srcAddressSpec)
            .setDstAddressSpec(dstAddressSpec)
            .setL4Options(icmpOptions)
            .setLine(10)
            .setText(LINE_NAME)
            .build();

    accessList.getLines().put(10L, line);

    // Create a mock CiscoNxosConfiguration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);
    when(config.getIpAccessLists()).thenReturn(ImmutableMap.of("test-acl", accessList));

    // Convert the access list
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify the converted rule
    assertThat(rules, hasSize(1));
    AwsSecurityGroupRule rule = rules.get(0);
    assertThat(rule.getProtocol(), equalTo(Protocol.ICMP));
    assertThat(rule.getSourceCidr(), equalTo("192.168.1.0/24"));
    assertThat(rule.getIcmpType(), equalTo(8));
    assertThat(rule.getIcmpCode(), equalTo(0));
    assertThat(rule.getDescription(), equalTo(LINE_NAME));
  }

  @Test
  public void testConvertAllProtocolsLine() {
    // Create a vendor-specific access list with a line with no protocol specified
    org.batfish.representation.cisco_nxos.IpAccessList accessList =
        new org.batfish.representation.cisco_nxos.IpAccessList("test-acl");

    // Create a line with no protocol specified
    IpAddressSpec srcAddressSpec = createLiteralIpAddressSpec("192.168.1.0/24");
    IpAddressSpec dstAddressSpec = createLiteralIpAddressSpec("0.0.0.0/0");

    ActionIpAccessListLine line =
        ActionIpAccessListLine.builder()
            .setAction(LineAction.PERMIT)
            .setProtocol(null) // No protocol specified
            .setSrcAddressSpec(srcAddressSpec)
            .setDstAddressSpec(dstAddressSpec)
            .setLine(10)
            .setText(LINE_NAME)
            .build();

    accessList.getLines().put(10L, line);

    // Create a mock CiscoNxosConfiguration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);
    when(config.getIpAccessLists()).thenReturn(ImmutableMap.of("test-acl", accessList));

    // Convert the access list
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify the converted rule
    assertThat(rules, hasSize(1));
    AwsSecurityGroupRule rule = rules.get(0);
    assertThat(rule.getProtocol(), equalTo(Protocol.ALL));
    assertThat(rule.getSourceCidr(), equalTo("192.168.1.0/24"));
    assertThat(rule.getDescription(), equalTo(LINE_NAME));
  }

  @Test
  public void testConvertTcpLineNoPortsSpecified() {
    // Create a vendor-specific access list with a TCP line with no ports specified
    org.batfish.representation.cisco_nxos.IpAccessList accessList =
        new org.batfish.representation.cisco_nxos.IpAccessList("test-acl");

    // Create a TCP line with no ports specified
    IpAddressSpec srcAddressSpec = createLiteralIpAddressSpec("192.168.1.0/24");
    IpAddressSpec dstAddressSpec = createLiteralIpAddressSpec("0.0.0.0/0");

    ActionIpAccessListLine line =
        ActionIpAccessListLine.builder()
            .setAction(LineAction.PERMIT)
            .setProtocol(IpProtocol.TCP)
            .setSrcAddressSpec(srcAddressSpec)
            .setDstAddressSpec(dstAddressSpec)
            .setLine(10)
            .setText(LINE_NAME)
            .build();

    accessList.getLines().put(10L, line);

    // Create a mock CiscoNxosConfiguration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);
    when(config.getIpAccessLists()).thenReturn(ImmutableMap.of("test-acl", accessList));

    // Convert the access list
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify the converted rule
    assertThat(rules, hasSize(1));
    AwsSecurityGroupRule rule = rules.get(0);
    assertThat(rule.getProtocol(), equalTo(Protocol.TCP));
    assertThat(rule.getSourceCidr(), equalTo("192.168.1.0/24"));
    assertThat(rule.getFromPort(), equalTo(1));
    assertThat(rule.getToPort(), equalTo(65535));
    assertThat(rule.getDescription(), equalTo(LINE_NAME));
  }

  @Test
  public void testConvertMultipleProtocolsLine() {
    // Create a vendor-specific access list with multiple protocol lines
    org.batfish.representation.cisco_nxos.IpAccessList accessList =
        new org.batfish.representation.cisco_nxos.IpAccessList("test-acl");

    // Create a TCP line
    IpAddressSpec srcAddressSpec = createLiteralIpAddressSpec("192.168.1.0/24");
    IpAddressSpec dstAddressSpec = createLiteralIpAddressSpec("0.0.0.0/0");

    TcpOptions tcpOptions =
        TcpOptions.builder().setDstPortSpec(new LiteralPortSpec(IntegerSpace.of(80))).build();

    ActionIpAccessListLine tcpLine =
        ActionIpAccessListLine.builder()
            .setAction(LineAction.PERMIT)
            .setProtocol(IpProtocol.TCP)
            .setSrcAddressSpec(srcAddressSpec)
            .setDstAddressSpec(dstAddressSpec)
            .setL4Options(tcpOptions)
            .setLine(10)
            .setText(LINE_NAME)
            .build();

    // Create a UDP line
    UdpOptions udpOptions =
        UdpOptions.builder().setDstPortSpec(new LiteralPortSpec(IntegerSpace.of(80))).build();

    ActionIpAccessListLine udpLine =
        ActionIpAccessListLine.builder()
            .setAction(LineAction.PERMIT)
            .setProtocol(IpProtocol.UDP)
            .setSrcAddressSpec(srcAddressSpec)
            .setDstAddressSpec(dstAddressSpec)
            .setL4Options(udpOptions)
            .setLine(20)
            .setText(LINE_NAME)
            .build();

    accessList.getLines().put(10L, tcpLine);
    accessList.getLines().put(20L, udpLine);

    // Create a mock CiscoNxosConfiguration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);
    when(config.getIpAccessLists()).thenReturn(ImmutableMap.of("test-acl", accessList));

    // Convert the access list
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify the converted rules
    assertThat(rules, hasSize(2));

    // Check that we have one TCP rule and one UDP rule
    assertThat(
        rules.stream()
            .map(AwsSecurityGroupRule::getProtocol)
            .collect(ImmutableList.toImmutableList()),
        containsInAnyOrder(Protocol.TCP, Protocol.UDP));

    // Check that both rules have the correct source CIDR and port
    for (AwsSecurityGroupRule rule : rules) {
      assertThat(rule.getSourceCidr(), equalTo("192.168.1.0/24"));
      assertThat(rule.getFromPort(), equalTo(80));
      assertThat(rule.getToPort(), equalTo(80));
      assertThat(rule.getDescription(), equalTo(LINE_NAME));
    }
  }

  @Test
  public void testConvertVendorSpecificAccessList() {
    // Create a vendor-specific access list with a TCP line
    org.batfish.representation.cisco_nxos.IpAccessList accessList =
        new org.batfish.representation.cisco_nxos.IpAccessList("test-acl");

    // Create a TCP line with port 80
    IpAddressSpec srcAddressSpec = createLiteralIpAddressSpec("192.168.1.0/24");
    IpAddressSpec dstAddressSpec = createLiteralIpAddressSpec("0.0.0.0/0");

    TcpOptions tcpOptions =
        TcpOptions.builder().setDstPortSpec(new LiteralPortSpec(IntegerSpace.of(80))).build();

    ActionIpAccessListLine line =
        ActionIpAccessListLine.builder()
            .setAction(LineAction.PERMIT)
            .setProtocol(IpProtocol.TCP)
            .setSrcAddressSpec(srcAddressSpec)
            .setDstAddressSpec(dstAddressSpec)
            .setL4Options(tcpOptions)
            .setLine(10)
            .setText(LINE_NAME)
            .build();

    accessList.getLines().put(10L, line);

    // Create a mock CiscoNxosConfiguration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);
    when(config.getIpAccessLists()).thenReturn(ImmutableMap.of("test-acl", accessList));

    // Convert the access list
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify the converted rule
    assertThat(rules, hasSize(1));
    AwsSecurityGroupRule rule = rules.get(0);
    assertThat(rule.getProtocol(), equalTo(Protocol.TCP));
    assertThat(rule.getFromPort(), equalTo(80));
    assertThat(rule.getToPort(), equalTo(80));
    assertThat(rule.getDescription(), equalTo(LINE_NAME));
  }

  @Test
  public void testConvertVendorSpecificUdpLine() {
    // Create a vendor-specific access list with a UDP line
    org.batfish.representation.cisco_nxos.IpAccessList accessList =
        new org.batfish.representation.cisco_nxos.IpAccessList("test-acl");

    // Create a UDP line with port 53
    IpAddressSpec srcAddressSpec = createLiteralIpAddressSpec("192.168.1.0/24");
    IpAddressSpec dstAddressSpec = createLiteralIpAddressSpec("0.0.0.0/0");

    UdpOptions udpOptions =
        UdpOptions.builder().setDstPortSpec(new LiteralPortSpec(IntegerSpace.of(53))).build();

    ActionIpAccessListLine line =
        ActionIpAccessListLine.builder()
            .setAction(LineAction.PERMIT)
            .setProtocol(IpProtocol.UDP)
            .setSrcAddressSpec(srcAddressSpec)
            .setDstAddressSpec(dstAddressSpec)
            .setL4Options(udpOptions)
            .setLine(10)
            .setText(LINE_NAME)
            .build();

    accessList.getLines().put(10L, line);

    // Create a mock CiscoNxosConfiguration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);
    when(config.getIpAccessLists()).thenReturn(ImmutableMap.of("test-acl", accessList));

    // Convert the access list
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify the converted rule
    assertThat(rules, hasSize(1));
    AwsSecurityGroupRule rule = rules.get(0);
    assertThat(rule.getProtocol(), equalTo(Protocol.UDP));
    assertThat(rule.getFromPort(), equalTo(53));
    assertThat(rule.getToPort(), equalTo(53));
    assertThat(rule.getDescription(), equalTo(LINE_NAME));
  }

  @Test
  public void testConvertVendorSpecificIcmpLine() {
    // Create a vendor-specific access list with an ICMP line
    org.batfish.representation.cisco_nxos.IpAccessList accessList =
        new org.batfish.representation.cisco_nxos.IpAccessList("test-acl");

    // Create an ICMP line with type 8 (echo) and code 0
    IpAddressSpec srcAddressSpec = createLiteralIpAddressSpec("192.168.1.0/24");
    IpAddressSpec dstAddressSpec = createLiteralIpAddressSpec("0.0.0.0/0");

    IcmpOptions icmpOptions = new IcmpOptions(8, 0);

    ActionIpAccessListLine line =
        ActionIpAccessListLine.builder()
            .setAction(LineAction.PERMIT)
            .setProtocol(IpProtocol.ICMP)
            .setSrcAddressSpec(srcAddressSpec)
            .setDstAddressSpec(dstAddressSpec)
            .setL4Options(icmpOptions)
            .setLine(10)
            .setText(LINE_NAME)
            .build();

    accessList.getLines().put(10L, line);

    // Create a mock CiscoNxosConfiguration
    CiscoNxosConfiguration config = mock(CiscoNxosConfiguration.class);
    when(config.getIpAccessLists()).thenReturn(ImmutableMap.of("test-acl", accessList));

    // Convert the access list
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify the converted rule
    assertThat(rules, hasSize(1));
    AwsSecurityGroupRule rule = rules.get(0);
    assertThat(rule.getProtocol(), equalTo(Protocol.ICMP));
    assertThat(rule.getIcmpType(), equalTo(8));
    assertThat(rule.getIcmpCode(), equalTo(0));
    assertThat(rule.getDescription(), equalTo(LINE_NAME));
  }
}
