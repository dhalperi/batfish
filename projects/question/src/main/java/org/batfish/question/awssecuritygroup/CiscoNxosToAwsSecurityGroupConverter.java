package org.batfish.question.awssecuritygroup;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.SubRange;
import org.batfish.question.awssecuritygroup.AwsSecurityGroupRule.Protocol;
import org.batfish.representation.cisco_nxos.ActionIpAccessListLine;
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.representation.cisco_nxos.IcmpOptions;
import org.batfish.representation.cisco_nxos.IpAccessList;
import org.batfish.representation.cisco_nxos.IpAccessListLine;
import org.batfish.representation.cisco_nxos.IpAddressSpec;
import org.batfish.representation.cisco_nxos.Layer4Options;
import org.batfish.representation.cisco_nxos.LiteralPortSpec;
import org.batfish.representation.cisco_nxos.PortSpec;
import org.batfish.representation.cisco_nxos.TcpOptions;
import org.batfish.representation.cisco_nxos.UdpOptions;

/** Converts Cisco NX-OS access lists to AWS security group rules. */
public class CiscoNxosToAwsSecurityGroupConverter {

  // Constants for port ranges
  private static final int MIN_PORT = 1;
  private static final int MAX_PORT = 65535;

  // Constants for ICMP defaults
  private static final int DEFAULT_ICMP_TYPE = 8; // Echo Request
  private static final int DEFAULT_ICMP_CODE = 0;

  // Constants for CIDR patterns
  private static final String DEFAULT_CIDR = "0.0.0.0/0";

  /**
   * Converts a vendor-specific access list to a list of AWS security group rules.
   *
   * <p>This method processes each line in the provided access list and converts permit lines to
   * appropriate AWS security group rules. Deny lines are skipped as AWS security groups are
   * allow-only.
   *
   * @param config The vendor-specific configuration containing the access list
   * @param accessList The vendor-specific access list to convert
   * @return A list of AWS security group rules
   */
  public List<AwsSecurityGroupRule> convertAccessList(
      CiscoNxosConfiguration config, IpAccessList accessList) {

    // General implementation for all access lists
    List<AwsSecurityGroupRule> rules = new ArrayList<>();

    // Process each line in the vendor-specific access list
    for (Map.Entry<Long, IpAccessListLine> entry : accessList.getLines().entrySet()) {
      IpAccessListLine line = entry.getValue();

      // Currently we only support ActionIpAccessListLine
      if (line instanceof ActionIpAccessListLine) {
        ActionIpAccessListLine actionLine = (ActionIpAccessListLine) line;

        // Skip deny lines - AWS security groups are allow-only
        if (actionLine.getAction() != LineAction.PERMIT) {
          continue;
        }

        // Convert the line directly to AWS security group rules
        List<AwsSecurityGroupRule> convertedRules = convertActionLine(config, actionLine);
        rules.addAll(convertedRules);
      }
    }

    return rules;
  }

  /**
   * Converts an ActionIpAccessListLine directly to AWS security group rules.
   *
   * <p>This method handles the conversion of a single access list line to one or more AWS security
   * group rules based on the protocol and other parameters specified in the line.
   *
   * @param config The vendor-specific configuration for resolving address groups
   * @param line The vendor-specific access list line
   * @return A list of AWS security group rules
   */
  private List<AwsSecurityGroupRule> convertActionLine(
      CiscoNxosConfiguration config, ActionIpAccessListLine line) {
    List<AwsSecurityGroupRule> rules = new ArrayList<>();

    // Get source CIDR
    String sourceCidr = getSourceCidrFromAddressSpec(config, line.getSrcAddressSpec());
    if (sourceCidr == null) {
      // If we can't determine the source CIDR, skip this line
      return rules;
    }

    // Get protocol
    IpProtocol protocol = line.getProtocol();
    if (protocol == null) {
      // If no protocol is specified, create an "all protocols" rule
      rules.add(
          AwsSecurityGroupRule.builder()
              .setProtocol(Protocol.ALL)
              .setSourceCidr(sourceCidr)
              .setDescription(line.getText())
              .build());
      return rules;
    }

    // Process based on protocol
    switch (protocol) {
      case TCP:
        rules.addAll(convertL4ProtocolActionLine(line, sourceCidr, Protocol.TCP));
        break;
      case UDP:
        rules.addAll(convertL4ProtocolActionLine(line, sourceCidr, Protocol.UDP));
        break;
      case ICMP:
        rules.add(convertIcmpActionLine(line, sourceCidr));
        break;
      default:
        // For other protocols, create a rule with just the protocol
        rules.add(
            AwsSecurityGroupRule.builder()
                .setProtocol(mapProtocol(protocol))
                .setSourceCidr(sourceCidr)
                .setDescription(line.getText())
                .build());
        break;
    }

    return rules;
  }

  /**
   * Converts an IpAddressSpec to an IpSpace using the IpAddressSpecVisitor pattern.
   *
   * <p>This method delegates to the IpAddressSpecToIpSpace visitor to handle different types of
   * address specifications in a type-safe way.
   *
   * @param config The vendor-specific configuration for resolving address groups
   * @param addressSpec The address specification
   * @return The corresponding IpSpace, or a default "any" IpSpace if the specification cannot be
   *     resolved
   */
  private IpSpace convertAddressSpecToIpSpace(
      CiscoNxosConfiguration config, IpAddressSpec addressSpec) {
    return IpAddressSpecToIpSpace.toIpSpace(config, addressSpec);
  }

  /**
   * Converts a PortSpec to a list of SubRanges.
   *
   * <p>This method extracts port ranges from a port specification:
   *
   * <ul>
   *   <li>For literal port specifications, the specified ports are returned as SubRanges
   *   <li>For other port specification types or null, the default port range (1-65535) is returned
   * </ul>
   *
   * @param portSpec The port specification
   * @return A list of SubRanges representing the port ranges
   */
  private List<SubRange> convertPortSpecToSubRanges(PortSpec portSpec) {
    if (portSpec instanceof LiteralPortSpec) {
      LiteralPortSpec literalPortSpec = (LiteralPortSpec) portSpec;
      // Convert IntegerSpace to SubRanges
      return new ArrayList<>(literalPortSpec.getPorts().getSubRanges());
    }

    // Default to all ports if we can't determine the specific ports
    return ImmutableList.of(new SubRange(MIN_PORT, MAX_PORT));
  }

  /**
   * Gets the source CIDR block from an IpAddressSpec.
   *
   * <p>This method attempts to extract a CIDR block representation from an IP address
   * specification:
   *
   * <ul>
   *   <li>First converts the address spec to an IpSpace
   *   <li>Checks for special cases like "any" or "0.0.0.0/0"
   *   <li>Attempts to extract CIDR notation using pattern matching
   *   <li>Falls back to using a representative IP with /32 mask
   *   <li>Returns the default CIDR "0.0.0.0/0" if all else fails
   * </ul>
   *
   * @param config The vendor-specific configuration for resolving address groups
   * @param addressSpec The address specification
   * @return The source CIDR block, or null if it can't be determined
   */
  @Nullable
  private String getSourceCidrFromAddressSpec(
      CiscoNxosConfiguration config, IpAddressSpec addressSpec) {
    if (addressSpec == null) {
      return null;
    }

    // Convert to IpSpace first
    IpSpace ipSpace = convertAddressSpecToIpSpace(config, addressSpec);
    if (ipSpace == null) {
      return null;
    }

    // Convert to CIDR
    String cidr = ipSpaceToCidr(ipSpace);
    if (cidr == null) {
      // Skip lines with IpSpaces that can't be represented as CIDRs (like EmptyIpSpace)
      return null;
    }

    return cidr;
  }

  /**
   * Converts an IpSpace to a CIDR notation string using the IpSpaceToCidr visitor.
   *
   * <p>This method delegates to the IpSpaceToCidr visitor to handle different types of IpSpace in a
   * type-safe way.
   *
   * @param ipSpace The IpSpace to convert
   * @return A CIDR notation string representing the IpSpace
   */
  private String ipSpaceToCidr(IpSpace ipSpace) {
    return IpSpaceToCidr.toCidr(ipSpace);
  }

  /**
   * Maps an IP protocol to an AWS security group protocol.
   *
   * <p>This method translates standard IP protocols to their AWS security group protocol
   * equivalents.
   *
   * @param protocol The IP protocol
   * @return The corresponding AWS security group protocol
   */
  @Nonnull
  private Protocol mapProtocol(IpProtocol protocol) {
    switch (protocol) {
      case TCP:
        return Protocol.TCP;
      case UDP:
        return Protocol.UDP;
      case ICMP:
        return Protocol.ICMP;
      default:
        return Protocol.ALL;
    }
  }

  /**
   * Converts a TCP or UDP ActionIpAccessListLine to AWS security group rules.
   *
   * <p>This method handles the conversion of TCP and UDP protocol lines to AWS security group
   * rules:
   *
   * <ul>
   *   <li>Extracts layer 4 options and port specifications
   *   <li>Creates rules with appropriate port ranges
   *   <li>Uses default port range (1-65535) if no specific ports are specified
   * </ul>
   *
   * @param line The access list line
   * @param sourceCidr The source CIDR block
   * @param protocol The AWS security group protocol (TCP or UDP)
   * @return A list of AWS security group rules
   */
  private List<AwsSecurityGroupRule> convertL4ProtocolActionLine(
      ActionIpAccessListLine line, String sourceCidr, Protocol protocol) {
    List<AwsSecurityGroupRule> rules = new ArrayList<>();

    // Get Layer4Options
    Layer4Options l4Options = line.getL4Options();
    PortSpec dstPortSpec = null;

    // Extract the destination port spec based on protocol
    if (protocol == Protocol.TCP && l4Options instanceof TcpOptions) {
      dstPortSpec = ((TcpOptions) l4Options).getDstPortSpec();
    } else if (protocol == Protocol.UDP && l4Options instanceof UdpOptions) {
      dstPortSpec = ((UdpOptions) l4Options).getDstPortSpec();
    }

    // If no valid options or port spec, create a rule for all ports
    if (dstPortSpec == null) {
      rules.add(
          AwsSecurityGroupRule.builder()
              .setProtocol(protocol)
              .setSourceCidr(sourceCidr)
              .setPortRange(MIN_PORT, MAX_PORT)
              .setDescription(line.getText())
              .build());
      return rules;
    }

    // Convert port spec to sub-ranges
    List<SubRange> portRanges = convertPortSpecToSubRanges(dstPortSpec);

    // Create a rule for each destination port range
    for (SubRange portRange : portRanges) {
      rules.add(
          AwsSecurityGroupRule.builder()
              .setProtocol(protocol)
              .setSourceCidr(sourceCidr)
              .setPortRange(portRange.getStart(), portRange.getEnd())
              .setDescription(line.getText())
              .build());
    }

    return rules;
  }

  /**
   * Converts an ICMP ActionIpAccessListLine to an AWS security group rule.
   *
   * <p>This method handles the conversion of ICMP protocol lines to AWS security group rules:
   *
   * <ul>
   *   <li>Extracts ICMP type and code if specified
   *   <li>Uses configurable defaults (type 8, code 0 - echo request) if not specified
   * </ul>
   *
   * @param line The access list line
   * @param sourceCidr The source CIDR block
   * @return An AWS security group rule for ICMP
   */
  private AwsSecurityGroupRule convertIcmpActionLine(
      ActionIpAccessListLine line, String sourceCidr) {
    // Get Layer4Options
    Layer4Options l4Options = line.getL4Options();

    AwsSecurityGroupRule.Builder builder =
        AwsSecurityGroupRule.builder()
            .setProtocol(Protocol.ICMP)
            .setSourceCidr(sourceCidr)
            .setDescription(line.getText());

    // Set ICMP type and code if specified
    if (l4Options instanceof IcmpOptions) {
      IcmpOptions icmpOptions = (IcmpOptions) l4Options;
      builder.setIcmpType(icmpOptions.getType());

      if (icmpOptions.getCode() != null) {
        builder.setIcmpCode(icmpOptions.getCode());
      } else {
        // Default code to 0 if not specified
        builder.setIcmpCode(DEFAULT_ICMP_CODE);
      }
    } else {
      // If no ICMP options are specified, use echo request (type 8, code 0) as a default
      builder.setIcmpType(DEFAULT_ICMP_TYPE).setIcmpCode(DEFAULT_ICMP_CODE);
    }

    return builder.build();
  }
}
