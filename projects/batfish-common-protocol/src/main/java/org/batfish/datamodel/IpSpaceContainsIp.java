package org.batfish.datamodel;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

/** Implementation of {@link AbstractIpSpaceContainsIp}. */
public final class IpSpaceContainsIp extends AbstractIpSpaceContainsIp {
  private final Map<String, IpSpace> _namedIpSpaces;
  private final Map<IpSpace, Boolean> _cache;

  public IpSpaceContainsIp(Ip ip, Map<String, IpSpace> namedIpSpaces) {
    super(ip);
    _namedIpSpaces = ImmutableMap.copyOf(namedIpSpaces);
    _cache = new HashMap<>();
  }

  @Override
  public Boolean visit(IpSpace ipSpace) {
    return _cache.computeIfAbsent(ipSpace, super::visit);
  }

  @Override
  public Boolean visitIpSpaceReference(IpSpaceReference ipSpaceReference) {
    IpSpace ipSpace = _namedIpSpaces.get(ipSpaceReference.getName());
    if (ipSpace == null) {
      return false;
    }
    return ipSpace.accept(this);
  }
}
