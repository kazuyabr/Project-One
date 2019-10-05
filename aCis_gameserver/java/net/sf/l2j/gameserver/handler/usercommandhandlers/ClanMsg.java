package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Williams
 *
 */
public class ClanMsg implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		116
	};
	
	@Override
	public boolean useUserCommand(int id, Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/clanLeaderMsg.htm");
		html.replace("%clanName%", activeChar.getClan().getName());
		html.replace("%player%", activeChar.getName());
		activeChar.sendPacket(html);
		return true;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}	
}