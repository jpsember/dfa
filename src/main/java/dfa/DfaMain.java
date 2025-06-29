package dfa;

import static js.base.Tools.*;

import js.app.App;

public class DfaMain extends App {

  public static void main(String[] args) {
    loadTools();
    todo("add ability to decompile a DFA to our NFA classes");
    DfaMain app = new DfaMain();
    //app.setCustomArgs("-v ascii");
    app.startApplication(args);
    app.exitWithReturnCode();
  }

  @Override
  public String getVersion() {
    return String.format("%.1f", Util.DFA_VERSION_5);
  }

  @Override
  protected void registerOperations() {
    registerOper(new DfaOper());
  }

}
