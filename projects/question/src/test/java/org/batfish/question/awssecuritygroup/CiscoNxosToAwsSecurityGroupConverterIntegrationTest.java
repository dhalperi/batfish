package org.batfish.question.awssecuritygroup;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.batfish.common.util.Resources.readResource;
import static org.batfish.main.BatfishTestUtils.DUMMY_SNAPSHOT_1;
import static org.batfish.main.BatfishTestUtils.configureBatfishTestSettings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.SerializationUtils;
import org.batfish.common.BatfishLogger;
import org.batfish.common.Warnings;
import org.batfish.common.plugin.IBatfish;
import org.batfish.config.Settings;
import org.batfish.datamodel.Configuration;
import org.batfish.grammar.cisco_nxos.CiscoNxosCombinedParser;
import org.batfish.grammar.cisco_nxos.NxosControlPlaneExtractor;
import org.batfish.grammar.silent_syntax.SilentSyntaxCollection;
import org.batfish.main.Batfish;
import org.batfish.main.BatfishTestUtils;
import org.batfish.question.awssecuritygroup.AwsSecurityGroupRule.Protocol;
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.representation.cisco_nxos.IpAccessList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/** Integration tests for {@link CiscoNxosToAwsSecurityGroupConverter}. */
@ParametersAreNonnullByDefault
public final class CiscoNxosToAwsSecurityGroupConverterIntegrationTest {

  private static final String TESTCONFIGS_PREFIX =
      "org/batfish/question/awssecuritygroup/testconfigs/";

  @Rule public TemporaryFolder _folder = new TemporaryFolder();
  @Rule public ExpectedException _thrown = ExpectedException.none();

  private @Nonnull Batfish getBatfishForConfigurationNames(String... configurationNames)
      throws IOException {
    String[] names =
        Arrays.stream(configurationNames).map(s -> TESTCONFIGS_PREFIX + s).toArray(String[]::new);
    Batfish batfish = BatfishTestUtils.getBatfishForTextConfigs(_folder, names);
    return batfish;
  }

  private @Nonnull Configuration parseConfig(String hostname) throws IOException {
    Map<String, Configuration> configs = parseTextConfigs(hostname);
    String canonicalHostname = hostname.toLowerCase();
    assertThat(configs.get(canonicalHostname), notNullValue());
    return configs.get(canonicalHostname);
  }

  private @Nonnull Map<String, Configuration> parseTextConfigs(String... configurationNames)
      throws IOException {
    IBatfish iBatfish = getBatfishForConfigurationNames(configurationNames);
    return iBatfish.loadConfigurations(iBatfish.getSnapshot());
  }

  private @Nonnull CiscoNxosConfiguration parseVendorConfig(String hostname) {
    String src = readResource(TESTCONFIGS_PREFIX + hostname, UTF_8);
    Settings settings = new Settings();
    configureBatfishTestSettings(settings);
    CiscoNxosCombinedParser ciscoNxosParser = new CiscoNxosCombinedParser(src, settings);
    NxosControlPlaneExtractor extractor =
        new NxosControlPlaneExtractor(
            src, ciscoNxosParser, new Warnings(), new SilentSyntaxCollection());
    ParserRuleContext tree =
        Batfish.parse(
            ciscoNxosParser, new BatfishLogger(BatfishLogger.LEVELSTR_FATAL, false), settings);
    extractor.processParseTree(DUMMY_SNAPSHOT_1, tree);
    CiscoNxosConfiguration vendorConfiguration =
        (CiscoNxosConfiguration) extractor.getVendorConfiguration();
    assertThat(vendorConfiguration, notNullValue());
    vendorConfiguration.setFilename(TESTCONFIGS_PREFIX + hostname);
    // crash if not serializable
    return SerializationUtils.clone(vendorConfiguration);
  }

  @Test
  public void testConvertTcpAccessList() {
    // Parse the vendor configuration
    CiscoNxosConfiguration config = parseVendorConfig("nxos_aws_tcp_acl");

    // Get the access list
    IpAccessList accessList = config.getIpAccessLists().get("acl_tcp_simple");
    assertThat(accessList, notNullValue());

    // Convert the access list to AWS security group rules
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify we have the expected number of rules
    assertThat(rules, hasSize(3));

    // Verify rules contain the expected properties
    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.TCP)),
                hasProperty("sourceCidr", equalTo("192.168.1.0/24")),
                hasProperty("fromPort", equalTo(80)),
                hasProperty("toPort", equalTo(80)))));

    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.TCP)),
                hasProperty("sourceCidr", equalTo("10.0.0.0/8")),
                hasProperty("fromPort", equalTo(443)),
                hasProperty("toPort", equalTo(443)))));

    // Find a rule with TCP protocol and port 22
    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.TCP)),
                hasProperty("fromPort", equalTo(22)),
                hasProperty("toPort", equalTo(22)))));
  }

  @Test
  public void testConvertTcpPortRangeAccessList() {
    // Parse the vendor configuration
    CiscoNxosConfiguration config = parseVendorConfig("nxos_aws_tcp_acl");

    // Get the access list
    IpAccessList accessList = config.getIpAccessLists().get("acl_tcp_port_ranges");
    assertThat(accessList, notNullValue());

    // Convert the access list to AWS security group rules
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify we have the expected number of rules
    assertThat(rules, hasSize(2));

    // Verify rules contain the expected properties
    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.TCP)),
                hasProperty("sourceCidr", equalTo("192.168.1.0/24")),
                hasProperty("fromPort", equalTo(8000)),
                hasProperty("toPort", equalTo(9000)))));

    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.TCP)),
                hasProperty("sourceCidr", equalTo("10.0.0.0/8")),
                hasProperty("fromPort", equalTo(1024)),
                hasProperty("toPort", equalTo(2048)))));
  }

  @Test
  public void testConvertUdpAccessList() {
    // Parse the vendor configuration
    CiscoNxosConfiguration config = parseVendorConfig("nxos_aws_udp_icmp_acl");

    // Get the access list
    IpAccessList accessList = config.getIpAccessLists().get("acl_udp_simple");
    assertThat(accessList, notNullValue());

    // Convert the access list to AWS security group rules
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify we have the expected number of rules
    assertThat(rules, hasSize(3));

    // Verify rules contain the expected properties
    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.UDP)),
                hasProperty("sourceCidr", equalTo("192.168.1.0/24")),
                hasProperty("fromPort", equalTo(53)),
                hasProperty("toPort", equalTo(53)))));

    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.UDP)),
                hasProperty("sourceCidr", equalTo("10.0.0.0/8")),
                hasProperty("fromPort", equalTo(123)),
                hasProperty("toPort", equalTo(123)))));

    // Find a rule with UDP protocol and port 161
    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.UDP)),
                hasProperty("fromPort", equalTo(161)),
                hasProperty("toPort", equalTo(161)))));
  }

  @Test
  public void testConvertIcmpAccessList() {
    // Parse the vendor configuration
    CiscoNxosConfiguration config = parseVendorConfig("nxos_aws_udp_icmp_acl");

    // Get the access list
    IpAccessList accessList = config.getIpAccessLists().get("acl_icmp_simple");
    assertThat(accessList, notNullValue());

    // Convert the access list to AWS security group rules
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify we have the expected number of rules
    assertThat(rules, hasSize(3));

    // Verify rules contain the expected properties
    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.ICMP)),
                hasProperty("sourceCidr", equalTo("192.168.1.0/24")),
                hasProperty("icmpType", equalTo(8)),
                hasProperty("icmpCode", equalTo(0)))));

    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.ICMP)),
                hasProperty("sourceCidr", equalTo("10.0.0.0/8")),
                hasProperty("icmpType", equalTo(0)),
                hasProperty("icmpCode", equalTo(0)))));

    // Find a rule with ICMP protocol and type 8, code 0
    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.ICMP)),
                hasProperty("icmpType", equalTo(8)),
                hasProperty("icmpCode", equalTo(0)))));
  }

  @Test
  public void testConvertMixedProtocolsAccessList() {
    // Parse the vendor configuration
    CiscoNxosConfiguration config = parseVendorConfig("nxos_aws_mixed_acl");

    // Get the access list
    IpAccessList accessList = config.getIpAccessLists().get("acl_mixed_protocols");
    assertThat(accessList, notNullValue());

    // Convert the access list to AWS security group rules
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify we have the expected number of rules
    assertThat(rules, hasSize(4));

    // Verify rules contain the expected properties
    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.TCP)),
                hasProperty("sourceCidr", equalTo("192.168.1.0/24")),
                hasProperty("fromPort", equalTo(80)),
                hasProperty("toPort", equalTo(80)))));

    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.UDP)),
                hasProperty("sourceCidr", equalTo("192.168.1.0/24")),
                hasProperty("fromPort", equalTo(53)),
                hasProperty("toPort", equalTo(53)))));

    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.ICMP)),
                hasProperty("sourceCidr", equalTo("192.168.1.0/24")),
                hasProperty("icmpType", equalTo(8)),
                hasProperty("icmpCode", equalTo(0)))));

    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.ALL)),
                hasProperty("sourceCidr", equalTo("192.168.1.0/24")))));
  }

  @Test
  public void testConvertComplexAccessList() {
    // Parse the vendor configuration
    CiscoNxosConfiguration config = parseVendorConfig("nxos_aws_mixed_acl");

    // Get the access list
    IpAccessList accessList = config.getIpAccessLists().get("acl_complex");
    assertThat(accessList, notNullValue());

    // Convert the access list to AWS security group rules
    CiscoNxosToAwsSecurityGroupConverter converter = new CiscoNxosToAwsSecurityGroupConverter();
    List<AwsSecurityGroupRule> rules = converter.convertAccessList(config, accessList);

    // Verify we have the expected number of rules - only permit rules should be converted
    assertThat(rules, hasSize(3));

    // Verify rules contain the expected properties
    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.TCP)),
                hasProperty("sourceCidr", equalTo("10.0.0.0/8")),
                hasProperty("fromPort", equalTo(1024)),
                hasProperty("toPort", equalTo(2048)))));

    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.UDP)),
                hasProperty("sourceCidr", equalTo("10.0.0.0/8")),
                hasProperty("fromPort", equalTo(1024)),
                hasProperty("toPort", equalTo(2048)))));

    assertThat(
        rules,
        hasItem(
            allOf(
                hasProperty("protocol", equalTo(Protocol.ICMP)),
                hasProperty("sourceCidr", equalTo("10.0.0.0/8")),
                hasProperty("icmpType", equalTo(8)),
                hasProperty("icmpCode", equalTo(0)))));

    // Verify that the deny rule is not converted (AWS security groups are allow-only)
    assertThat(
        rules,
        not(
            hasItem(
                AwsSecurityGroupRule.builder()
                    .setProtocol(Protocol.ALL)
                    .setSourceCidr("0.0.0.0/0")
                    .build())));
  }
}
