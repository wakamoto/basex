package org.deepfs.fsml.parsers;

import java.io.IOException;
import java.util.TreeMap;

import org.basex.core.Main;
import org.basex.core.Prop;
import org.basex.util.Token;
import org.deepfs.fsml.BufferedFileChannel;
import org.deepfs.fsml.DeepFile;
import org.deepfs.fsml.FileType;
import org.deepfs.fsml.MimeType;
import org.deepfs.fsml.ParserRegistry;

/**
 * Text parser that tries to extract textual content from files.
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Bastian Lemke
 */
public final class TXTParser implements IFileParser {

  /** Suffixes of all file formats, this parser is able to parse. */
  private static final TreeMap<String, MimeType> SUFFIXES =
      new TreeMap<String, MimeType>();

  static {
    SUFFIXES.put("txt", MimeType.TXT);
    SUFFIXES.put("html", MimeType.HTML);
    SUFFIXES.put("htm", MimeType.HTML);
    SUFFIXES.put("css", MimeType.CSS);
    SUFFIXES.put("java", MimeType.JAVA);
    SUFFIXES.put("xq", MimeType.TXT);
    SUFFIXES.put("sh", MimeType.SH);
    for(final String suf : SUFFIXES.keySet())
      ParserRegistry.register(suf, TXTParser.class);
    ParserRegistry.registerFallback(TXTParser.class);
  }

  /** {@inheritDoc} */
  @Override
  public boolean check(final BufferedFileChannel bfc) {
    try {
      // [BL] check fstextmax property
      final int len = (int) Math.min(bfc.size(), (Integer) Prop.FSTEXTMAX[1]);
      return Token.isValidUTF8(bfc.get(new byte[len]));
    } catch(final Exception e) {
      return false;
    }
  }

  @Override
  public void extract(final DeepFile deepFile) throws IOException {
    final BufferedFileChannel bfc = deepFile.getBufferedFileChannel();

    if(deepFile.extractMeta()) {
      final String name = bfc.getFileName();
      final String suf = name.substring(name.lastIndexOf('.') +
          1).toLowerCase();
      final MimeType mime = SUFFIXES.get(suf);
      if(mime == null) {
        deepFile.setFileType(FileType.TEXT);
        deepFile.setFileFormat(MimeType.UNKNOWN);
      } else {
        for(final FileType ft : mime.getMetaTypes()) deepFile.setFileType(ft);
        deepFile.setFileFormat(mime);
      }
    }
    if(deepFile.extractText()) {
      final int len = (int) Math.min(bfc.size(), deepFile.maxTextSize());
      final byte[] text = bfc.get(new byte[len]);
      if(Token.isValidUTF8(text)) deepFile.addText(0, len, Token.string(text));
    }
  }

  @Override
  public void propagate(final DeepFile deepFile) {
    Main.notimplemented();
  }
}