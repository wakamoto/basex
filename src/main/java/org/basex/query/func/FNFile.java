package org.basex.query.func;

import java.io.File;
import org.basex.core.Prop;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.Bln;
import org.basex.query.item.Item;
import org.basex.query.item.Str;
import org.basex.query.iter.Iter;
import org.basex.util.Token;

/**
 * Functions on files and directories.
 * 
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Rositsa Shadura
 */
final class FNFile extends Fun {
  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    checkAdmin(ctx);

    switch(func) {
      case FILES: return listFiles(ctx);
      default:    return super.iter(ctx);
    }
  }

  @Override
  public Item atomic(final QueryContext ctx) throws QueryException {
    checkAdmin(ctx);

    final File path = expr.length == 0 ? null : 
      new File(Token.string(checkStr(expr[0].atomic(ctx))));

    switch(func) {
      case MKDIR:      return Bln.get(path.mkdir());
      case MKDIRS:     return Bln.get(path.mkdirs());
      case ISDIR:      return Bln.get(path.isDirectory());
      case ISFILE:     return Bln.get(path.isFile());
      case ISREAD:     return Bln.get(path.canRead());
      case ISWRITE:    return Bln.get(path.canWrite());
      case PATHSEP:    return Str.get(Prop.SEP);
      case DELETE:     return Bln.get(path.delete());
      case PATHTOFULL: return Str.get(path.getAbsolutePath());
      default:         return super.atomic(ctx);
    }
  }

  /**
   * Lists all files in a directory.
   * @param ctx query context
   * @return iterator
   * @throws QueryException query exception
   */
  private Iter listFiles(final QueryContext ctx) throws QueryException {
    return new Iter() {
      final String path = new String(checkStr(expr[0].atomic(ctx)));
      final String[] files = new File(path).list();
      int c = -1;

      @Override
      public Item next() {
        return ++c < files.length ? Str.get(files[c]) : null;
      }
    };
  }
}