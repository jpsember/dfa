package dfa;

import js.json.JSMap;
import js.testutil.MyTestCase;

import static js.base.Tools.*;

import org.junit.Test;

import js.parsing.DFA;


public class ScanTest extends MyTestCase {

  @Test
  public void simple() {
    script("{ \"alpha\": [1,2,3]}");
  }

  @Test
  public void small() {
    dfa("{\"graph\":[5,2,1,98,99,15,1,97,98,10,1,1,-1,14,0,1,1,98,99,20,1,1,-2,14],\"token_names\":\"A B\",\"version\":\"$1\"}");
    script("aabba");
  }

  @Test
  public void unknown() {
    mAllowUnknown = true;
    dfa("{\"graph\":[5,2,1,98,99,15,1,97,98,10,1,1,-1,14,0,1,1,98,99,20,1,1,-2,14],\"token_names\":\"A B\",\"version\":\"$1\"}");
    script("aabbac");
  }


  private static String dfaScript() {
    return "{\"final\":2,\"tokens\":[\"WS\",\"BROP\",\"BRCL\",\"TRUE\",\"FALSE\",\"NULL\"," +
        "\"CBROP\",\"CBRCL\",\"COMMA\",\"COLON\",\"STRING\",\"NUMBER\"],\"version\":4.0,\"states\"" +
        ":[[[125,126],46,[123,124],45,[116,117],41,[110,111],37,[102,103],32,[93,94],31,[91,92],30," +
        "[58,59],29,[49,58],28,[48,49],22,[47,48],18,[45,46],17,[44,45],16,[35,36],7,[34,35],3," +
        "[9,11,12,14,32,33],1],[[-2,-1],2,[9,11,12,14,32,33],1],[],[[32,34,35,92,93,256],3,[92,93]," +
        "5,[34,35],4],[[-12,-11]],[[32,34,35,92,93,256],3,[92,93],5,[34,35],6],[[-12,-11],2,[32,34," +
        "35,92,93,256],3,[92,93],5,[34,35],4],[[-2,-1],2,[10,11],12,[1,10,11,13,14,256],7,[13,14],8]," +
        "[[47,48],9],[[47,48],10],[[13,14],11],[[10,11],12],[[47,48],13],[[47,48],14],[[10,11],15]," +
        "[[-2,-1]],[[-10,-9]],[[49,58],28,[48,49],22],[[47,48],21,[42,43],19],[[1,42,43,256],19,[42,43]," +
        "20],[[47,48],15,[1,42,43,47,48,256],19,[42,43],20],[[-2,-1],2,[1,10,11,256],21,[10,11],12],[[-13,-12]," +
        "2,[69,70,101,102],25,[46,47],23],[[48,58],24],[[-13,-12],2,[69,70,101,102],25,[48,58],24],[[48,58],27," +
        "[43,44,45,46],26],[[48,58],27],[[-13,-12],2,[48,58],27],[[-13,-12],2,[48,58],28,[69,70,101," +
        "102],25,[46,47],23],[[-11,-10]],[[-3,-2]],[[-4,-3]],[[97,98],33],[[108,109],34],[[115,116]" +
        ",35],[[101,102],36],[[-6,-5]],[[117,118],38],[[108,109],39],[[108,109],40],[[-7,-6]],[[114" +
        ",115],42],[[117,118],43],[[101,102],44],[[-5,-4]],[[-8,-7]],[[-9,-8]]]}";
  }


  private void dfa(String s) {
    log("parsing dfa from:", INDENT, s);
    mDfa = CompactDFA.parse(s);
  }

  private CompactDFA dfa() {
    if (mDfa == null) {
      // We have to convert the dfa to the compact form
      var mp = new JSMap(dfaScript());
      // This is kind of awkward... during the parsing back and forth, it converts 4.0 to 4
      mp.put("version", 4.0);
      var newScript = Util.convertOldDFAJSMapToCompactDFA(mp).toString();
      dfa(newScript);
    }
    return mDfa;
  }

  private CompactDFA mDfa;

  private void script(String text) {
    var s = new CompactScanner(dfa(), text, -1);
    if (mAllowUnknown)
      s.setAcceptUnknownTokens();

    var sb = new StringBuilder();
    while (s.hasNext()) {
      var t = s.read();
      sb.append(String.format("%9s ", mDfa.tokenName(t.id())) + " loc:" + t.locInfo() + "  '" + t.text() + "'");
      addLF(sb);
    }

    generateMessage(sb);

    assertGenerated();
  }

  private boolean mAllowUnknown;
}
