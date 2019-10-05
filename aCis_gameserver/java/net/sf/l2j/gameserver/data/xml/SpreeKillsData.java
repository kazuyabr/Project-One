package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.util.StatsSet;

import net.sf.l2j.gameserver.model.holder.IntIntHolder;

import org.w3c.dom.Document;

/**
 * @author Williams
 *
 */
public class SpreeKillsData implements IXmlReader
{
	private final List<SpreeKills> _spree = new ArrayList<>();
	
	public SpreeKillsData()
	{
		load();
	}
	
	public void reload()
	{
		_spree.clear();
		load();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/spreeKills.xml");
		LOGGER.info("Loaded {} spreeKills data.", _spree.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "SpreeKills", node ->
		{
			final StatsSet set = parseAttributes(node);
			_spree.add(new SpreeKills(set));
		}));
		
	}

	public List<SpreeKills> getSpreeKills()
	{
		return _spree;
	}

	public class SpreeKills
	{
		private final int _type;
		private final int _kill;
		private final String _msg;
		private final String _sound;
		private final List<IntIntHolder> _reward;
		
		public SpreeKills(StatsSet set)
		{
			_type = set.getInteger("type");
			_kill = set.getInteger("kills");
			_msg = set.getString("msg");
			_sound = set.getString("sound");
			_reward = set.getIntIntHolderList("reward");
		}

		public int getType()
		{
			return _type;
		}
		
		public int getKills()
		{
			return _kill;
		}
		
		public String getMsg()
		{
			return _msg;
		}

		public String getSound()
		{
			return _sound;
		}

		public List<IntIntHolder> getReward()
		{
			return _reward;
		}
	}
	
	public static SpreeKillsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SpreeKillsData INSTANCE = new SpreeKillsData();
	}
}