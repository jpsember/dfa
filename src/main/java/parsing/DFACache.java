package js.parsing;

import static js.base.Tools.*;

import js.base.BaseObject;
import js.base.SystemCall;
import js.file.DirWalk;
import js.file.Files;
import js.geometry.MyMath;

import java.io.File;
import java.util.Map;

/**
 * Supports two caches:
 *
 * An in-memory cache, for loading DFAs from a .dfa file
 *
 * An in-filesystem cache for generating DFAs from RXP scripts
 */
public final class DFACache extends BaseObject {

  private static final String EXT_RXP = "rxp";
  private static final String EXT_DFA = "dfa";

  public static DFACache SHARED_INSTANCE = new DFACache();

  private DFACache() {
  }


  @Deprecated // Mark this as deprecated as it should only be called from development when rebuild is necessary
  public DFA buildFromSource(File optionalSourceDir, String resourceName) {

    checkArgument(!resourceName.contains("."), "please omit extension");

    alert("Building .dfa from source:", optionalSourceDir, resourceName);
    var inputFileRXP = Files.absolute(new File(optionalSourceDir, resourceName + "." + EXT_RXP));
    Files.assertExists(inputFileRXP, "fromRXP");
    var outputFileDFA = Files.setExtension(inputFileRXP, EXT_DFA);
    var key = outputFileDFA.hashCode();

    var dfa = mMemoryMap.get(key);
    if (dfa == null) {
      // Call the dfa program to compile regexps to a dfa
      var sc = new SystemCall().withVerbose(verbose());
      pr("inputFileRXP:",inputFileRXP);
      sc.arg("dfa", "input", inputFileRXP, "output", outputFileDFA);
      todo("add support for assert example etc");
      sc.assertSuccess();

      dfa = DFA.parse(Files.readString(outputFileDFA));
      mMemoryMap.put(key, dfa);
    }
    return dfa;
  }

    /**
     * Read DFA from resources, if not already in cache.  Optionally call the dfa tool to regenerate the dfa from an .rxp file
     */
  public DFA fromRXP(Class klass, String resourceName /*, File optionalSourceDir*/) {

    checkArgument(!resourceName.contains("."), "please omit extension");

//    if (Files.nonEmpty(optionalSourceDir)) {
//      alert("Building .dfa from source:", optionalSourceDir, resourceName);
//      var inputFileRXP = Files.absolute(new File(optionalSourceDir, resourceName + "." + EXT_RXP));
//      Files.assertExists(inputFileRXP, "fromRXP");
//      var outputFileDFA = Files.setExtension(inputFileRXP, EXT_DFA);
//      var key = outputFileDFA.hashCode();
//
//      var dfa = mMemoryMap.get(key);
//      if (dfa == null) {
//        // Call the dfa program to compile regexps to a dfa
//        var sc = new SystemCall().withVerbose(verbose());
//        sc.arg("dfa", "input", inputFileRXP, "output", outputFileDFA);
//        todo("add support for assert example etc");
//        sc.assertSuccess();
//
//        dfa = DFA.parse(Files.readString(outputFileDFA));
//        mMemoryMap.put(key, dfa);
//      }
//      return dfa;
//    }

    var key = (klass.toString() + ":" + resourceName).hashCode();
    var dfa = mMemoryMap.get(key);
    if (dfa == null) {
      var dfaFilename = Files.setExtension(resourceName, EXT_DFA);
      var res = Files.readString(klass, dfaFilename);
      dfa = DFA.parse(res);
      mMemoryMap.put(key, dfa);
    }
    return dfa;
  }

  /**
   * If not already in the cache, construct DFA file from an RXP script
   */
  public DFA forTokenDefinitions(String rxpScript) {
    int key = rxpScript.hashCode();
    var result = mMemoryMap.get(key);
    log("get DFA from key:", key);
    if (result == null) {

      log("...not in cache; building; source:", INDENT, rxpScript);

      // Check the filesystem cache for the DFA file
      var dfaFile = new File(cacheDir(), key + "." + EXT_DFA);
      if (!dfaFile.exists()) {
        // Trim the cache to a reasonable size
        final int MAX_CACHE_SIZE = 20;
        {
          var dw = new DirWalk(cacheDir()).withExtensions(EXT_DFA);
          var dfaList = dw.files();
          if (dfaList.size() > MAX_CACHE_SIZE) {
            MyMath.permute(dfaList, null);
            while (dfaList.size() > MAX_CACHE_SIZE) {
              var purgeFile = pop(dfaList);
              log("...culling cache, removing:", INDENT, purgeFile);
              files().deleteFile(purgeFile);
            }
          }
        }

        // Call the dfa program to compile regexps to a dfa
        var tmpDir = Files.createTempDir("PrepOper_build_dfa");
        var inputFile = new File(tmpDir, "x.rxp");
        files().writeString(inputFile, rxpScript);
        var sc = new SystemCall().withVerbose(verbose());
        sc.arg("dfa", "input", inputFile, "output", dfaFile);
        sc.assertSuccess();
      }
      result = DFA.parse(Files.readString(dfaFile));
      mMemoryMap.put(key, result);
    }
    return result;
  }

  public void withCacheDir(File dir) {
    Files.assertNonEmpty(dir, "cache dir");
    files().mkdirs(dir);
    mCacheDir = dir;
  }

  private File cacheDir() {
    if (mCacheDir == null) {
      withCacheDir(new File(Files.homeDirectory(), ".dfa_cache"));
    }
    return mCacheDir;
  }

  private Files files() {
    return Files.S;
  }

  private File mCacheDir;
  private Map<Integer, DFA> mMemoryMap = concurrentHashMap();
}
