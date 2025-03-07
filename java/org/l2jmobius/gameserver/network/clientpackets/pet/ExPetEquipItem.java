package org.l2jmobius.gameserver.network.clientpackets.pet;

import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.network.ReadablePacket;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.CtrlEvent;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.ai.NextAction;
import org.l2jmobius.gameserver.enums.PrivateStoreType;
import org.l2jmobius.gameserver.instancemanager.FortManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.ArmorType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.pet.ExPetSkillList;
import org.l2jmobius.gameserver.network.serverpackets.pet.PetSummonInfo;

/**
 * @author Berezkin Nikolay
 */
public class ExPetEquipItem implements ClientPacket
{
	private int _objectId;
	private int _itemId;
	
	@Override
	public void read(ReadablePacket packet)
	{
		_objectId = packet.readInt();
	}
	
	@Override
	public void run(GameClient client)
	{
		final Player player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		final Pet pet = player.getPet();
		if (pet == null)
		{
			return;
		}
		
		// Flood protect UseItem
		if (!client.getFloodProtectors().canUseItem())
		{
			return;
		}
		
		if (player.isInsideZone(ZoneId.JAIL))
		{
			player.sendMessage("You cannot use items while jailed.");
			return;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.cancelActiveTrade();
		}
		
		if (player.getPrivateStoreType() != PrivateStoreType.NONE)
		{
			player.sendPacket(SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_objectId);
		if (item == null)
		{
			return;
		}
		
		// No UseItem is allowed while the player is in special conditions
		if (player.hasBlockActions() || player.isControlBlocked() || player.isAlikeDead())
		{
			return;
		}
		
		// Char cannot use item when dead
		if (player.isDead() || pet.isDead() || !player.getInventory().canManipulateWithItemId(item.getId()))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
			sm.addItemName(item);
			player.sendPacket(sm);
			return;
		}
		
		if (!item.isEquipable())
		{
			return;
		}
		
		_itemId = item.getId();
		if (player.isFishing() && ((_itemId < 6535) || (_itemId > 6540)))
		{
			// You cannot do anything else while fishing
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_3);
			return;
		}
		
		player.onActionRequest();
		
		if (item.isEquipable())
		{
			if (pet.getInventory().isItemSlotBlocked(item.getTemplate().getBodyPart()))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
				return;
			}
			// Prevent players to equip weapon while wearing combat flag
			// Don't allow weapon/shield equipment if a cursed weapon is equipped.
			if ((item.getTemplate().getBodyPart() == ItemTemplate.SLOT_LR_HAND) || (item.getTemplate().getBodyPart() == ItemTemplate.SLOT_L_HAND) || (item.getTemplate().getBodyPart() == ItemTemplate.SLOT_R_HAND))
			{
				if ((player.getActiveWeaponItem() != null) && (player.getActiveWeaponItem().getId() == FortManager.ORC_FORTRESS_FLAG))
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
					return;
				}
			}
			else if (item.getTemplate().getBodyPart() == ItemTemplate.SLOT_DECO)
			{
				if (!item.isEquipped() && (player.getInventory().getTalismanSlots() == 0))
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
					return;
				}
			}
			else if (item.getTemplate().getBodyPart() == ItemTemplate.SLOT_BROOCH_JEWEL)
			{
				if (!item.isEquipped() && (player.getInventory().getBroochJewelSlots() == 0))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_CANNOT_EQUIP_S1_WITHOUT_EQUIPPING_A_BROOCH);
					sm.addItemName(item);
					player.sendPacket(sm);
					return;
				}
			}
			else if (item.getTemplate().getBodyPart() == ItemTemplate.SLOT_AGATHION)
			{
				if (!item.isEquipped() && (player.getInventory().getAgathionSlots() == 0))
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
					return;
				}
			}
			else if (item.getTemplate().getBodyPart() == ItemTemplate.SLOT_ARTIFACT)
			{
				if (!item.isEquipped() && (player.getInventory().getArtifactSlots() == 0))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
					sm.addItemName(item);
					player.sendPacket(sm);
					return;
				}
			}
			
			// Pets cannot use enchanted weapons.
			if (item.isWeapon() && item.isEnchanted())
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
				return;
			}
			
			// Pets cannot use shields or sigils.
			if (item.isArmor() && ((item.getArmorItem().getItemType() == ArmorType.SHIELD) || (item.getArmorItem().getItemType() == ArmorType.SIGIL)))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
				return;
			}
			
			// Pets cannot use time-limited items.
			if (item.isTimeLimitedItem() || item.isShadowItem())
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
				return;
			}
			
			// Pets cannot use Event items.
			if ((item.getTemplate().getAdditionalName() != null) && item.getTemplate().getAdditionalName().toLowerCase().contains("event"))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
				return;
			}
			
			final Item oldItem = pet.getInventory().getPaperdollItemByItemId((int) item.getTemplate().getBodyPart());
			if (oldItem != null)
			{
				pet.transferItem("UnequipFromPet", oldItem.getObjectId(), 1, player.getInventory(), player, null);
			}
			if (player.isCastingNow())
			{
				// Create and Bind the next action to the AI.
				player.getAI().setNextAction(new NextAction(CtrlEvent.EVT_FINISH_CASTING, CtrlIntention.AI_INTENTION_CAST, () ->
				{
					final Item transferedItem = player.transferItem("UnequipFromPet", item.getObjectId(), 1, pet.getInventory(), null);
					pet.useEquippableItem(transferedItem, false);
					sendInfos(pet, player);
				}));
			}
			else if (player.isAttackingNow())
			{
				// Equip or unEquip.
				ThreadPool.schedule(() ->
				{
					final Item transferedItem = player.transferItem("UnequipFromPet", item.getObjectId(), 1, pet.getInventory(), null);
					pet.useEquippableItem(transferedItem, false);
					sendInfos(pet, player);
				}, player.getAttackEndTime() - TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()));
			}
			else
			{
				final Item transferedItem = player.transferItem("UnequipFromPet", item.getObjectId(), 1, pet.getInventory(), null);
				pet.useEquippableItem(transferedItem, false);
				sendInfos(pet, player);
			}
		}
	}
	
	private void sendInfos(Pet pet, Player player)
	{
		pet.getStat().recalculateStats(true);
		player.sendPacket(new PetSummonInfo(pet, 1));
		player.sendPacket(new ExPetSkillList(false, pet));
	}
}
