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
package org.l2jmobius.gameserver.network.clientpackets.compound;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.CombinationItemsData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.CompoundRequest;
import org.l2jmobius.gameserver.model.item.combination.CombinationItem;
import org.l2jmobius.gameserver.model.item.combination.CombinationItemReward;
import org.l2jmobius.gameserver.model.item.combination.CombinationItemType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExItemAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.compound.ExEnchantFail;
import org.l2jmobius.gameserver.network.serverpackets.compound.ExEnchantOneFail;
import org.l2jmobius.gameserver.network.serverpackets.compound.ExEnchantSucess;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author UnAfraid
 */
public class RequestNewEnchantTry implements ClientPacket
{
	@Override
	public void run(GameClient client)
	{
		final Player player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_IN_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.sendPacket(ExEnchantOneFail.STATIC_PACKET);
			return;
		}
		
		if (player.isProcessingTransaction() || player.isProcessingRequest())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_SYSTEM_DURING_TRADING_PRIVATE_STORE_AND_WORKSHOP_SETUP);
			player.sendPacket(ExEnchantOneFail.STATIC_PACKET);
			return;
		}
		
		final CompoundRequest request = player.getRequest(CompoundRequest.class);
		if ((request == null) || request.isProcessing())
		{
			player.sendPacket(ExEnchantFail.STATIC_PACKET);
			return;
		}
		
		request.setProcessing(true);
		
		final Item itemOne = request.getItemOne();
		final Item itemTwo = request.getItemTwo();
		if ((itemOne == null) || (itemTwo == null))
		{
			player.sendPacket(ExEnchantFail.STATIC_PACKET);
			player.removeRequest(request.getClass());
			return;
		}
		
		// Lets prevent using same item twice. Also stackable item check.
		if ((itemOne.getObjectId() == itemTwo.getObjectId()) && (!itemOne.isStackable() || (player.getInventory().getInventoryItemCount(itemOne.getTemplate().getId(), -1) < 2)))
		{
			player.sendPacket(new ExEnchantFail(itemOne.getId(), itemTwo.getId()));
			player.removeRequest(request.getClass());
			return;
		}
		
		final CombinationItem combinationItem = CombinationItemsData.getInstance().getItemsBySlots(itemOne.getId(), itemOne.getEnchantLevel(), itemTwo.getId(), itemTwo.getEnchantLevel());
		
		// Not implemented or not able to merge!
		if (combinationItem == null)
		{
			player.sendPacket(new ExEnchantFail(itemOne.getId(), itemTwo.getId()));
			player.removeRequest(request.getClass());
			return;
		}
		
		if (combinationItem.getCommission() > player.getAdena())
		{
			client.sendPacket(new ExEnchantFail(itemOne.getId(), itemTwo.getId()));
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			return;
		}
		
		// Calculate compound result.
		final double random = (Rnd.nextDouble() * 100);
		final boolean success = random <= combinationItem.getChance();
		final CombinationItemReward rewardItem = combinationItem.getReward(success ? CombinationItemType.ON_SUCCESS : CombinationItemType.ON_FAILURE);
		
		// Add item (early).
		final Item item = player.addItem("Compound-Result", rewardItem.getId(), rewardItem.getCount(), rewardItem.getEnchantLevel(), null, true);
		
		// Send success or fail packet.
		if (success)
		{
			player.sendPacket(new ExEnchantSucess(item.getId()));
			if (combinationItem.isAnnounce())
			{
				Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.COMPOUND));
			}
		}
		else
		{
			player.sendPacket(new ExEnchantFail(item.getId(), itemTwo.getId()));
		}
		
		// Take required items.
		if (player.destroyItem("Compound-Item-One", itemOne, 1, null, true) && player.destroyItem("Compound-Item-Two", itemTwo, 1, null, true) && ((combinationItem.getCommission() <= 0) || player.reduceAdena("Compound-Commission", combinationItem.getCommission(), player, true)))
		{
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(item);
			if (itemOne.isStackable() && (itemOne.getCount() > 0))
			{
				iu.addModifiedItem(itemOne);
			}
			else
			{
				iu.addRemovedItem(itemOne);
			}
			if (itemTwo.isStackable() && (itemTwo.getCount() > 0))
			{
				iu.addModifiedItem(itemTwo);
			}
			else
			{
				iu.addRemovedItem(itemTwo);
			}
			player.sendInventoryUpdate(iu);
		}
		
		player.removeRequest(request.getClass());
	}
}
