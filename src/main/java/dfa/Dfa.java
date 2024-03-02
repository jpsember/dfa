package dfa;

import static js.base.Tools.*;

import js.app.App;

public class Dfa extends App {

  public static void main(String[] args) {
    loadTools();
    Dfa app = new Dfa();
    app.startApplication(args);
    app.exitWithReturnCode();
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  protected void registerOperations() {
    registerOper(new DfaOper());
  }

}
