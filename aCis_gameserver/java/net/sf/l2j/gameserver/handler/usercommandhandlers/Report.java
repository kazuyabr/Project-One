package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Williams
 *
 */
public class Report implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		122
	};
	
	@Override
	public boolean useUserCommand(int id, Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/report.htm");
		html.replace("%name%", activeChar.getName());
		activeChar.sendPacket(html);
		return true;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}	
}