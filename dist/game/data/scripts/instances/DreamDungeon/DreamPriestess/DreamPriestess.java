/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package instances.DreamDungeon.DreamPriestess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.AbstractPlayerGroup;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import instances.AbstractInstance;
import instances.DreamDungeon.CatGuildsLair.CatGuildsLair;
import instances.DreamDungeon.DraconidFortress.DraconidFortress;

/**
 * @author Index
 */
public class DreamPriestess extends AbstractInstance
{
	private static final Map<Integer, Set<Player>> PLAYER_LIST_TO_ENTER = new HashMap<>();
	private static final List<Integer> INSTANCE_IDS = new ArrayList<>(List.of(221, 222, 223, 224));
	private static final int DREAM_PRIESTESS = 34304;
	
	private DreamPriestess()
	{
		super(0);
		addCondMinLevel(76, DREAM_PRIESTESS + "-noreq.htm");
		addFirstTalkId(DREAM_PRIESTESS);
		addStartNpc(DREAM_PRIESTESS);
		addTalkId(DREAM_PRIESTESS);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return DREAM_PRIESTESS + ".htm";
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		if (player == null)
		{
			return super.onAdvEvent(event, npc, null);
		}
		
		if (event.startsWith("enter_dream_dungeon"))
		{
			final Instance currentInstance = InstanceManager.getInstance().getPlayerInstance(player, false);
			if ((currentInstance != null) && INSTANCE_IDS.contains(currentInstance.getTemplateId()))
			{
				enterInstance(player, npc, currentInstance.getTemplateId());
				return null;
			}
			
			// for don't call many methods
			PLAYER_LIST_TO_ENTER.put(player.getObjectId(), new HashSet<>());
			PLAYER_LIST_TO_ENTER.get(player.getObjectId()).add(player);
			
			final int dungeonId;
			if (player.isGM())
			{
				final String[] split = event.split(" ");
				if (split.length <= 1)
				{
					PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
					return DREAM_PRIESTESS + "-gm.htm";
				}
				
				dungeonId = Integer.parseInt(split[1]);
				if (dungeonId == 999999)
				{
					enterInstance(player, npc, DraconidFortress.INSTANCE_ID);
					ThreadPool.schedule(() -> CatGuildsLair.startCatLairInstance(player), 5000);
					return super.onAdvEvent(event, npc, player);
				}
				
				if (!INSTANCE_IDS.contains(dungeonId))
				{
					PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
					player.sendMessage("Wrong instance ID");
					return DREAM_PRIESTESS + "-gm.htm";
				}
			}
			else
			{
				dungeonId = INSTANCE_IDS.get(Rnd.get(1, INSTANCE_IDS.size()) - 1);
			}
			
			// zone not available in solo, but GM can enter
			// zone will be work if comment this check
			if (!player.isInParty() && !player.isGM())
			{
				PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
				return DREAM_PRIESTESS + "-noreq.htm";
			}
			
			if (!player.isInCommandChannel() && (event.split(" ").length == 1))
			{
				PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
				return DREAM_PRIESTESS + "-02.htm";
			}
			
			if (checkRequirementsForEnter(player))
			{
				PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
				return DREAM_PRIESTESS + "-noreq.htm";
			}
			
			if (InstanceManager.getInstance().getWorldCount(dungeonId) > InstanceManager.getInstance().getInstanceTemplate(dungeonId).getMaxWorlds())
			{
				PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
				player.sendPacket(SystemMessageId.THE_MAXIMUM_NUMBER_OF_INSTANCE_ZONES_HAS_BEEN_EXCEEDED_YOU_CANNOT_ENTER);
				return DREAM_PRIESTESS + "-noreq.htm";
			}
			
			PLAYER_LIST_TO_ENTER.get(player.getObjectId()).forEach(p -> enterInstance(p, npc, dungeonId));
			PLAYER_LIST_TO_ENTER.remove(player.getObjectId());
		}
		else if (event.equalsIgnoreCase("back"))
		{
			return DREAM_PRIESTESS + ".htm";
		}
		else if (event.equalsIgnoreCase("gm_dream_dungeon_reset") && player.isGM())
		{
			for (int instanceId : INSTANCE_IDS)
			{
				InstanceManager.getInstance().deleteInstanceTime(player, instanceId);
			}
			return DREAM_PRIESTESS + "-gm.htm";
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	private static boolean checkRequirementsForEnter(Player requestor)
	{
		final AbstractPlayerGroup group = requestor.isInParty() ? requestor.getParty() : requestor.isInCommandChannel() ? requestor.getCommandChannel() : null;
		if (group == null)
		{
			return checkInstanceStatus(requestor);
		}
		
		if (!group.isLeader(requestor))
		{
			return true;
		}
		
		if (requestor.isInParty() && (group.getMemberCount() < 2))
		{
			requestor.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
			return true;
		}
		
		if (requestor.isInCommandChannel() && (group.getMemberCount() > 10))
		{
			requestor.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_DUE_TO_THE_PARTY_HAVING_EXCEEDED_THE_LIMIT);
			return true;
		}
		
		for (Player player : group.getMembers())
		{
			if (player.getLevel() < 76)
			{
				requestor.sendPacket(new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_LEVEL_REQUIREMENTS_AND_CANNOT_ENTER).addPcName(player));
				player.sendPacket(new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_LEVEL_REQUIREMENTS_AND_CANNOT_ENTER).addPcName(player));
				return true;
			}
			
			if (checkInstanceStatus(player))
			{
				return true;
			}
		}
		
		PLAYER_LIST_TO_ENTER.get(requestor.getObjectId()).addAll(group.getMembers());
		return false;
	}
	
	private static boolean checkInstanceStatus(Player player)
	{
		final long currentTime = System.currentTimeMillis();
		for (Integer instanceId : INSTANCE_IDS)
		{
			if (currentTime < InstanceManager.getInstance().getInstanceTime(player, instanceId))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.C1_CANNOT_ENTER_YET).addString(player.getName()));
				return true;
			}
			
			if (InstanceManager.getInstance().getPlayerInstance(player, true) != null)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_ENTER_AS_C1_IS_IN_ANOTHER_INSTANCE_ZONE).addString(player.getName()));
				return true;
			}
			
			if (InstanceManager.getInstance().getPlayerInstance(player, false) != null)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_ENTER_AS_C1_IS_IN_ANOTHER_INSTANCE_ZONE).addString(player.getName()));
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args)
	{
		new DreamPriestess();
	}
}
