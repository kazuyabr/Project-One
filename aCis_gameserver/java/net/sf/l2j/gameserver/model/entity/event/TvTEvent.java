package net.sf.l2j.gameserver.model.entity.event;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.EventState;
import net.sf.l2j.gameserver.enums.MessageType;
import net.sf.l2j.gameserver.enums.TeamType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.model.entity.event.imp.Event;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.spawn.L2Spawn;
import net.sf.l2j.gameserver.network.serverpackets.ChangeWaitType;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

/**
 * @author Williams
 */
public class TvTEvent extends Event
{
	// TvT related lists
	private Set<Player> _registered = ConcurrentHashMap.newKeySet();
	private Set<Player> _blueTeam = ConcurrentHashMap.newKeySet();
	private Set<Player> _redTeam = ConcurrentHashMap.newKeySet();
	
	private Npc _npcManager;
	
	public Future<?> _registrationTask = null;
	
	private TvTEvent()
	{
		// Event has already started, so do not reload anything
		if (_state == EventState.STARTED)
			return;
		
		// Clean up
		_registered.clear();
		_redTeam.clear();
		_blueTeam.clear();
		
		if (Config.TVT_ENABLE)
		{
			LOGGER.info("TvT Event: Initialized Event");
			
			if (_state == EventState.INITIAL || _state == EventState.SCHEDULED_NEXT)
			{
				_state = EventState.INITIAL;
				scheduleRegistration();
			}
		}
	}
	
	private void scheduleRegistration()
	{
		// If registration task is currently running, cancel it now
		if (_registrationTask != null)
		{
			_registrationTask.cancel(false);
			_registrationTask = null;
		}
		
		// Delete registration NPC if spawned
		if (_npcManager != null)
		{
			if (_npcManager.isVisible())
				_npcManager.deleteMe();
		}
		
		// Start task
		if (Config.TVT_ENABLE)
		{
			if (_state != EventState.SCHEDULED_NEXT)
			{
				// Set state
				_state = EventState.SCHEDULED_NEXT;
				
				World.announceToOnlinePlayers("TvT Event: Next event in " + (Config.EVENT_DELAY / 60) + " minute(s)", true);
				LOGGER.info("TvT Event: Next event in " + (Config.EVENT_DELAY / 60) + " minute(s)");
				_registrationTask = ThreadPool.schedule(new RegistrationTask(), Config.EVENT_DELAY * 1000L);
			}
		}
	}
	
	protected void scheduleEvent()
	{
		// Set state
		_state = EventState.REGISTER;
		
		// Spawn TvT manager NPC
		try
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(Config.TVT_NPC_ID);
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(Config.TVT_NPC_LOCATION.getX(), Config.TVT_NPC_LOCATION.getY(), Config.TVT_NPC_LOCATION.getZ(), 0);
			spawn.setRespawnDelay(60000);
			spawn.setRespawnState(false);
			
			SpawnTable.getInstance().addSpawn(spawn, false);
			
			_npcManager = spawn.doSpawn(false);
			_npcManager.broadcastPacket(new MagicSkillUse(_npcManager, _npcManager, 1034, 1, 1, 1));
		}
		catch (Exception e)
		{
			return;
		}
		
		World.announceToOnlinePlayers("TvT Event: Registration opened for " + (Config.PARTICIPATION_TIME / 60) + " minute(s).", true);
		World.announceToOnlinePlayers("TvT Event: Recruiting levels: " + Config.MIN_LEVEL + " to " + Config.MAX_LEVEL + ".", true);
		World.announceToOnlinePlayers("TvT Event: Max players in team: "+ Config.MAX_PARTICIOANTS +".", true);
		World.announceToOnlinePlayers("TvT Event: Commands /register /unregister.", true);
		
		for (IntIntHolder reward: Config.TVT_REWARDS)
			World.announceToOnlinePlayers("TvT Event: Reward "+ ItemTable.getInstance().getTemplate(reward.getId()).getName() +","+ reward.getValue(), true);
		
		// Start timer
		eventTimer(Config.PARTICIPATION_TIME);
		
		if ((_registered.size() >= 2) && (_state != EventState.INITIAL))
		{
			// Close doors
			toggleArenaDoors(false);
			
			// Port players and start event
			World.announceToOnlinePlayers("TvT Event: Event has started!", true);
			portTeamsToArena();
			eventTimer(Config.EVENT_DURATION);
			
			if (_state == EventState.INITIAL)
				World.announceToOnlinePlayers("TvT Event: Event was cancelled.", true);
			else
				World.announceToOnlinePlayers("TvT Event: Blue Team kills: " + _blueTeamKills + " , Red Team kills: " + _redTeamKills + ".", true);
			
			// Shutting down event
			eventRemovals();
		}
		else
		{
			if (_state == EventState.INITIAL)
				World.announceToOnlinePlayers("TvT Event: Event was cancelled.", true);
			else
				World.announceToOnlinePlayers("TvT Event: Event was cancelled due to lack of participation.", true);
			
			_registered.clear();
		}
		
		// Open doors
		toggleArenaDoors(true);
		
		_state = EventState.INITIAL;
		
		// Schedule next registration
		scheduleRegistration();
	}
	
	private static void toggleArenaDoors(boolean open)
	{
		for (String doorId : Config.TVT_DOOR_LIST)
		{
			final Door door = DoorData.getInstance().getDoor(Integer.parseInt(doorId));
			if (door != null)
			{
				if (open)
					door.openMe();
				else
					door.closeMe();
			}
		}
	}
	
	private void eventRemovals()
	{
		TeamType teamWinner = TeamType.NONE;
		
		for (Player player : World.getInstance().getPlayers())
		{
			if (player == null)
				continue;
			
			if (_state != EventState.INITIAL && (_blueTeamKills > _redTeamKills || _blueTeamKills == _redTeamKills && Config.REWARD_DIE))
				teamWinner = TeamType.BLUE;
			else if (_state != EventState.INITIAL && (_blueTeamKills < _redTeamKills || _blueTeamKills == _redTeamKills && Config.REWARD_DIE))
				teamWinner = TeamType.RED;
			
			if (player.getTeam() == teamWinner)
			{
				for (IntIntHolder reward : Config.TVT_REWARDS)
					player.addItem("TvTReward", reward.getId(), reward.getValue(), null, true);
			}
			
			if (player.isDead())
				player.doRevive();
			
			removePlayer(player);
			player.teleToLocation(player.getOriginalCoordinates());
		}
		
		if (_blueTeamKills == _redTeamKills && !Config.REWARD_DIE)
			World.announceToOnlinePlayers("TvT Event: Event ended in a Tie. No rewards will be given!", true);
		
		_blueTeam.clear();
		_redTeam.clear();
		_blueTeamKills = 0;
		_redTeamKills = 0;
	}
	
	private void eventTimer(int time)
	{
		for (int seconds = time; (seconds > 0 && _state != EventState.INITIAL); seconds--)
		{
			switch (seconds)
			{
				case 3600:
					if (_state == EventState.STARTED)
						World.announceToOnlinePlayers("TvT Event: " + (seconds / 60) + " hour(s) until event is finished!", true);
					else
						World.announceToOnlinePlayers("TvT Event: " + (seconds / 60) + " hour(s) until registration is closed!", true);
					break;
					
				case 1800: // 30 minutes left
				case 900: // 15 minutes left
				case 600: // 10 minutes left
				case 300: // 5 minutes left
				case 240: // 4 minutes left
				case 180: // 3 minutes left
				case 120: // 2 minutes left
				case 60: // 1 minute left
					if (_state == EventState.STARTED)
						World.announceToOnlinePlayers("TvT Event: " + (seconds / 60) + " minute(s) until event is finished!", true);
					else
						World.announceToOnlinePlayers("TvT Event: " + (seconds / 60) + " minute(s) until registration is closed!", true);
					break;
				case 30: // 30 seconds left
				case 15: // 15 seconds left
				case 5:// 5 seconds left
					if (_state == EventState.STARTED)
						World.announceToOnlinePlayers("TvT Event: " + seconds + " second(s) until event is finished!", true);
					else
						World.announceToOnlinePlayers("TvT Event: " + seconds + " second(s) until registration is closed!", true);
					break;
			}
			
			long oneSecWaitStart = System.currentTimeMillis();
			while ((oneSecWaitStart + 1000L) > System.currentTimeMillis())
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException ie)
				{
				}
			}
		}
	}
	
	private void portTeamsToArena()
	{
		if (_registered.size() > 0)
		{
			Player player = World.getInstance().getPlayer(Rnd.get(_registered.size()));
			
			if (_blueTeam.size() > _redTeam.size())
			{
				_redTeam.add(player);
				player.setTeam(TeamType.RED);
			}
			else
			{
				_blueTeam.add(player);
				player.setTeam(TeamType.BLUE);
			}
			
			if (player.isCastingNow())
				player.abortCast();
			
			player.getAppearance().setVisible();
			
			if (player.isDead())
				player.doRevive();
			else
			{
				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
			}
			
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			
			player.stopCubicsByOthers();
			
			if (player.isMounted())
				player.dismount();
			else
			{
				final Summon summon = player.getSummon();
				if (summon instanceof Pet)
					summon.unSummon(player);
				else if (summon != null)
				{
					summon.stopAllEffectsExceptThoseThatLastThroughDeath();
					summon.abortAttack();
					summon.abortCast();
				}
			}
			
			if (player.getParty() != null)
			{
				Party party = player.getParty();
				party.removePartyMember(player, MessageType.EXPELLED);
			}
			
			if (player.isInDuel())
				player.setDuelState(DuelState.INTERRUPTED);
			
			_registered.remove(player);

			_state = EventState.STARTED;
			
			Location playerCoordinates = new Location(player.getPosition());
			player.setOriginalCoordinates(playerCoordinates);
			
			if (player.getTeam() == TeamType.BLUE)
				player.teleportTo(Config.TVT_BLUE_SPAWN_LOCATION, 0);
			if (player.getTeam() == TeamType.RED)
				player.teleportTo(Config.TVT_RED_SPAWN_LOCATION, 0);

			player.sendMessage("You have been teleported.");
		}
	}
	
	@Override
	public void registerPlayer(Player player)
	{
		if (_state != EventState.REGISTER)
		{
			player.sendMessage("TvT Registration is not in progress.");
			return;
		}
		
		if (player.isFestivalParticipant())
		{
			player.sendMessage("Festival participants cannot register to the event.");
			return;
		}
		
		if (player.isInJail())
		{
			player.sendMessage("Jailed players cannot register to the event.");
			return;
		}

		if (player.isAio())
		{
			player.sendMessage("Aio's players cannot register to the event.");
			return;
		}
		
		if (player.isDead())
		{
			player.sendMessage("Dead players cannot register to the event.");
			return;
		}
		
		if (OlympiadManager.getInstance().isRegisteredInComp(player))
		{
			player.sendMessage("Grand Olympiad participants cannot register to the event.");
			return;
		}
		
		if ((player.getLevel() < Config.MIN_LEVEL) || (player.getLevel() > Config.MAX_LEVEL))
		{
			player.sendMessage("You have not reached the appropriate level to join the event.");
			return;
		}
		
		if (_registered.size() == Config.MAX_PARTICIOANTS)
		{
			player.sendMessage("There is no more room for you to register to the event.");
			return;
		}
		
		for (Player registered : _registered)
		{
			if (registered == null)
				continue;
			
			if (registered.getObjectId() == player.getObjectId())
			{
				player.sendMessage("You are already registered in the TvT event.");
				return;
			}
			
			// Check if dual boxing is not allowed
			if (!Config.DUAL_BOX)
			{
				if ((registered.getClient() == null) || (player.getClient() == null))
					continue;
				
				String ip1 = player.getClient().getConnection().getInetAddress().getHostAddress();
				String ip2 = registered.getClient().getConnection().getInetAddress().getHostAddress();
				if ((ip1 != null) && (ip2 != null) && ip1.equals(ip2))
				{
					player.sendMessage("Your IP is already registered in the TvT event.");
					return;
				}
			}
		}
		
		_registered.add(player);
		
		player.sendMessage("You have registered to participate in the TvT Event.");
		
		super.registerPlayer(player);
	}
	
	/**
	 * Removes player from event.
	 * 
	 * @param player
	 */
	@Override
	public void removePlayer(Player player)
	{
		if (_registered.contains(player))
		{
			_registered.remove(player);
			player.sendMessage("You have been removed from the TvT Event registration list.");
		}
		else if (player.getTeam() == TeamType.BLUE)
			_blueTeam.remove(player);
		else if (player.getTeam() == TeamType.RED)
			_redTeam.remove(player);
		
		// If no participants left, abort event
		if ((player.getTeam().getId() > 0) && (_blueTeam.size() == 0) && (_redTeam.size() == 0))
			_state = EventState.INITIAL;
		
		// Now, remove team status
		player.setTeam(TeamType.NONE);
		
		super.removePlayer(player);
	}
	
	@Override
	public boolean isRegistered(Player player)
	{
		return _registered.contains(player);
	}
	
	public Set<Player> getBlueTeam()
	{
		return _blueTeam;
	}
	
	public Set<Player> getRedTeam()
	{
		return _redTeam;
	}
	
	public Set<Player> getRegistered()
	{
		return _registered;
	}
	
	class RegistrationTask implements Runnable
	{
		public RegistrationTask()
		{
		}
		
		@Override
		public void run()
		{
			if (Config.TVT_ENABLE)
				scheduleEvent();
			else
				_state = EventState.INITIAL; // Default state
		}
	}
	
	@Override
	public void onDie(Creature creature)
	{
		if (creature == null)
			return;
		
		if (creature instanceof Player)
		{
			final Player player = ((Player) creature);
			player.sendMessage("You will be respawned in " + Config.PLAYER_RESPAWN_DELAY + " seconds.");
			player.broadcastPacket(new ChangeWaitType(player, ChangeWaitType.WT_START_FAKEDEATH));
			ThreadPool.schedule(() -> revive(player), Config.PLAYER_RESPAWN_DELAY * 1000);
		}
	}

	public final static void revive(Player player)
	{
		if (!player.isDead())
			return;
		
		player.doRevive();
		
		if (player.getTeam() == TeamType.BLUE)
			player.teleToLocation(Config.TVT_BLUE_SPAWN_LOCATION);
		else if (player.getTeam() == TeamType.RED)
			player.teleToLocation(Config.TVT_RED_SPAWN_LOCATION);
		else
			player.teleportTo(MapRegionData.TeleportType.TOWN);
	}
	
	@Override
	public void onKill(Player player, Player target)
	{
		if (player == null || target == null)
			return;
		
		// Increase kills only if victim belonged to enemy team
		if (player.getTeam() == TeamType.BLUE && target.getTeam() == TeamType.RED)
			_blueTeamKills++;
		else if (player.getTeam() == TeamType.RED && target.getTeam() == TeamType.BLUE)
			_redTeamKills++;
		
		player.broadcastTitleInfo();
		player.broadcastUserInfo();
	}
	
	@Override
	public void onRevive(Creature creature)
	{
		if (creature == null)
			return;
		
		// Heal Player fully
		creature.setCurrentHpMp(creature.getMaxHp(), creature.getMaxMp());
		creature.setCurrentCp(creature.getMaxCp());
		
		ChangeWaitType revive = new ChangeWaitType(creature, ChangeWaitType.WT_STOP_FAKEDEATH);
		creature.broadcastPacket(revive);	
	}

	public static final TvTEvent getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TvTEvent INSTANCE = new TvTEvent();
	}
}