package org.batfish.grammar.flatjuniper;

import org.batfish.common.WellKnownCommunity;
import org.batfish.datamodel.bgp.community.StandardCommunity;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SkipNode;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.common.StringBuilderSink;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;

/** A class that converts a Juniper community regex to a Java regex. */
@BuildParseTree
@SuppressWarnings({
  "checkstyle:methodname", // this class uses idiomatic names
  "WeakerAccess", // access of Rule methods is needed for parser auto-generation.
})
public class BgpCommunityRegex extends BaseParser<String> {

  // Helper to convert Juniper constants to BGP community constants.
  static String wellKnownToRegex(String s) {
    long wellKnownValue;
    switch (s) {
      case "no-advertise":
        wellKnownValue = WellKnownCommunity.NO_ADVERTISE;
        break;
      case "no-export":
        wellKnownValue = WellKnownCommunity.NO_EXPORT;
        break;
      case "no-export-subconfed":
        wellKnownValue = WellKnownCommunity.NO_EXPORT_SUBCONFED;
        break;
      default:
        throw new IllegalArgumentException(s);
    }
    return '^' + StandardCommunity.of(wellKnownValue).toString() + '$';
  }

  Rule TopLevel() {
    return Sequence(FirstOf(ExtendedCommunityTopLevel(), StandardCommunityTopLevel()), EOI);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // Extended communities
  ////////////////////////////////////////////////////////////////////////////////////////

  Rule ExtendedCommunityTopLevel() {
    return Sequence(ExtLiteralCommunity(), push(pop()));
  }

  Rule ExtLiteralCommunity() {
    return Sequence(
        ExtLiteralTerm(),
        ':',
        ExtLiteralAdministrator(),
        ':',
        ExtLiteralAssignedNumber(),
        push(String.format("%s:%s:%s", pop(2), pop(1), pop())));
  }

  Rule ExtLiteralTerm() {
    return FirstOf(ExtWellKnownTerms(), Digits());
  }

  Rule ExtWellKnownTerms() {
    return Sequence(
        FirstOf("bandwidth", "domain-id", "origin", "rt-import", "src-as", "target"),
        push(match()));
  }

  Rule ExtLiteralAdministrator() {
    return FirstOf(Ipv4Address(), Sequence(Uint32(), 'L', push(pop() + 'L')), Uint16());
  }

  Rule ExtLiteralAssignedNumber() {
    return Digits();
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // Standard communities
  ////////////////////////////////////////////////////////////////////////////////////////

  Rule StandardCommunityTopLevel() {
    return Sequence(FirstOf(LiteralCommunity(), RegexCommunity(), CommunityAsLong()), EOI);
  }

  Rule LiteralCommunity() {
    return FirstOf(RegularCommunity(), WellKnownCommunity());
  }

  Rule Wildcard() {
    return Sequence(Ch('*'), push(".*"));
  }

  Rule CommunityAsLong() {
    return Sequence(
        Digits(), push("^" + StandardCommunity.of(Long.valueOf(pop())).toString() + "$"));
  }

  Rule RegularCommunity() {
    return Sequence(Digits(), ':', Digits(), push(String.format("^%s:%s$", pop(1), pop(0))));
  }

  Rule WellKnownCommunity() {
    return Sequence(
        FirstOf("no-advertise", "no-export-subconfed", "no-export"),
        push(wellKnownToRegex(match())));
  }

  Rule RegexCommunity() {
    return Sequence(
        Optional('^'),
        push(matchOrDefault("")),
        Term(),
        ':',
        Term(),
        Optional('$'),
        push(matchOrDefault("")),
        push(String.format("%s%s:%s%s", pop(3), pop(2), pop(1), pop())));
  }

  @SkipNode
  Rule Operator() {
    return Sequence(
        Optional(
            FirstOf(
                Op_Asterisk(), Op_Plus(), Op_QuestionMark(), Op_Exact(), Op_OrMore(), Op_Range())),
        push(matchOrDefault("")));
  }

  @SuppressSubnodes
  Rule Op_Asterisk() {
    return Ch('*');
  }

  @SuppressSubnodes
  Rule Op_Plus() {
    return Ch('+');
  }

  @SuppressSubnodes
  Rule Op_QuestionMark() {
    return Ch('?');
  }

  @SuppressSubnodes
  Rule Op_Exact() {
    return Sequence('{', Digits(), '}');
  }

  @SuppressSubnodes
  Rule Op_Range() {
    return Sequence('{', Digits(), ',', Digits(), '}');
  }

  @SuppressSubnodes
  Rule Op_OrMore() {
    return Sequence('{', Digits(), ',', '}');
  }

  Rule Term_Inner() {
    return Sequence(T_TopLevel(), Operator(), push(String.format("%s%s", pop(1), pop())));
  }

  Rule Term() {
    return FirstOf(
        Wildcard(), // wildcard is only valid if it's the entire term
        Sequence(
            Term_Inner(), // first term, will be pop(1) below
            ZeroOrMore(
                Term_Inner(), // pop()
                push(String.format("%s%s", pop(1), pop())))));
  }

  Rule T_TopLevel() {
    return FirstOf(T_Group(), T_Or(), SetOfDigits(), Digits(), T_Dot());
  }

  @SuppressSubnodes
  Rule T_Dot() {
    return Sequence(Ch('.'), push(match()));
  }

  Rule T_Group() {
    return Sequence(
        '(',
        IgnoreSpace(),
        Term(),
        ZeroOrMore(Term(), push(String.format("%s%s", pop(1), pop()))),
        IgnoreSpace(),
        ')',
        push(String.format("(%s)", pop())));
  }

  Rule T_Or() {
    return Sequence(
        '(',
        IgnoreSpace(),
        Term(), // pop(1)
        IgnoreSpace(),
        '|',
        IgnoreSpace(),
        Term(), // pop()
        IgnoreSpace(),
        ')',
        push(String.format("(%s|%s)", pop(1), pop())));
  }

  @SuppressSubnodes
  Rule Digit() {
    return CharRange('0', '9');
  }

  @SuppressSubnodes
  Rule PositiveDigit() {
    return CharRange('0', '9');
  }

  @SuppressSubnodes
  Rule Digits() {
    return Sequence(OneOrMore(Digit()), push(match()));
  }

  @SuppressSubnodes
  Rule Uint8() {
    return Sequence(
        FirstOf(
            // Three-digit number leq 255
            Sequence("25", CharRange('0', '5')),
            Sequence('2', CharRange('0', '4'), Digit()),
            Sequence(CharRange('0', '1'), Digit(), Digit()),
            // Two-digit number
            Sequence(PositiveDigit(), Digit()),
            // One-digit number
            Digit()),
        push(match()));
  }

  Rule Ipv4Address() {
    return Sequence(
        NTimes(4, Uint8(), "."), push(String.format("%s.%s.%s.%s", pop(3), pop(2), pop(1), pop())));
  }

  @SuppressSubnodes
  Rule Uint16() {
    return Sequence(
        FirstOf(
            // Five-digit number leq 65535
            Sequence("6553", CharRange('0', '5')),
            Sequence("655", CharRange('0', '2'), Digit()),
            Sequence("65", CharRange('0', '4'), Digit(), Digit()),
            Sequence('6', CharRange('0', '4'), NTimes(3, Digit())),
            Sequence(CharRange('0', '5'), NTimes(4, Digit())),
            // Two, Three, or Four-digit number
            Sequence(PositiveDigit(), Digit(), NTimes(2, Optional(Digit()))),
            // One-digit number
            Digit()),
        push(match()));
  }

  @SuppressSubnodes
  Rule Uint32() {
    return Sequence(
        FirstOf(
            // Ten-digit number leq 4294967295
            Sequence("429496729", CharRange('0', '5')),
            Sequence("42949672", CharRange('0', '8'), Digit()),
            Sequence("4294967", CharRange('0', '1'), NTimes(2, Digit())),
            Sequence("429496", CharRange('0', '6'), NTimes(3, Digit())),
            Sequence("42949", CharRange('0', '5'), NTimes(4, Digit())),
            Sequence("4294", CharRange('0', '8'), NTimes(5, Digit())),
            Sequence("429", CharRange('0', '3'), NTimes(6, Digit())),
            Sequence("42", CharRange('0', '8'), NTimes(7, Digit())),
            Sequence('4', CharRange('0', '1'), NTimes(8, Digit())),
            Sequence(CharRange('0', '3'), NTimes(9, Digit())),
            // Two- through nine-digit numbers
            Sequence(PositiveDigit(), Digit(), NTimes(7, Optional(Digit()))),
            // One-digit number
            Digit()),
        push(match()));
  }

  @SuppressSubnodes
  Rule DigitRange() {
    return Sequence(
        Sequence(Digit(), '-', Digit()), // preserve as-is
        push(match()));
  }

  @SuppressNode
  Rule IgnoreSpace() {
    return ZeroOrMore(' ');
  }

  Rule SetOfDigits() {
    return Sequence(
        '[',
        Optional('^'),
        push(matchOrDefault("")), // pop(1)
        FirstOf(DigitRange(), Digits()), // pop()
        ']',
        push(String.format("[%s%s]", pop(1), pop())));
  }

  /** Converts the given Juniper regular expression to a Java regular expression. */
  public static String convertToJavaRegex(String regex) {
    BgpCommunityRegex parser = Parboiled.createParser(BgpCommunityRegex.class);
    BasicParseRunner<String> runner = new BasicParseRunner<>(parser.TopLevel());
    ParsingResult<String> result = runner.run(regex);
    if (!result.matched) {
      throw new IllegalArgumentException("Unhandled input: " + regex);
    }
    return result.resultValue;
  }

  /** Like {@link #convertToJavaRegex(String)}, but for debugging. */
  @SuppressWarnings("unused") // leaving here for future debugging.
  static String debugConvertToJavaRegex(String regex) {
    BgpCommunityRegex parser = Parboiled.createParser(BgpCommunityRegex.class);
    TracingParseRunner<String> runner =
        new TracingParseRunner<String>(parser.TopLevel()).withLog(new StringBuilderSink());
    ParsingResult<String> result = runner.run(regex);
    if (!result.matched) {
      throw new IllegalArgumentException("Unhandled input: " + regex + "\n" + runner.getLog());
    }
    return result.resultValue;
  }
}
