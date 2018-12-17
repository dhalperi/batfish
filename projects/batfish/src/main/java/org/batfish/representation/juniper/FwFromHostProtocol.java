package org.batfish.representation.juniper;

import java.io.Serializable;
import org.batfish.datamodel.acl.AclLineMatchExpr;

public class FwFromHostProtocol implements Serializable {

  /** */
  private static final long serialVersionUID = 1L;

  private final HostProtocol _protocol;

  public FwFromHostProtocol(HostProtocol protocol) {
    _protocol = protocol;
  }

  public AclLineMatchExpr getMatchExpr() {
    return _protocol.getMatchCondition();
  }
}
