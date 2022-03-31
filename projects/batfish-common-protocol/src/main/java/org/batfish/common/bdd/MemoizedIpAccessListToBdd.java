package org.batfish.common.bdd;

import com.google.common.annotations.VisibleForTesting;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import net.sf.javabdd.BDD;
import org.batfish.datamodel.AclLine;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.IpSpace;
import org.batfish.datamodel.acl.AclLineMatchExpr;

/**
 * An {@link IpAccessListToBdd} that memoizes its {@link IpAccessListToBdd#convert} method using an
 * {@link IdentityHashMap}.
 */
public final class MemoizedIpAccessListToBdd extends IpAccessListToBdd {
  private final Map<AclLine, PermitAndDenyBdds> _lineCache = new IdentityHashMap<>();
  private final Map<AclLineMatchExpr, BDD> _exprCache = new IdentityHashMap<>();

  public MemoizedIpAccessListToBdd(
      BDDPacket packet,
      BDDSourceManager mgr,
      Map<String, IpAccessList> aclEnv,
      Map<String, IpSpace> namedIpSpaces) {
    super(packet, mgr, new HeaderSpaceToBDD(packet, namedIpSpaces), aclEnv);
  }

  @Override
  public PermitAndDenyBdds toPermitAndDenyBdds(@Nonnull AclLine line) {
    return _lineCache.computeIfAbsent(line, this::convert).id();
  }

  @Override
  public BDD toBdd(@Nonnull AclLineMatchExpr expr) {
    return _exprCache.computeIfAbsent(expr, this::convert).id();
  }

  @VisibleForTesting
  Optional<BDD> getMemoizedBdd(AclLineMatchExpr expr) {
    return Optional.ofNullable(_exprCache.get(expr));
  }
}
