package org.batfish.question.awssecuritygroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.IpIpSpace;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpSpaceReference;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardIpSpace;
import org.batfish.datamodel.IpWildcardSetIpSpace;
import org.batfish.datamodel.PrefixIpSpace;
import org.batfish.datamodel.UniverseIpSpace;
import org.batfish.datamodel.visitors.GenericIpSpaceVisitor;
import org.batfish.datamodel.visitors.IpSpaceRepresentative;

/**
 * Visitor that converts an {@link IpSpace} to a CIDR notation string.
 *
 * <p>This visitor handles the following cases:
 *
 * <ul>
 *   <li>PrefixIpSpace - converts to prefix notation (e.g., "192.168.1.0/24")
 *   <li>IpIpSpace - converts to host prefix notation (e.g., "192.168.1.1/32")
 *   <li>IpWildcardIpSpace - converts to prefix notation if possible, otherwise uses a
 *       representative IP
 *   <li>UniverseIpSpace - returns "0.0.0.0/0"
 *   <li>EmptyIpSpace - returns null
 *   <li>Other IpSpace types - attempts to find a representative IP and returns it with /32
 * </ul>
 */
public class IpSpaceToCidr implements GenericIpSpaceVisitor<String> {

  private static final String DEFAULT_CIDR = "0.0.0.0/0";
  private static final IpSpaceRepresentative IP_SPACE_REPRESENTATIVE = new IpSpaceRepresentative();

  /**
   * Converts an IpSpace to a CIDR notation string.
   *
   * @param ipSpace The IpSpace to convert
   * @return The CIDR notation string, or null if the IpSpace cannot be represented as a CIDR (e.g.,
   *     EmptyIpSpace or null)
   */
  public static @Nullable String toCidr(@Nullable IpSpace ipSpace) {
    if (ipSpace == null) {
      return null;
    }
    return ipSpace.accept(new IpSpaceToCidr());
  }

  @Override
  public @Nullable String visitAclIpSpace(AclIpSpace aclIpSpace) {
    // For AclIpSpace, try to get a representative IP
    return getRepresentativeIp(aclIpSpace);
  }

  @Override
  public String visitEmptyIpSpace(EmptyIpSpace emptyIpSpace) {
    // Empty space doesn't have a CIDR representation
    return null;
  }

  @Override
  public String visitIpIpSpace(IpIpSpace ipIpSpace) {
    // For a single IP, use /32 prefix
    return ipIpSpace.getIp().toString() + "/32";
  }

  @Override
  public String visitIpSpaceReference(IpSpaceReference ipSpaceReference) {
    // We can't resolve references here, so return the default
    return DEFAULT_CIDR;
  }

  @Override
  public String visitIpWildcardIpSpace(IpWildcardIpSpace ipWildcardIpSpace) {
    IpWildcard ipWildcard = ipWildcardIpSpace.getIpWildcard();

    // If the wildcard represents a prefix, use that
    if (ipWildcard.isPrefix()) {
      return ipWildcard.toPrefix().toString();
    }

    // Otherwise, get a representative IP
    return getRepresentativeIp(ipWildcardIpSpace);
  }

  @Override
  public String visitIpWildcardSetIpSpace(IpWildcardSetIpSpace ipWildcardSetIpSpace) {
    // For wildcard sets, try to get a representative IP
    return getRepresentativeIp(ipWildcardSetIpSpace);
  }

  @Override
  public String visitPrefixIpSpace(PrefixIpSpace prefixIpSpace) {
    // For prefix space, use the prefix directly
    return prefixIpSpace.getPrefix().toString();
  }

  @Override
  public String visitUniverseIpSpace(UniverseIpSpace universeIpSpace) {
    // Universe represents all IPs
    return DEFAULT_CIDR;
  }

  /** Gets a representative IP from the given IpSpace and formats it as a /32 CIDR. */
  private @Nullable String getRepresentativeIp(@Nonnull IpSpace ipSpace) {
    return IP_SPACE_REPRESENTATIVE
        .getRepresentative(ipSpace)
        .map(ip -> ip.toString() + "/32")
        .orElse(null);
  }
}
