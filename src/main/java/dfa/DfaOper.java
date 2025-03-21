package dfa;

import static js.base.Tools.*;

import java.io.File;
import java.util.List;

import js.app.AppOper;
import js.app.CmdLineArgs;
import js.app.HelpFormatter;
import js.base.BasePrinter;
import js.file.Files;
import js.json.JSMap;

public class DfaOper extends AppOper {

  @Override
  public String userCommand() {
    return "dfa";
  }

  @Override
  public String shortHelp() {
    return "compile ." + OBJECT_EXT + " file from an ." + SOURCE_EXT + " file";
  }

  @Override
  protected void longHelp(BasePrinter b) {
    var hf = new HelpFormatter();
    hf.addItem("<." + SOURCE_EXT + " input>", "source file");
    hf.addItem("[<." + OBJECT_EXT + " output>]", "object file");
    hf.addItem("[ids <source file>]", "output file for ids");
    b.pr(hf);
  }

  @Override
  protected void processAdditionalArgs() {
    CmdLineArgs args = app().cmdLineArgs();
    int i = 0;
    while (args.hasNextArg()) {
      String a = args.nextArg();
      if (a.equals("ids")) {
        File relPath = new File(args.nextArg());
        if (!relPath.isAbsolute()) {
          relPath = new File(Files.currentDirectory(), relPath.toString());
        }
        mIdSourceFile = relPath;
        continue;
      }
      if (i >= 2)
        break;
      File relPath = new File(a);
      if (!relPath.isAbsolute()) {
        relPath = new File(Files.currentDirectory(), relPath.toString());
      }
      mFiles.add(relPath);
      i++;
    }

    args.assertArgsDone();
  }

  @Override
  public void perform() {
    if (mFiles.isEmpty())
      pr("(please specify an ." + SOURCE_EXT + " files)");
    File sourceFile = mFiles.get(0);
    File targetFile = null;
    if (mFiles.size() >= 2)
      targetFile = mFiles.get(1);
    processSourceFile(sourceFile, targetFile);
  }

  private static final String SOURCE_EXT = "rxp";
  private static final String OBJECT_EXT = "dfa";

  private void processSourceFile(File sourceFile, File targetFile) {
    sourceFile = assertExt(Files.addExtension(sourceFile, SOURCE_EXT), SOURCE_EXT);

    if (!sourceFile.exists())
      setError("No such file:", sourceFile);

    if (Files.empty(targetFile))
      targetFile = Files.setExtension(sourceFile, OBJECT_EXT);
    assertExt(targetFile, OBJECT_EXT);

    DFACompiler compiler = new DFACompiler();
    compiler.setVerbose(verbose());
    JSMap jsonMap = compiler.parse(Files.readString(sourceFile));
    String str = jsonMap.toString();
    files().writeIfChanged(targetFile, str);
    procIdsFile(compiler.tokenNames());
  }

  private static final int FTYPE_JAVA = 0, FTYPE_RUST = 1;

  /**
   * If an ids source file argument was given, write the token ids to it
   */
  private void procIdsFile(List<String> tokenNames) {
    if (Files.empty(mIdSourceFile))
      return;

    File idFile = Files.addExtension(mIdSourceFile, "java");

    int ftype = FTYPE_JAVA;
    {
      var ext = Files.getExtension(idFile);
      if (ext.equals("rs")) {
        ftype = FTYPE_RUST;
      }
    }

    // Look for markers denoting the section of the file containing the token id constants.
    // If not found, append to the end of the file.
    //
    String marker0 = "// Token Ids generated by 'dev dfa' tool (DO NOT EDIT BELOW)\n";
    String marker1 = "// End of token Ids generated by 'dev dfa' tool (DO NOT EDIT ABOVE)\n";

    String content = Files.readString(idFile, "");
    if (content.isEmpty()) {
      log("Ids file didn't exist, creating it:", idFile);
      content = marker0 + marker1;
    }

    String beforeText = "";
    String afterText = "";

    String symbolPrefix = "T_";
    int indent = 4;
    int m0 = content.indexOf(marker0);
    int m1 = content.indexOf(marker1);
    if (m0 < 0 || m1 < 0 || m1 <= m0) {
      log("Invalid or missing markers in ids file:", idFile);
      beforeText = content;
      afterText = "";
    } else {

      {
        // Use the number of spaces that the first marker is indented to determine the indentation
        int j;
        for (j = 0;; j++) {
          int i = m0 - j - 1;
          if (i < 0 || content.charAt(i) == '\n')
            break;
        }
        indent = j;
      }

      String existingText;
      boolean success = false;
      do {
        // Look to existing first symbol to infer prefix
        existingText = content.substring(m0 + marker0.length(), m1);
        String tag;
        switch (ftype) {
        case FTYPE_JAVA: {
          tag = "static final int";
        }
          break;
        case FTYPE_RUST: {
          tag = "const";
        }
          break;

        default:
          throw badArg("Unsupported file type");
        }

        //String tag = "static final int";
        int j = existingText.indexOf(tag);
        if (j < 0)
          break;
        tag = existingText.substring(j + tag.length()).trim();
        j = tag.indexOf('\n');
        if (j > 0)
          tag = tag.substring(0, j);
        j = tag.indexOf('_');
        if (j < 0)
          symbolPrefix = "";
        else
          symbolPrefix = tag.substring(0, j + 1).trim();
        success = true;
      } while (false);
      if (!success)
        log("Can't infer prefix from:", existingText);

      beforeText = content.substring(0, m0);
      afterText = content.substring(m1 + marker1.length());
    }

    StringBuilder sb = new StringBuilder();
    sb.append(beforeText);
    sb.append(marker0);

    String tab = spaces(indent);
    int index = INIT_INDEX;
    for (String tokenName : tokenNames) {
      index++;
      sb.append(tab);

      switch (ftype) {
      case FTYPE_JAVA:
        sb.append("public static final int ");
        sb.append(symbolPrefix);
        sb.append(tokenName);
        sb.append(" = ");
        sb.append(index);
        sb.append(";\n");
        break;
      case FTYPE_RUST:
        sb.append("const ");
        sb.append(symbolPrefix);
        sb.append(tokenName);
        sb.append(": i32 = ");
        sb.append(index);
        sb.append(";\n");
        break;
      default:
        throw notSupported();
      }
    }
    sb.append(tab);
    sb.append(marker1);
    sb.append(afterText);

    files().writeIfChanged(idFile, sb.toString());
  }

  private File assertExt(File file, String ext) {
    if (!Files.getExtension(file).equals(ext))
      setError("Not a ." + ext + " file:", file);
    return file;
  }

  private List<File> mFiles = arrayList();
  private File mIdSourceFile;

}
