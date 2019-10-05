package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.enums.EventState;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.event.TvTEvent;

/**
 * @author Williams
 *
 */
public class AdminEventEngine implements IAdminCommandHandler
{
    private static String[] _adminCommands = 
    {
	   	"admin_tvt_abort"
    };

    @Override
	public boolean useAdminCommand(String command, Player activeChar)
    {
        if (command.startsWith("admin_tvt_abort"))
        {
            if (TvTEvent.getInstance().getEventState() == EventState.STARTED || TvTEvent.getInstance().getEventState() == EventState.REGISTER)
            {
                TvTEvent.getInstance().setEventState(EventState.INITIAL);
                activeChar.sendMessage("TvT Event was successfully aborted.");
            }
            else
                activeChar.sendMessage("TvT Event is not currently in progress.");
        }

        return true;
    }

	@Override
	public String[] getAdminCommandList()
	{
		return _adminCommands;
	}
}