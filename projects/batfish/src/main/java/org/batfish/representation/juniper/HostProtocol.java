package org.batfish.representation.juniper;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.function.Supplier;
import org.batfish.common.BatfishException;
import org.batfish.datamodel.HeaderSpace;
import org.batfish.datamodel.IcmpType;
import org.batfish.datamodel.IpProtocol;
import org.batfish.datamodel.IpWildcard;
import org.batfish.datamodel.NamedPort;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.acl.AclLineMatchExpr;
import org.batfish.datamodel.acl.FalseExpr;
import org.batfish.datamodel.acl.MatchHeaderSpace;
import org.batfish.datamodel.acl.OrMatchExpr;

public enum HostProtocol {
  ALL,
  BFD,
  BGP,
  DVMRP,
  IGMP,
  LDP,
  MSDP,
  NHRP,
  OSPF,
  OSPF3,
  PGM,
  PIM,
  RIP,
  RIPNG,
  ROUTER_DISCOVERY,
  RSVP,
  SAP,
  VRRP;

  private final Supplier<AclLineMatchExpr> _match;

  HostProtocol() {
    _match = Suppliers.memoize(this::init);
  }

  public AclLineMatchExpr getMatchCondition() {
    return _match.get();
  }

  private AclLineMatchExpr init() {
    HeaderSpace.Builder headerSpaceBuilder = HeaderSpace.builder();
    switch (this) {
      case ALL:
        return new OrMatchExpr(
            Arrays.stream(values())
                .filter(v -> v != ALL)
                .map(HostProtocol::getMatchCondition)
                .collect(ImmutableList.toImmutableList()),
            "host-inbound-traffic protocols all");
      case BFD:
        return new MatchHeaderSpace(
            headerSpaceBuilder
                .setIpProtocols(IpProtocol.TCP, IpProtocol.UDP)
                .setDstPorts(NamedPort.BFD_CONTROL, NamedPort.BFD_ECHO)
                .build(),
            "bfd");
      case BGP:
        return new MatchHeaderSpace(
            headerSpaceBuilder.setIpProtocols(IpProtocol.TCP).setDstPorts(NamedPort.BGP).build(),
            "bgp");
      case DVMRP:
        // TODO: DVMRP uses IGMP (an IP Protocol) type 3. need to add support for IGMP types in
        //  packet headers
        return new MatchHeaderSpace(
            headerSpaceBuilder.setIpProtocols(IpProtocol.IGMP).build(), "dvmrp");
      case IGMP:
        return new MatchHeaderSpace(
            headerSpaceBuilder.setIpProtocols(IpProtocol.IGMP).build(), "igmp");
      case LDP:
        return new MatchHeaderSpace(
            headerSpaceBuilder
                .setIpProtocols(IpProtocol.TCP, IpProtocol.UDP)
                .setDstPorts(NamedPort.LDP)
                .build(),
            "ldp");
      case MSDP:
        return new MatchHeaderSpace(
            headerSpaceBuilder.setIpProtocols(IpProtocol.TCP).setDstPorts(NamedPort.MSDP).build(),
            "msdp");
      case NHRP:
        return new MatchHeaderSpace(
            headerSpaceBuilder.setIpProtocols(IpProtocol.NARP).build(), "narp");
      case OSPF:
        return new MatchHeaderSpace(
            headerSpaceBuilder.setIpProtocols(IpProtocol.OSPF).build(), "ospf");
      case OSPF3:
        // TODO: OSPFv3 is an IPV6-encapsulated protocol
        return FalseExpr.INSTANCE;
      case PGM:
        return new MatchHeaderSpace(
            headerSpaceBuilder.setIpProtocols(IpProtocol.PGM).build(), "pgm");
      case PIM:
        return new MatchHeaderSpace(
            headerSpaceBuilder.setIpProtocols(IpProtocol.PIM).build(), "pim");
      case RIP:
        return new MatchHeaderSpace(
            headerSpaceBuilder.setIpProtocols(IpProtocol.UDP).setDstPorts(NamedPort.RIP).build(),
            "rip");
      case RIPNG:
        // TODO: RIPng is an IPV6-encapsulated protocol
        return FalseExpr.INSTANCE;
      case ROUTER_DISCOVERY:
        return new MatchHeaderSpace(
            headerSpaceBuilder
                .setIpProtocols(IpProtocol.ICMP)
                .setIcmpTypes(IcmpType.ROUTER_ADVERTISEMENT, IcmpType.ROUTER_SOLICITATION)
                .build(),
            "router-discovery");
      case RSVP:
        return new MatchHeaderSpace(
            headerSpaceBuilder.setIpProtocols(IpProtocol.RSVP, IpProtocol.RSVP_E2E_IGNORE).build(),
            "rsvp");
      case SAP:
        return new MatchHeaderSpace(
            headerSpaceBuilder
                .setIpProtocols(IpProtocol.UDP)
                .setDstPorts(NamedPort.SAP)
                .setDstIps(ImmutableSet.of(new IpWildcard(Prefix.parse("224.2.127.285/32"))))
                .build(),
            "sap");
      case VRRP:
        return new MatchHeaderSpace(
            headerSpaceBuilder.setIpProtocols(IpProtocol.VRRP).build(), "VRRP");
      default:
        {
          throw new BatfishException(
              "missing definition for host-inbound-traffic protocol: \"" + name() + "\"");
        }
    }
  }
}
