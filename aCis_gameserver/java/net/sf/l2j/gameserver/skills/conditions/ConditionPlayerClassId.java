package net.sf.l2j.gameserver.skills.conditions;

import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.skills.Env;

/**
 * @author Williams
 */
public class ConditionPlayerClassId extends Condition
{
	private final ClassId _class;
	
	public ConditionPlayerClassId(ClassId race)
	{
		_class = race;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		if (env.getPlayer() == null)
			return false;
			
		return env.getPlayer().getClassId() == _class && !env.getPlayer().isInOlympiadMode();
	}
}