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
package org.l2jmobius.gameserver.network.serverpackets.magiclamp;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Serenitty
 */
public class ExMagicLampInfo extends ServerPacket
{
	private final Player _player;
	
	public ExMagicLampInfo(Player player)
	{
		_player = player;
	}
	
	@Override
	public void write()
	{
		ServerPackets.EX_MAGICLAMP_INFO.writeId(this);
		
		final int nExpPercentage = _player.getLampExp() / 10;
		writeInt(nExpPercentage);
		
		final int count = (int) _player.getStat().getValue(Stat.LAMP_BONUS_EXP, 0);
		final int bonus = (int) _player.getStat().getValue(Stat.LAMP_BONUS_BUFFS_COUNT, 0);
		writeInt(bonus);
		writeInt(count);
	}
}
