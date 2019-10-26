package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Williams
 *
 */
public class BugReport extends Folk
{
	private static final Logger REPORT_LOG = Logger.getLogger("report");
	
	public BugReport(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("send_report"))
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			
			String msg = "";
			String type = st.nextToken();

			GameClient info = player.getClient().getConnection().getClient();
			
			try
			{
				while (st.hasMoreTokens())
					msg = msg + st.nextToken() + " ";

				if (msg.equals(""))
				{
					player.sendMessage("A caixa de mensagem não pode estar vazia.");
					return;
				}

				switch (type)
				{
					case "Armaduras":
						break;
					case "Boss":
						break;
					case "Skills":
						break;
					case "Quests":
						break;
					case "Other":
						break;		
				}
				
				REPORT_LOG.info("Character Info: " + info + "\r\nBug Type: " + type + "\r\nMessage: " + msg);
				player.sendMessage("Relatério enviado. Os Gms irão verifica-la em breve, obrigado.");
				
				for (Player allgms : AdminData.getInstance().getAllGms(true))
					allgms.sendMessage("Report Manager: "+ player.getName() + " enviou um relatório de bug.");
			}
			catch (Exception e)
			{
				player.sendMessage("Algo deu errado, tente novamente.");
			}
		}
		
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		String name = "data/html/report/" + getNpcId() + ".htm";
		if (val != 0)
			name = "data/html/report/" + getNpcId() + "-" + val + ".htm";
		
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(name);
		html.replace("%objectId%", getObjectId());
		html.replace("%player%", player.getName());
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}