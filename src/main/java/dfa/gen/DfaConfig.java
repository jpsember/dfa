package dfa.gen;

import java.io.File;
import js.data.AbstractData;
import js.file.Files;
import js.json.JSMap;

public class DfaConfig implements AbstractData {

  public File input() {
    return mInput;
  }

  public File output() {
    return mOutput;
  }

  public File ids() {
    return mIds;
  }

  public File exampleText() {
    return mExampleText;
  }

  public boolean exampleVerify() {
    return mExampleVerify;
  }

  public float version() {
    return mVersion;
  }

  public boolean ascii() {
    return mAscii;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "input";
  protected static final String _1 = "output";
  protected static final String _2 = "ids";
  protected static final String _3 = "example_text";
  protected static final String _4 = "example_verify";
  protected static final String _5 = "version";
  protected static final String _6 = "ascii";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mInput.toString());
    m.putUnsafe(_1, mOutput.toString());
    m.putUnsafe(_2, mIds.toString());
    m.putUnsafe(_3, mExampleText.toString());
    m.putUnsafe(_4, mExampleVerify);
    m.putUnsafe(_5, mVersion);
    m.putUnsafe(_6, mAscii);
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
    {
      mInput = Files.DEFAULT;
      String x = m.opt(_0, (String) null);
      if (x != null) {
        mInput = new File(x);
      }
    }
    {
      mOutput = Files.DEFAULT;
      String x = m.opt(_1, (String) null);
      if (x != null) {
        mOutput = new File(x);
      }
    }
    {
      mIds = Files.DEFAULT;
      String x = m.opt(_2, (String) null);
      if (x != null) {
        mIds = new File(x);
      }
    }
    {
      mExampleText = Files.DEFAULT;
      String x = m.opt(_3, (String) null);
      if (x != null) {
        mExampleText = new File(x);
      }
    }
    mExampleVerify = m.opt(_4, false);
    mVersion = m.opt(_5, 4.0f);
    mAscii = m.opt(_6, false);
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
    if (!(mInput.equals(other.mInput)))
      return false;
    if (!(mOutput.equals(other.mOutput)))
      return false;
    if (!(mIds.equals(other.mIds)))
      return false;
    if (!(mExampleText.equals(other.mExampleText)))
      return false;
    if (!(mExampleVerify == other.mExampleVerify))
      return false;
    if (!(mVersion == other.mVersion))
      return false;
    if (!(mAscii == other.mAscii))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mInput.hashCode();
      r = r * 37 + mOutput.hashCode();
      r = r * 37 + mIds.hashCode();
      r = r * 37 + mExampleText.hashCode();
      r = r * 37 + (mExampleVerify ? 1 : 0);
      r = r * 37 + (int)mVersion;
      r = r * 37 + (mAscii ? 1 : 0);
      m__hashcode = r;
    }
    return r;
  }

  protected File mInput;
  protected File mOutput;
  protected File mIds;
  protected File mExampleText;
  protected boolean mExampleVerify;
  protected float mVersion;
  protected boolean mAscii;
  protected int m__hashcode;

  public static final class Builder extends DfaConfig {

    private Builder(DfaConfig m) {
      mInput = m.mInput;
      mOutput = m.mOutput;
      mIds = m.mIds;
      mExampleText = m.mExampleText;
      mExampleVerify = m.mExampleVerify;
      mVersion = m.mVersion;
      mAscii = m.mAscii;
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
      r.mInput = mInput;
      r.mOutput = mOutput;
      r.mIds = mIds;
      r.mExampleText = mExampleText;
      r.mExampleVerify = mExampleVerify;
      r.mVersion = mVersion;
      r.mAscii = mAscii;
      return r;
    }

    public Builder input(File x) {
      mInput = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder output(File x) {
      mOutput = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder ids(File x) {
      mIds = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder exampleText(File x) {
      mExampleText = (x == null) ? Files.DEFAULT : x;
      return this;
    }

    public Builder exampleVerify(boolean x) {
      mExampleVerify = x;
      return this;
    }

    public Builder version(float x) {
      mVersion = x;
      return this;
    }

    public Builder ascii(boolean x) {
      mAscii = x;
      return this;
    }

  }

  public static final DfaConfig DEFAULT_INSTANCE = new DfaConfig();

  private DfaConfig() {
    mInput = Files.DEFAULT;
    mOutput = Files.DEFAULT;
    mIds = Files.DEFAULT;
    mExampleText = Files.DEFAULT;
    mVersion = 4.0f;
  }

}
