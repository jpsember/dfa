package dfa;

import static js.base.Tools.*;

import js.app.App;

public class Dfa extends App {

  public static void main(String[] args) {
    loadTools();
    Dfa app = new Dfa();
    //app.setCustomArgs("-v");
    app.startApplication(args);
    app.exitWithReturnCode();
  }

  @Override
  public String getVersion() {
    return String.format("%.1f", Util.DFA_VERSION_4);
  }

  @Override
  protected void registerOperations() {
    registerOper(new DfaOper());
  }

}
