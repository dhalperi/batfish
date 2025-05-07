package org.batfish.question.awssecuritygroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.representation.cisco_nxos.AddrGroupIpAddressSpec;
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.representation.cisco_nxos.IpAddressSpec;
import org.batfish.representation.cisco_nxos.IpAddressSpecVisitor;
import org.batfish.representation.cisco_nxos.LiteralIpAddressSpec;
import org.batfish.representation.cisco_nxos.ObjectGroup;
import org.batfish.representation.cisco_nxos.ObjectGroupIpAddress;
import org.batfish.representation.cisco_nxos.ObjectGroupIpAddressLine;

/**
 * Visitor that converts an {@link IpAddressSpec} to an {@link IpSpace}.
 *
 * <p>This visitor handles the following cases:
 *
 * <ul>
 *   <li>LiteralIpAddressSpec - returns the contained IpSpace directly
 *   <li>AddrGroupIpAddressSpec - resolves the address group from the configuration and returns the
 *       combined IpSpace
 * </ul>
 */
public class IpAddressSpecToIpSpace implements IpAddressSpecVisitor<IpSpace> {

  private static final String DEFAULT_CIDR = "0.0.0.0/0";
  private final CiscoNxosConfiguration _config;

  public IpAddressSpecToIpSpace(CiscoNxosConfiguration config) {
    _config = config;
  }

  /**
   * Converts an IpAddressSpec to an IpSpace.
   *
   * @param config The vendor-specific configuration for resolving address groups
   * @param addressSpec The address specification
   * @return The corresponding IpSpace, or a default "any" IpSpace if the specification cannot be
   *     resolved
   */
  /**
   * Converts an IpAddressSpec to an IpSpace.
   *
   * @param config The vendor-specific configuration for resolving address groups
   * @param addressSpec The address specification
   * @return The corresponding IpSpace, or null if the specification is null
   */
  public static @Nullable IpSpace toIpSpace(
      @Nonnull CiscoNxosConfiguration config, @Nullable IpAddressSpec addressSpec) {
    if (addressSpec == null) {
      return null;
    }
    return addressSpec.accept(new IpAddressSpecToIpSpace(config));
  }

  @Override
  public IpSpace visitLiteralIpAddressSpec(LiteralIpAddressSpec literalIpAddressSpec) {
    return literalIpAddressSpec.getIpSpace();
  }

  @Override
  public IpSpace visitAddrGroupIpAddressSpec(AddrGroupIpAddressSpec addrGroupIpAddressSpec) {
    String groupName = addrGroupIpAddressSpec.getName();

    // Resolve the address group from the configuration
    ObjectGroup objectGroup = _config.getObjectGroups().get(groupName);
    if (objectGroup instanceof ObjectGroupIpAddress) {
      ObjectGroupIpAddress ipAddressGroup = (ObjectGroupIpAddress) objectGroup;

      // Combine all IP wildcards in the group into a single IpSpace
      return AclIpSpace.permitting(
              ipAddressGroup.getLines().values().stream()
                  .map(ObjectGroupIpAddressLine::getIpWildcard)
                  .map(IpWildcard::toIpSpace))
          .build();
    }

    // If we couldn't resolve the group or it's not an IP address group, return null
    return null;
  }
}
