package org.batfish.question.awssecuritygroup;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.batfish.datamodel.AclIpSpace;
import org.batfish.datamodel.EmptyIpSpace;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.IpIpSpace;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.IpWildcardIpSpace;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.PrefixIpSpace;
import org.batfish.datamodel.UniverseIpSpace;
import org.junit.Test;

/** Tests for {@link IpSpaceToCidr}. */
public class IpSpaceToCidrTest {

  @Test
  public void testVisitPrefixIpSpace() {
    PrefixIpSpace prefixIpSpace = (PrefixIpSpace) Prefix.parse("192.168.1.0/24").toIpSpace();
    assertThat(prefixIpSpace.accept(new IpSpaceToCidr()), equalTo("192.168.1.0/24"));
  }

  @Test
  public void testVisitIpIpSpace() {
    IpIpSpace ipIpSpace = (IpIpSpace) Ip.parse("192.168.1.1").toIpSpace();
    assertThat(ipIpSpace.accept(new IpSpaceToCidr()), equalTo("192.168.1.1/32"));
  }

  @Test
  public void testVisitIpWildcardIpSpace_prefix() {
    // IpWildcard that represents a prefix
    IpWildcardIpSpace ipWildcardIpSpace =
        (IpWildcardIpSpace) IpWildcard.parse("192.168.1.0/24").toIpSpace();
    assertThat(ipWildcardIpSpace.accept(new IpSpaceToCidr()), equalTo("192.168.1.0/24"));
  }

  @Test
  public void testVisitIpWildcardIpSpace_nonPrefix() {
    // IpWildcard that doesn't represent a prefix
    IpWildcardIpSpace ipWildcardIpSpace =
        (IpWildcardIpSpace)
            IpWildcard.ipWithWildcardMask(Ip.parse("192.168.1.0"), Ip.parse("0.0.0.15"))
                .toIpSpace();

    // Should return a valid CIDR
    String result = ipWildcardIpSpace.accept(new IpSpaceToCidr());
    assertThat(result, notNullValue());
    assertThat(result.contains("/"), equalTo(true));
  }

  @Test
  public void testVisitUniverseIpSpace() {
    assertThat(UniverseIpSpace.INSTANCE.accept(new IpSpaceToCidr()), equalTo("0.0.0.0/0"));
  }

  @Test
  public void testVisitEmptyIpSpace() {
    assertThat(EmptyIpSpace.INSTANCE.accept(new IpSpaceToCidr()), nullValue());
  }

  @Test
  public void testVisitAclIpSpace() {
    // Simple AclIpSpace with a single prefix
    IpSpace prefixIpSpace = Prefix.parse("192.168.1.0/24").toIpSpace();
    IpSpace aclIpSpace = AclIpSpace.builder().thenPermitting(prefixIpSpace).build();

    // Should return a valid CIDR
    String result = aclIpSpace.accept(new IpSpaceToCidr());
    assertThat(result, notNullValue());
    assertThat(result.contains("/"), equalTo(true));
  }

  @Test
  public void testToCidr_null() {
    assertThat(IpSpaceToCidr.toCidr(null), nullValue());
  }

  @Test
  public void testToCidr_emptyIpSpace() {
    assertThat(IpSpaceToCidr.toCidr(EmptyIpSpace.INSTANCE), nullValue());
  }
}
