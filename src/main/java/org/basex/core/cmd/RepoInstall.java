package org.basex.core.cmd;

import static org.basex.core.Text.*;

import java.io.IOException;

import org.basex.core.Command;
import org.basex.core.CommandBuilder;
import org.basex.core.Commands.Cmd;
import org.basex.core.Commands.CmdRepo;
import org.basex.core.User;
import org.basex.query.QueryException;
import org.basex.query.util.pkg.RepoManager;
import org.basex.util.InputInfo;
import org.basex.util.Util;

/**
 * Evaluates the 'repo install' command.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Rositsa Shadura
 */
public final class RepoInstall extends Command {
  /** Input info. */
  private final InputInfo ii;

  /**
   * Constructor.
   * @param p package
   * @param i input info
   */
  public RepoInstall(final String p, final InputInfo i) {
    super(User.ADMIN, p);
    ii = i;
  }

  @Override
  protected boolean run() throws IOException {
    try {
      new RepoManager(context.repo).install(args[0], ii);
      return info(REPOINST, args[0], perf);
    } catch(final QueryException ex) {
      Util.debug(ex);
      return error(ex.getMessage());
    }
  }

  @Override
  public void build(final CommandBuilder cb) {
    cb.init(Cmd.REPO + " " + CmdRepo.INSTALL).args();
  }
}
