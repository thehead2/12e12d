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

import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Serenitty, Dims
 */
public class ExMagicLampResult extends ServerPacket
{
	private final int _exp;
	private final int _grade;
	
	public ExMagicLampResult(int exp, int grade)
	{
		_exp = exp;
		_grade = grade;
	}
	
	@Override
	public void write()
	{
		ServerPackets.EX_MAGICLAMP_RESULT.writeId(this);
		writeLong(_exp);
		writeInt(_grade);
	}
}
