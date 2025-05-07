package org.batfish.question.awssecuritygroup;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import org.batfish.common.Answerer;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.plugin.IBatfish;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.IpAccessList;
import org.batfish.datamodel.answers.Schema;
import org.batfish.datamodel.pojo.Node;
import org.batfish.datamodel.questions.DisplayHints;
import org.batfish.datamodel.questions.Question;
import org.batfish.datamodel.table.ColumnMetadata;
import org.batfish.datamodel.table.Row;
import org.batfish.datamodel.table.TableAnswerElement;
import org.batfish.datamodel.table.TableMetadata;
import org.batfish.question.awssecuritygroup.AwsSecurityGroupRule.Protocol;
import org.batfish.representation.cisco_nxos.CiscoNxosConfiguration;
import org.batfish.specifier.FilterSpecifier;
import org.batfish.specifier.SpecifierContext;

/** Converts firewall access lists to AWS security group rules. */
public class ConvertToAwsSecurityGroupAnswerer extends Answerer {
  public static final String COL_NODE = "Node";
  public static final String COL_FILTER_NAME = "Filter_Name";
  public static final String COL_AWS_GROUP_NAME = "AWS_Group_Name";
  public static final String COL_PROTOCOL = "Protocol";
  public static final String COL_SOURCE_CIDR = "Source_CIDR";
  public static final String COL_PORT_RANGE = "Port_Range";
  public static final String COL_DESCRIPTION = "Description";

  public static final List<ColumnMetadata> COLUMN_METADATA =
      ImmutableList.of(
          new ColumnMetadata(COL_NODE, Schema.NODE, "Node", true, false),
          new ColumnMetadata(COL_FILTER_NAME, Schema.STRING, "Filter name", true, false),
          new ColumnMetadata(
              COL_AWS_GROUP_NAME, Schema.STRING, "AWS security group name", false, false),
          new ColumnMetadata(COL_PROTOCOL, Schema.STRING, "Protocol", false, false),
          new ColumnMetadata(COL_SOURCE_CIDR, Schema.STRING, "Source CIDR block", false, false),
          new ColumnMetadata(COL_PORT_RANGE, Schema.STRING, "Port range", false, false),
          new ColumnMetadata(COL_DESCRIPTION, Schema.STRING, "Description", false, false));

  public ConvertToAwsSecurityGroupAnswerer(Question question, IBatfish batfish) {
    super(question, batfish);
  }

  /**
   * Creates a {@link TableAnswerElement} object containing the right metadata
   *
   * @param question The question object for which the answer is being created, to borrow its {@link
   *     DisplayHints}
   */
  public static TableAnswerElement create(ConvertToAwsSecurityGroupQuestion question) {
    String textDesc =
        String.format(
            "Filter ${%s} on node ${%s} converted to AWS security group rule with protocol ${%s},"
                + " source ${%s}, and ports ${%s}",
            COL_FILTER_NAME, COL_NODE, COL_PROTOCOL, COL_SOURCE_CIDR, COL_PORT_RANGE);
    DisplayHints dhints = question.getDisplayHints();
    if (dhints != null && dhints.getTextDesc() != null) {
      textDesc = dhints.getTextDesc();
    }
    TableMetadata metadata = new TableMetadata(COLUMN_METADATA, textDesc);
    return new TableAnswerElement(metadata);
  }

  @Override
  public TableAnswerElement answer(NetworkSnapshot snapshot) {
    Multiset<Row> rows = getRows(snapshot);

    ConvertToAwsSecurityGroupQuestion question = (ConvertToAwsSecurityGroupQuestion) _question;
    TableAnswerElement answer = create(question);
    answer.postProcessAnswer(question, rows);
    return answer;
  }

  private Multiset<Row> getRows(NetworkSnapshot snapshot) {
    ConvertToAwsSecurityGroupQuestion question = (ConvertToAwsSecurityGroupQuestion) _question;
    SpecifierContext context = _batfish.specifierContext(snapshot);

    // Load vendor configurations instead of converted configurations
    Map<String, org.batfish.vendor.VendorConfiguration> vendorConfigurations =
        _batfish.loadVendorConfigurations(snapshot);

    SortedSet<String> includeNodes =
        ImmutableSortedSet.copyOf(question.getNodeSpecifier().resolve(context));
    FilterSpecifier filterSpecifier = question.getFilterSpecifier();

    Multiset<Row> rows = HashMultiset.create();

    for (String node : includeNodes) {
      // Get the standard filters for reference
      SortedSet<IpAccessList> filtersByName =
          ImmutableSortedSet.copyOf(
              Comparator.comparing(IpAccessList::getName), filterSpecifier.resolve(node, context));
      if (filtersByName.isEmpty()) {
        continue;
      }

      // Get the vendor configuration
      org.batfish.vendor.VendorConfiguration vendorConfig = vendorConfigurations.get(node);

      // Get the standard configuration for hostname and other basic info
      Configuration c = context.getConfigs().get(node);
      if (c == null) {
        continue;
      }

      // Process based on vendor type
      if (vendorConfig instanceof CiscoNxosConfiguration) {
        CiscoNxosConfiguration nxosConfig = (CiscoNxosConfiguration) vendorConfig;

        // Convert each filter to AWS security group rules
        for (IpAccessList filter : filtersByName) {
          // Find the corresponding vendor-specific access list
          org.batfish.representation.cisco_nxos.IpAccessList vendorFilter =
              nxosConfig.getIpAccessLists().get(filter.getName());

          if (vendorFilter != null) {
            CiscoNxosToAwsSecurityGroupConverter converter =
                new CiscoNxosToAwsSecurityGroupConverter();
            List<AwsSecurityGroupRule> rules =
                converter.convertAccessList(nxosConfig, vendorFilter);

            // Create rows for each rule
            for (AwsSecurityGroupRule rule : rules) {
              rows.add(
                  Row.builder()
                      .put(COL_NODE, new Node(c.getHostname()))
                      .put(COL_FILTER_NAME, filter.getName())
                      .put(
                          COL_AWS_GROUP_NAME,
                          generateAwsGroupName(c.getHostname(), filter.getName()))
                      .put(COL_PROTOCOL, rule.getProtocol().toString())
                      .put(COL_SOURCE_CIDR, rule.getSourceCidr())
                      .put(COL_PORT_RANGE, formatPortRange(rule))
                      .put(COL_DESCRIPTION, rule.getDescription())
                      .build());
            }
          }
        }
      }
      // Add support for other vendors here in the future
    }
    return rows;
  }

  /** Generates an AWS security group name from the device and filter names. */
  private String generateAwsGroupName(String deviceName, String filterName) {
    // AWS security group names can only contain a-z, A-Z, 0-9, spaces, and ._-:/()#,@[]+=&;{}!$*
    // and must be up to 255 characters
    String name = String.format("%s-%s", deviceName, filterName);
    // Replace invalid characters with dashes
    name = name.replaceAll("[^a-zA-Z0-9._\\-:/()#,@\\[\\]+=&;{}!$*]", "-");
    // Truncate if too long
    if (name.length() > 255) {
      name = name.substring(0, 255);
    }
    return name;
  }

  /** Formats the port range for display. */
  private String formatPortRange(AwsSecurityGroupRule rule) {
    if (rule.getProtocol() == Protocol.TCP || rule.getProtocol() == Protocol.UDP) {
      if (java.util.Objects.equals(rule.getFromPort(), rule.getToPort())) {
        return String.valueOf(rule.getFromPort());
      } else {
        return String.format("%d-%d", rule.getFromPort(), rule.getToPort());
      }
    } else if (rule.getProtocol() == Protocol.ICMP) {
      if (rule.getIcmpType() != null) {
        if (rule.getIcmpCode() != null) {
          return String.format("Type: %d, Code: %d", rule.getIcmpType(), rule.getIcmpCode());
        } else {
          return String.format("Type: %d", rule.getIcmpType());
        }
      }
    }
    return "All";
  }
}
