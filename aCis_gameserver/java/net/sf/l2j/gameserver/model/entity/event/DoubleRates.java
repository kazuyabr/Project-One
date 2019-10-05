package net.sf.l2j.gameserver.model.entity.event;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.World;

/**
 * @author Williams
 *
 */
public class DoubleRates
{
	private static final CLogger LOGGER = new CLogger(DoubleRates.class.getName());
	
	public DoubleRates()
	{
		ThreadPool.schedule(new StartEvent(), Config.TIME_DOUBLE_RATES * 1000 * 60 );
		LOGGER.warn("Double Rates initiated.");
	}
	
	public class StartEvent implements Runnable
	{
		@Override
		public void run()
		{
			DoubleRates(true);
			try
			{
				Thread.sleep(Config.TIME_DOUBLE_RATES * 1000 * 60);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			DoubleRates(false);
		}
		
		public void DoubleRates(boolean rates)
		{
			if (rates)
			{
				Config.RATE_XP = Config.RATE_MULTIPLER * Config.RATE_XP;
				Config.RATE_SP = Config.RATE_MULTIPLER * Config.RATE_SP;
				Config.RATE_DROP_ITEMS = Config.RATE_MULTIPLER * Config.RATE_DROP_ITEMS;
				Config.RATE_DROP_ITEMS_BY_RAID = Config.RATE_MULTIPLER * Config.RATE_DROP_ITEMS_BY_RAID;
				Config.RATE_DROP_ADENA = Config.RATE_MULTIPLER * Config.RATE_DROP_ADENA;
				World.announceToOnlinePlayers("The Dobro de Rates started you have " + Config.TIME_DOUBLE_RATES + " minutes to release. The rates were multiplied by " + Config.RATE_MULTIPLER);	
			}
			else
			{
				Config.RATE_XP = Config.RATE_XP / Config.RATE_MULTIPLER;
				Config.RATE_SP = Config.RATE_SP / Config.RATE_MULTIPLER;
				Config.RATE_DROP_ITEMS = Config.RATE_DROP_ITEMS / Config.RATE_MULTIPLER;
				Config.RATE_DROP_ITEMS_BY_RAID = Config.RATE_DROP_ITEMS_BY_RAID / Config.RATE_MULTIPLER;
				Config.RATE_DROP_ADENA = Config.RATE_DROP_ADENA / Config.RATE_MULTIPLER;
				World.announceToOnlinePlayers("The rates are restored!!");
			}
		}
	}
	
	public static final DoubleRates getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DoubleRates INSTANCE = new DoubleRates();
	}
}