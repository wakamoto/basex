package org.basex.core;

import static org.basex.core.Text.*;
import java.io.IOException;
import java.io.OutputStream;
import org.basex.core.Commands.CmdPerm;
import org.basex.data.Data;
import org.basex.data.Result;
import org.basex.io.NullOutput;
import org.basex.io.PrintOutput;
import org.basex.util.Performance;
import org.basex.util.TokenBuilder;

/**
 * This class provides the architecture for all internal command
 * implementations. It evaluates queries that are sent by the GUI, the client or
 * the standalone version.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public abstract class Command extends Progress {
  /** Commands flag: standard. */
  public static final int STANDARD = 256;
  /** Commands flag: data reference needed. */
  public static final int DATAREF = 512;

  /** Container for query information. */
  protected final TokenBuilder info = new TokenBuilder();
  /** Command arguments. */
  protected final String[] args;

  /** Database context. */
  protected Context context;
  /** Output stream. */
  protected PrintOutput out;
  /** Database properties. */
  protected Prop prop;

  /** Performance measurements. */
  protected Performance perf;
  /** Temporary query result. */
  protected Result result;

  /** Flags for controlling command processing. */
  private final int flags;

  /**
   * Constructor.
   * @param f command flags
   * @param a arguments
   */
  public Command(final int f, final String... a) {
    flags = f;
    args = a;
  }

  /**
   * Executes the command and serializes textual results to the specified output
   * stream. If an exception occurs, a {@link BaseXException} is thrown.
   * @param ctx database context
   * @param os output stream reference
   * @throws BaseXException command exception
   */
  public final void execute(final Context ctx, final OutputStream os)
      throws BaseXException {
    if(!exec(ctx, os)) throw new BaseXException(info());
  }

  /**
   * Executes the command.
   * {@link #execute(Context, OutputStream)} should be called
   * to retrieve textual results. If an exception occurs,
   * a {@link BaseXException} is thrown.
   * @param ctx database context
   * @throws BaseXException command exception
   */
  public final void execute(final Context ctx) throws BaseXException {
    if(!exec(ctx)) throw new BaseXException(info());
  }

  /**
   * Executes the command, prints the result to the specified output stream
   * and returns a success flag.
   * @param ctx database context
   * @param os output stream
   * @return success flag. The {@link #info()} method returns information
   * on a potential exception
   */
  public final boolean exec(final Context ctx, final OutputStream os) {
    // check if data reference is available
    final Data data = ctx.data;
    if(data == null && (flags & DATAREF) != 0) return error(PROCNODB);

    // check permissions
    if(!ctx.perm(flags & 0xFF, data != null ? data.meta : null)) {
      final CmdPerm[] perms = CmdPerm.values();
      int i = perms.length;
      final int f = flags & 0xFF;
      while(--i >= 0 && (1 << i & f) == 0);
      return error(PERMNO, perms[i + 1]);
    }

    // check concurrency of commands
    boolean ok = false;
    final boolean writing =
      (flags & (User.CREATE | User.WRITE)) != 0 || updating(ctx);

    ctx.lock.before(writing);
    ok = run(ctx, os);
    ctx.lock.after(writing);
    return ok;
  }

  /**
   * Executes the command and returns a success flag.
   * {@link #run(Context, OutputStream)} should be called
   * to retrieve textual results.
   * @param ctx database context
   * @return success flag. The {@link #info()} method returns information
   * on a potential exception
   */
  public final boolean exec(final Context ctx) {
    return exec(ctx, null);
  }

  /**
   * Runs the command without permission, data and concurrency checks.
   * @param ctx query context
   * @param os output stream
   * @return result of check
   */
  private boolean run(final Context ctx, final OutputStream os) {
    try {
      perf = new Performance();
      context = ctx;
      prop = ctx.prop;
      out = os == null ? new NullOutput() : os instanceof PrintOutput ?
          (PrintOutput) os : new PrintOutput(os);
      return run();
    } catch(final ProgressException ex) {
      abort();
      return error(PROGERR);
    } catch(final Throwable ex) {
      Performance.gc(2);
      Main.debug(ex);
      abort();
      if(ex instanceof OutOfMemoryError) return error(PROCOUTMEM);

      final Object[] st = ex.getStackTrace();
      final Object[] obj = new Object[st.length + 1];
      obj[0] = ex.toString();
      System.arraycopy(st, 0, obj, 1, st.length);
      return error(Main.bug(obj));
    }
  }

  /**
   * Runs the command without permission, data and concurrency checks.
   * @param ctx query context
   * @return result of check
   */
  public final boolean run(final Context ctx) {
    return run(ctx, null);
  }

  /**
   * Returns command information or error message.
   * @return info string
   */
  public final String info() {
    return info.toString();
  }

  /**
   * Returns the result set, generated by the last query.
   * Must only be called if {@link Prop#CACHEQUERY} is set.
   * @return result set
   */
  public final Result result() {
    return result;
  }

  /**
   * Tests if the command performs updates/write operations, and thus needs to
   * lock other commands.
   * @param ctx context reference
   * @return result of check
   */
  @SuppressWarnings("unused")
  public boolean updating(final Context ctx) {
    return false;
  }

  // PROTECTED METHODS ========================================================

  /**
   * Executes the command and serializes the result.
   * @return success of operation
   * @throws IOException I/O exception
   */
  protected abstract boolean run() throws IOException;

  /**
   * Adds the error message to the message buffer {@link #info}.
   * @param msg error message
   * @param ext error extension
   * @return {@code false}
   */
  protected final boolean error(final String msg, final Object... ext) {
    info.reset();
    info.add(msg == null ? "" : msg, ext);
    return false;
  }

  /**
   * Adds information on command execution.
   * @param str information to be added
   * @param ext extended info
   * @return {@code true}
   */
  protected final boolean info(final String str, final Object... ext) {
    if(prop.is(Prop.INFO)) {
      info.add(str, ext);
      info.add(Prop.NL);
    }
    return true;
  }

  /**
   * Returns the specified command option.
   * @param typ options enumeration
   * @param <E> token type
   * @return option
   */
  protected final <E extends Enum<E>> E getOption(final Class<E> typ) {
    try {
      return Enum.valueOf(typ, args[0].toUpperCase());
    } catch(final Exception ex) {
      error(CMDWHICH, args[0]);
      return null;
    }
  }

  /**
   * Builds a string representation from the command. This string must be
   * correctly built, as commands are sent to the server as strings.
   * @param cb command builder
   */
  public void build(final CommandBuilder cb) {
    cb.init().args();
  }

  /**
   * Returns a string representation of the command.
   * @return string representation
   */
  @Override
  public final String toString() {
    final CommandBuilder cb = new CommandBuilder(this);
    build(cb);
    return cb.toString();
  }
}