package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.data.manager.HeroManager;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.OlympiadManagerNpc;
import net.sf.l2j.gameserver.model.entity.event.imp.Event;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.taskmanager.NoticeTaskManager;

public final class RequestBypassToServer extends L2GameClientPacket
{
	private static final Logger GMAUDIT_LOG = Logger.getLogger("gmaudit");
	private static final Logger REPORT_LOG = Logger.getLogger("report");
	
	private String _command;
	
	@Override
	protected void readImpl()
	{
		_command = readS();
	}
	
	@Override
	protected void runImpl()
	{
		if (_command.isEmpty())
			return;
		
		if (!FloodProtectors.performAction(getClient(), Action.SERVER_BYPASS))
			return;
		
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (_command.startsWith("admin_"))
		{
			String command = _command.split(" ")[0];
			
			final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(command);
			if (ach == null)
			{
				if (player.isGM())
					player.sendMessage("The command " + command.substring(6) + " doesn't exist.");
				
				LOGGER.warn("No handler registered for admin command '{}'.", command);
				return;
			}
			
			if (!AdminData.getInstance().hasAccess(command, player.getAccessLevel()))
			{
				player.sendMessage("You don't have the access rights to use this command.");
				LOGGER.warn("{} tried to use admin command '{}' without proper Access Level.", player.getName(), command);
				return;
			}
			
			if (Config.GMAUDIT)
				GMAUDIT_LOG.info(player.getName() + " [" + player.getObjectId() + "] used '" + _command + "' command on: " + ((player.getTarget() != null) ? player.getTarget().getName() : "none"));
			
			ach.useAdminCommand(_command, player);
		}
		else if (_command.startsWith("player_help "))
		{
			final String path = _command.substring(12);
			if (path.indexOf("..") != -1)
				return;
			
			final StringTokenizer st = new StringTokenizer(path);
			final String[] cmd = st.nextToken().split("#");
			
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/help/" + cmd[0]);
			if (cmd.length > 1)
			{
				final int itemId = Integer.parseInt(cmd[1]);
				html.setItemId(itemId);
				
				if (itemId == 7064 && cmd[0].equalsIgnoreCase("lidias_diary/7064-16.htm"))
				{
					final QuestState qs = player.getQuestState("Q023_LidiasHeart");
					if (qs != null && qs.getInt("cond") == 5 && qs.getInt("diary") == 0)
						qs.set("diary", "1");
				}
			}
			html.disableValidation();
			player.sendPacket(html);
		}
		else if (_command.startsWith("npc_"))
		{
			if (!player.validateBypass(_command))
				return;
			
			int endOfId = _command.indexOf('_', 5);
			String id;
			if (endOfId > 0)
				id = _command.substring(4, endOfId);
			else
				id = _command.substring(4);
			
			try
			{
				final WorldObject object = World.getInstance().getObject(Integer.parseInt(id));
				
				if (object != null && object instanceof Npc && endOfId > 0 && ((Npc) object).canInteract(player))
					((Npc) object).onBypassFeedback(player, _command.substring(endOfId + 1));
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			catch (NumberFormatException nfe)
			{
			}
		}
		// Navigate throught Manor windows
		else if (_command.startsWith("manor_menu_select?"))
		{
			WorldObject object = player.getTarget();
			if (object instanceof Npc)
				((Npc) object).onBypassFeedback(player, _command);
		}
		else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block"))
		{
			CommunityBoard.getInstance().handleCommands(getClient(), _command);
		}
		else if (_command.startsWith("Quest "))
		{
			if (!player.validateBypass(_command))
				return;
			
			String[] str = _command.substring(6).trim().split(" ", 2);
			if (str.length == 1)
				player.processQuestEvent(str[0], "");
			else
				player.processQuestEvent(str[0], str[1]);
		}
		else if (_command.startsWith("_match"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = HeroManager.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				HeroManager.getInstance().showHeroFights(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("_diary"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = HeroManager.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				HeroManager.getInstance().showHeroDiary(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("arenachange")) // change
		{
			final boolean isManager = player.getCurrentFolk() instanceof OlympiadManagerNpc;
			if (!isManager)
			{
				// Without npc, command can be used only in observer mode on arena
				if (!player.isInObserverMode() || player.isInOlympiadMode() || player.getOlympiadGameId() < 0)
					return;
			}
			
			Event event = player.getEvent();
			if (event != null && event.isStarted())
			{
				player.sendMessage("You can not observe games while registered for an event!");
				return;
			}
			
			if (OlympiadManager.getInstance().isRegisteredInComp(player))
			{
				player.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
				return;
			}
			
			final int arenaId = Integer.parseInt(_command.substring(12).trim());
			player.enterOlympiadObserverMode(arenaId);
		}
		else if (_command.startsWith("clan_notice"))
		{
			StringTokenizer st = new StringTokenizer(_command);
			String announce = _command.substring(9);
			int duration = 5;
			
			if (st.hasMoreTokens())
				duration = Integer.parseInt(st.nextToken());
			
			final Clan clan = player.getClan();
			if (clan != null)
			{
				if (!player.isClanLeader())
				{
					player.sendMessage("You not is Leader of clan "+ clan.getName() +".");
					return;	
				}
				
				if (clan.getLevel() < 5)
				{
					player.sendMessage("Your clan need to be Level 5.");
					return;
				}
				
				player.setNotice(true);
				NoticeTaskManager.getInstance().add(player);
				
				long remainingTime = player.getMemos().getLong("noticeTime", 0);
				if (remainingTime > 0)
					player.getMemos().set("noticeTime", remainingTime + TimeUnit.MINUTES.toMillis(duration));
				else
				{
					player.getMemos().set("noticeTime", System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(duration));
					player.setAnnounce(announce);
				}
			}
		}
		else if (_command.startsWith("submitpin"))
		{
			String value = _command.substring(9);
			StringTokenizer s = new StringTokenizer(value," ");
			int _pin = player.getPin();
			
			if (player.getPincheck())
			{
				_pin = Integer.parseInt(s.nextToken());
				
				if (Integer.toString(_pin).length() != 4)
				{
					player.sendMessage("You have to fill the pin box with 4 numbers.Not more, not less.");
					return;
				}
				
				try (Connection con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("UPDATE characters SET pin=? WHERE obj_id=?"))
				{
					statement.setInt(1, _pin);
					statement.setInt(2, player.getObjectId());
					statement.execute();
					
					player.setPincheck(false);
					player.updatePincheck();
					player.sendMessage("You successfully submitted your pin code.You will need it in order to login.");
					player.sendMessage("Your Pin Code is: " + _pin );
				}
				catch(Exception e)
				{
					LOGGER.warn("could not set char first login:" + e);
				}
			}
		}
		else if(_command.startsWith("enterpin"))
		{
			String value = _command.substring(8);
			StringTokenizer s = new StringTokenizer(value," ");
			int dapin = 0;
			int pin = 0;
			
			dapin = Integer.parseInt(s.nextToken());				
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT pin FROM characters WHERE obj_Id=?"))
			{
				statement.setInt(1, player.getObjectId());
				
				try (ResultSet rset = statement.executeQuery())
				{
					while (rset.next())
						pin = rset.getInt("pin");
					
					if (pin == dapin)
						player.sendMessage("Pin Code Authenticated Successfully.You are now free to move.");
					else
					{
						player.sendMessage("Pin Code does not match with the submitted one.You will now get disconnected!");
						player.logout(true);
					}
				}
			}
			catch (Exception e)
			{
				player.sendMessage("The Pin Code must be 4 numbers.");
			}
		}
		else if (_command.startsWith("antibot"))
		{
			StringTokenizer st = new StringTokenizer(_command);
			st.nextToken();
			
			if (st.hasMoreTokens())
			{
				player.checkCode(st.nextToken());
				return;
			}
			player.checkCode("Fail");
		}
		else if (_command.startsWith("send_report"))
		{
			StringTokenizer st = new StringTokenizer(_command);
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
	}
}