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
package org.l2jmobius.gameserver.network.clientpackets.newhenna;

import org.l2jmobius.commons.network.ReadablePacket;
import org.l2jmobius.gameserver.data.xml.HennaPatternPotentialData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.henna.DyePotentialFee;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.newhenna.NewHennaList;
import org.l2jmobius.gameserver.network.serverpackets.newhenna.NewHennaPotenEnchantReset;

/**
 * @author Serenitty
 */
public class ExRequestNewHennaEnchantReset implements ClientPacket
{
	@Override
	public void read(ReadablePacket packet)
	{
		packet.readInt(); // nCostItemId
	}
	
	@Override
	public void run(GameClient client)
	{
		final Player player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final int dailyReset = player.getDyePotentialDailyEnchantReset();
		final ItemHolder enchant;
		try
		{
			enchant = HennaPatternPotentialData.getInstance().getEnchantReset().get(dailyReset);
		}
		catch (Exception e)
		{
			return;
		}
		
		if (dailyReset <= 9)
		{
			if (player.destroyItemByItemId("Reset fee", enchant.getId(), enchant.getCount(), player, true))
			{
				final DyePotentialFee newFee = HennaPatternPotentialData.getInstance().getFee(1 /* daily step */);
				player.setDyePotentialDailyCount(newFee.getDailyCount());
				player.setDyePotentialDailyEnchantReset(dailyReset + 1);
				player.sendPacket(new NewHennaPotenEnchantReset(true));
				player.sendPacket(new NewHennaList(player, 1));
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MONEY_TO_USE_THE_FUNCTION));
			}
		}
	}
}
