package org.batfish.datamodel;

import java.util.SortedMap;
import java.util.TreeMap;

import org.batfish.common.util.ComparableStructure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;

@JsonSchemaDescription("An IPV4 firewall zone")
public final class Zone extends ComparableStructure<String> {

   private static final String FROM_HOST_FILTER_VAR = "fromHostFilter";

   private static final String INBOUND_FILTER_VAR = "inboundFilter";

   private static final String INBOUND_INTERFACE_FILTERS_VAR = "inboundInterfaceFilters";

   /**
    *
    */
   private static final long serialVersionUID = 1L;

   private static final String TO_HOST_FILTER_VAR = "toHostFilter";

   private static final String TO_ZONE_POLICIES_VAR = "toZonePolicies";

   private IpAccessList _fromHostFilter;

   private transient String _fromHostFilterName;

   private IpAccessList _inboundFilter;

   private transient String _inboundFilterName;

   private SortedMap<String, IpAccessList> _inboundInterfaceFilters;

   private transient SortedMap<String, String> _inboundInterfaceFiltersNames;

   private IpAccessList _toHostFilter;

   private transient String _toHostFilterName;

   private SortedMap<String, IpAccessList> _toZonePolicies;

   private transient SortedMap<String, String> _toZonePoliciesNames;

   @JsonCreator
   public Zone(@JsonProperty(NAME_VAR) String name) {
      super(name);
   }

   public Zone(String name, IpAccessList inboundFilter,
         IpAccessList fromHostFilter, IpAccessList toHostFilter) {
      super(name);
      _inboundFilter = inboundFilter;
      _inboundInterfaceFilters = new TreeMap<>();
      _fromHostFilter = fromHostFilter;
      _toHostFilter = toHostFilter;
      _toZonePolicies = new TreeMap<>();
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      }
      Zone other = (Zone) object;
      if (!this._fromHostFilter.equals(other._fromHostFilter)) {
         return false;
      }
      if (!this._inboundFilter.equals(other._inboundFilter)) {
         return false;
      }
      if (!this._inboundInterfaceFilters
            .equals(other._inboundInterfaceFilters)) {
         return false;
      }
      if (!this._toHostFilter.equals(other._toHostFilter)) {
         return false;
      }
      if (!this._toZonePolicies.equals(other._toZonePolicies)) {
         return false;
      }
      return true;
   }

   @JsonIgnore
   public IpAccessList getFromHostFilter() {
      return _fromHostFilter;
   }

   @JsonProperty(FROM_HOST_FILTER_VAR)
   @JsonPropertyDescription("Filter applied against packets originating from an interface in this zone on this node")
   public String getFromHostFilterName() {
      if (_fromHostFilter != null) {
         return _fromHostFilter.getName();
      }
      else {
         return _fromHostFilterName;
      }
   }

   @JsonIgnore
   public IpAccessList getInboundFilter() {
      return _inboundFilter;
   }

   @JsonProperty(INBOUND_FILTER_VAR)
   @JsonPropertyDescription("Filter applied against packets whose final destination is an interface in this zone that does not have its own inbound filter")
   public String getInboundFilterName() {
      if (_inboundFilter != null) {
         return _inboundFilter.getName();
      }
      else {
         return _inboundFilterName;
      }
   }

   @JsonIgnore
   public SortedMap<String, IpAccessList> getInboundInterfaceFilters() {
      return _inboundInterfaceFilters;
   }

   @JsonProperty(INBOUND_INTERFACE_FILTERS_VAR)
   @JsonPropertyDescription("Mapping of interfaces in this zone to their corresponding inbound filters: the filter applied against packets whose final destination is the interface whose name is the key in this mapping")
   public SortedMap<String, String> getInboundInterfaceFiltersNames() {
      if (_inboundInterfaceFilters != null
            && !_inboundInterfaceFilters.isEmpty()) {
         SortedMap<String, String> map = new TreeMap<>();
         _inboundInterfaceFilters.forEach((ifaceName, filter) -> {
            map.put(ifaceName, filter.getName());
         });
         return map;
      }
      else {
         return _inboundInterfaceFiltersNames;
      }
   }

   @JsonIgnore
   public IpAccessList getToHostFilter() {
      return _toHostFilter;
   }

   @JsonProperty(TO_HOST_FILTER_VAR)
   @JsonPropertyDescription("Filter applied against packets whose final destination is an interface in this zone. If this filter exists, it is applied IN ADDITION to the interface-specific or default inbound filter.")
   public String getToHostFilterName() {
      if (_toHostFilter != null) {
         return _toHostFilter.getName();
      }
      else {
         return _toHostFilterName;
      }
   }

   @JsonIgnore
   public SortedMap<String, IpAccessList> getToZonePolicies() {
      return _toZonePolicies;
   }

   @JsonProperty(TO_ZONE_POLICIES_VAR)
   @JsonPropertyDescription("Maps names of destination zones to the corresponding filter applied against packets which are received on this zone and routed to the named zone")
   public SortedMap<String, String> getToZonePoliciesNames() {
      if (_toZonePolicies != null && !_toZonePolicies.isEmpty()) {
         SortedMap<String, String> map = new TreeMap<>();
         _toZonePolicies.forEach((zoneName, filter) -> {
            map.put(zoneName, filter.getName());
         });
         return map;
      }
      else {
         return _toZonePoliciesNames;
      }
   }

   @JsonIgnore
   public void setFromHostFilter(IpAccessList fromHostFilter) {
      _fromHostFilter = fromHostFilter;
   }

   @JsonProperty(FROM_HOST_FILTER_VAR)
   public void setFromHostFilterName(String fromHostFilterName) {
      _fromHostFilterName = fromHostFilterName;
   }

   @JsonIgnore
   public void setInboundFilter(IpAccessList inboundFilter) {
      _inboundFilter = inboundFilter;
   }

   @JsonProperty(INBOUND_FILTER_VAR)
   public void setInboundFilterName(String inboundFilterName) {
      _inboundFilterName = inboundFilterName;
   }

   @JsonIgnore
   public void setInboundInterfaceFilters(
         SortedMap<String, IpAccessList> inboundInterfaceFilters) {
      _inboundInterfaceFilters = inboundInterfaceFilters;
   }

   @JsonProperty(INBOUND_INTERFACE_FILTERS_VAR)
   public void setInboundInterfaceFiltersNames(
         SortedMap<String, String> inboundInterfaceFiltersNames) {
      _inboundInterfaceFiltersNames = inboundInterfaceFiltersNames;
   }

   @JsonIgnore
   public void setToHostFilter(IpAccessList toHostFilter) {
      _toHostFilter = toHostFilter;
   }

   @JsonProperty(TO_HOST_FILTER_VAR)
   public void setToHostFilterName(String toHostFilterName) {
      _toHostFilterName = toHostFilterName;
   }

   @JsonIgnore
   public void setToZonePolicies(
         SortedMap<String, IpAccessList> toZonePolicies) {
      _toZonePolicies = toZonePolicies;
   }

   @JsonProperty(TO_ZONE_POLICIES_VAR)
   public void setToZonePoliciesNames(
         SortedMap<String, String> toZonePoliciesNames) {
      _toZonePoliciesNames = toZonePoliciesNames;
   }

   public boolean unorderedEqual(Object object) {
      if (this == object) {
         return true;
      }
      Zone other = (Zone) object;
      if (this.equals(other)) {
         return true;
      }
      if (!this._fromHostFilter.unorderedEqual(other._fromHostFilter)) {
         return false;
      }
      if (!this._inboundFilter.unorderedEqual(other._inboundFilter)) {
         return false;
      }
      if (unorderedEqualSortedMap(this._inboundInterfaceFilters,
            other._inboundInterfaceFilters)) {
         return false;
      }
      if (!this._toHostFilter.unorderedEqual(other._toHostFilter)) {
         return false;
      }
      if (unorderedEqualSortedMap(this._toZonePolicies,
            other._toZonePolicies)) {
         return false;
      }
      return true;
   }

   private boolean unorderedEqualSortedMap(SortedMap<String, IpAccessList> a,
         SortedMap<String, IpAccessList> b) {
      if (!a.keySet().equals(b.keySet())) {
         return false;
      }
      for (String s : a.keySet()) {
         if (!a.get(s).unorderedEqual(b.get(s))) {
            return false;
         }
      }
      return true;
   }

}
