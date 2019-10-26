package net.sf.l2j.gameserver.taskmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author williams
 *
 */
public final class VipTaskManager implements Runnable
{
	private final Map<Player, Long> _players = new ConcurrentHashMap<>();
	
	protected VipTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
	}

	/**
	 * Adds {@link Player} to the VipTask.
	 * @param player : Player to be added and checked.
	 */
	public final void add(Player player)
	{
		_players.put(player, System.currentTimeMillis());
	}
	
	public final void remove(Player player)
	{
		_players.remove(player);
	}

	@Override
	public final void run()
	{
		// List is empty, skip.
		if (_players.isEmpty())
			return;
		
		// Loop all players.
		for (Map.Entry<Player, Long> entry : _players.entrySet())
		{
			// Get player.
			final Player player = entry.getKey();
			
			if (player.getMemos().getLong("vipTime") < System.currentTimeMillis())
			{
				player.setVip(false);
				remove(player);
			}
		}
	}
	
	public static final VipTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final VipTaskManager _instance = new VipTaskManager();
	}
}