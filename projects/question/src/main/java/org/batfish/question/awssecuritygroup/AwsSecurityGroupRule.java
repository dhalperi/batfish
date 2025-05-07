package org.batfish.question.awssecuritygroup;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Represents an AWS security group rule converted from a firewall access list entry. */
public class AwsSecurityGroupRule {

  /** Supported protocols for AWS security group rules. */
  public enum Protocol {
    TCP("tcp"),
    UDP("udp"),
    ICMP("icmp"),
    ALL("all");

    private final String _protocolName;

    Protocol(String protocolName) {
      _protocolName = protocolName;
    }

    @Override
    public String toString() {
      return _protocolName;
    }
  }

  private final @Nonnull Protocol _protocol;
  private final @Nonnull String _sourceCidr;
  private final @Nullable Integer _fromPort;
  private final @Nullable Integer _toPort;
  private final @Nullable Integer _icmpType;
  private final @Nullable Integer _icmpCode;
  private final @Nullable String _description;

  private AwsSecurityGroupRule(
      Protocol protocol,
      String sourceCidr,
      Integer fromPort,
      Integer toPort,
      Integer icmpType,
      Integer icmpCode,
      String description) {
    _protocol = protocol;
    _sourceCidr = sourceCidr;
    _fromPort = fromPort;
    _toPort = toPort;
    _icmpType = icmpType;
    _icmpCode = icmpCode;
    _description = description;
  }

  public static Builder builder() {
    return new Builder();
  }

  public @Nonnull Protocol getProtocol() {
    return _protocol;
  }

  public @Nonnull String getSourceCidr() {
    return _sourceCidr;
  }

  public @Nullable Integer getFromPort() {
    return _fromPort;
  }

  public @Nullable Integer getToPort() {
    return _toPort;
  }

  public @Nullable Integer getIcmpType() {
    return _icmpType;
  }

  public @Nullable Integer getIcmpCode() {
    return _icmpCode;
  }

  public @Nullable String getDescription() {
    return _description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AwsSecurityGroupRule)) {
      return false;
    }
    AwsSecurityGroupRule that = (AwsSecurityGroupRule) o;
    return Objects.equals(_protocol, that._protocol)
        && Objects.equals(_sourceCidr, that._sourceCidr)
        && Objects.equals(_fromPort, that._fromPort)
        && Objects.equals(_toPort, that._toPort)
        && Objects.equals(_icmpType, that._icmpType)
        && Objects.equals(_icmpCode, that._icmpCode)
        && Objects.equals(_description, that._description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        _protocol, _sourceCidr, _fromPort, _toPort, _icmpType, _icmpCode, _description);
  }

  /** Builder for AWS security group rules. */
  public static class Builder {
    private Protocol _protocol;
    private String _sourceCidr;
    private Integer _fromPort;
    private Integer _toPort;
    private Integer _icmpType;
    private Integer _icmpCode;
    private String _description;

    private Builder() {}

    public Builder setProtocol(Protocol protocol) {
      _protocol = protocol;
      return this;
    }

    public Builder setSourceCidr(String sourceCidr) {
      _sourceCidr = sourceCidr;
      return this;
    }

    public Builder setFromPort(Integer fromPort) {
      _fromPort = fromPort;
      return this;
    }

    public Builder setToPort(Integer toPort) {
      _toPort = toPort;
      return this;
    }

    public Builder setPortRange(Integer fromPort, Integer toPort) {
      _fromPort = fromPort;
      _toPort = toPort;
      return this;
    }

    public Builder setIcmpType(Integer icmpType) {
      _icmpType = icmpType;
      return this;
    }

    public Builder setIcmpCode(Integer icmpCode) {
      _icmpCode = icmpCode;
      return this;
    }

    public Builder setDescription(String description) {
      _description = description;
      return this;
    }

    public AwsSecurityGroupRule build() {
      if (_protocol == null) {
        throw new IllegalArgumentException("Protocol must be specified");
      }
      if (_sourceCidr == null) {
        throw new IllegalArgumentException("Source CIDR must be specified");
      }

      return new AwsSecurityGroupRule(
          _protocol, _sourceCidr, _fromPort, _toPort, _icmpType, _icmpCode, _description);
    }
  }
}
