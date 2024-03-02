package dfa.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class DfaConfig implements AbstractData {

  public String notUsedYet() {
    return mNotUsedYet;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "not_used_yet";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mNotUsedYet);
    return m;
  }

  @Override
  public DfaConfig build() {
    return this;
  }

  @Override
  public DfaConfig parse(Object obj) {
    return new DfaConfig((JSMap) obj);
  }

  private DfaConfig(JSMap m) {
    mNotUsedYet = m.opt(_0, "");
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof DfaConfig))
      return false;
    DfaConfig other = (DfaConfig) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mNotUsedYet.equals(other.mNotUsedYet)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mNotUsedYet.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected String mNotUsedYet;
  protected int m__hashcode;

  public static final class Builder extends DfaConfig {

    private Builder(DfaConfig m) {
      mNotUsedYet = m.mNotUsedYet;
    }

    @Override
    public Builder toBuilder() {
      return this;
    }

    @Override
    public int hashCode() {
      m__hashcode = 0;
      return super.hashCode();
    }

    @Override
    public DfaConfig build() {
      DfaConfig r = new DfaConfig();
      r.mNotUsedYet = mNotUsedYet;
      return r;
    }

    public Builder notUsedYet(String x) {
      mNotUsedYet = (x == null) ? "" : x;
      return this;
    }

  }

  public static final DfaConfig DEFAULT_INSTANCE = new DfaConfig();

  private DfaConfig() {
    mNotUsedYet = "";
  }

}
