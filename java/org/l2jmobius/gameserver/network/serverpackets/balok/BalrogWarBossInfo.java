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
package org.l2jmobius.gameserver.network.serverpackets.balok;

import org.l2jmobius.gameserver.instancemanager.BattleWithBalokManager;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Serenitty, NasSeKa
 */
public class BalrogWarBossInfo extends ServerPacket
{
	private final int _bossState1;
	private final int _bossState2;
	private final int _bossState3;
	private final int _bossState4;
	private final int _bossState5;
	private final int _finalBossId;
	private final int _finalState;
	
	public BalrogWarBossInfo(int balokid, int balokstatus, int boss1, int boss2, int boss3, int boss4, int boss5)
	{
		_finalBossId = balokid + 1000000;
		_finalState = balokstatus;
		_bossState1 = boss1;
		_bossState2 = boss2;
		_bossState3 = boss3;
		_bossState4 = boss4;
		_bossState5 = boss5;
	}
	
	@Override
	public void write()
	{
		ServerPackets.EX_BALROGWAR_BOSSINFO.writeId(this);
		final long globalpoints = BattleWithBalokManager.getInstance().getGlobalPoints();
		final int globalstage = BattleWithBalokManager.getInstance().getGlobalStage();
		if ((globalpoints < 320000) && (globalstage <= 2))
		{
			writeInt(1);
			writeInt(1);
			writeInt(1);
			writeInt(1);
			writeInt(1);
			writeInt(0);
			writeInt(0);
			writeInt(0);
			writeInt(0);
			writeInt(0);
		}
		else
		{
			final int bossId1 = 25956 + 1000000;
			final int bossId2 = 25957 + 1000000;
			final int bossId3 = 25958 + 1000000;
			int bossId4 = 0;
			int bossId5 = 0;
			if ((globalpoints >= 800000) && (globalstage >= 3))
			{
				bossId4 = 25959 + 1000000;
				bossId5 = 25960 + 1000000;
			}
			
			writeInt(bossId1);
			writeInt(bossId2);
			writeInt(bossId3);
			writeInt(bossId4);
			writeInt(bossId5);
			writeInt(_bossState1);
			writeInt(_bossState2);
			writeInt(_bossState3);
			writeInt(_bossState4);
			writeInt(_bossState5);
			writeInt(_finalBossId);
			writeInt(_finalState);
		}
	}
}
