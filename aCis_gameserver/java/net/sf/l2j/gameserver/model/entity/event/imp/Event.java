package net.sf.l2j.gameserver.model.entity.event.imp;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.gameserver.enums.EventState;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author Williams
 *
 */
public abstract class Event
{
	public static final CLogger LOGGER = new CLogger(Event.class.getName());
	
	public abstract boolean isRegistered(Player player);
	public abstract void onDie(Creature creature);
	public abstract void onKill(Player player, Player target);
	public abstract void onRevive(Creature creature);
	
	public int _blueTeamKills = 0;
	public int _redTeamKills = 0;
	
	public EventState _state = EventState.INITIAL;
	
	public boolean isStarted()
	{
		return _state == EventState.STARTED;
	}
	
	public EventState getEventState()
	{
		return _state;
	}
	
	public void setEventState(EventState state)
	{
		_state = state;
	}

	public int getBlueTeamKills()
	{
		return _blueTeamKills;
	}
	
	public int getRedTeamKills()
	{
		return _redTeamKills;
	}
	
	public void registerPlayer(Player player)
	{
		player.setEvent(this);
	}

	public void removePlayer(Player player)
	{
		player.setEvent(null);
	}
}