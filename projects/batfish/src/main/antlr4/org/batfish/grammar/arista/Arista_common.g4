parser grammar Arista_common;

options {
   tokenVocab = AristaLexer;
}

// Basically an ip_prefix, but the IP should not be canonicalized.
interface_address
:
  ip = ip_address subnet = SUBNET_MASK
  | prefix = IP_PREFIX
;

// An IPv4 address
ip_address: IP_ADDRESS | SUBNET_MASK;

// An IPv4 prefix expressed as IP/LENGTH or IP MASK. IP will be canonicalized upon parsing.
ip_prefix
:
  address = ip_address mask = SUBNET_MASK
  | prefix = IP_PREFIX
;

// An IP with a wildcard against it.
ip_wildcard: ip = ip_address wildcard = ip_address;

ipv6_prefix
:
  prefix = IPV6_PREFIX
;

ospf_area
:
  id_ip = ip_address
  | id = uint32
;

port_number
:
// 1-65535
  uint16
;

protocol_distance:
// 1-255
   uint8
;

uint8: UINT8;
uint16: UINT8 | UINT16;
uint32: UINT8 | UINT16 | UINT32;
// TODO: delete all uses of dec, replace with named rules that have a toInt/LongInSpace function
dec: UINT8 | UINT16 | UINT32 | DEC;

vrf_name
:
//1-100 characters
  WORD
;

word
:
  WORD
;