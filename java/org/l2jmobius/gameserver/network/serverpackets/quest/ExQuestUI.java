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
package org.l2jmobius.gameserver.network.serverpackets.quest;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Magik
 */
public class ExQuestUI extends ServerPacket
{
	private final Player _player;
	private final Collection<QuestState> _allQuests;
	
	public ExQuestUI(Player player)
	{
		_player = player;
		_allQuests = player.getAllQuestStates();
	}
	
	@Override
	public void write()
	{
		if (_player == null)
		{
			return;
		}
		
		ServerPackets.EX_QUEST_UI.writeId(this);
		if (!_allQuests.isEmpty())
		{
			final List<QuestState> activeQuests = new LinkedList<>();
			for (QuestState qs : _allQuests)
			{
				if (qs.isStarted() && !qs.isCompleted())
				{
					activeQuests.add(qs);
				}
			}
			
			writeInt(_allQuests.size());
			_allQuests.forEach(qs ->
			{
				writeInt(qs.getQuest().getId());
				writeInt(qs.getCount());
				writeByte(qs.getState());
			});
			writeInt(activeQuests.size());
		}
		else
		{
			writeInt(0);
			writeInt(0);
		}
	}
}
