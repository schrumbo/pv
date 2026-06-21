package schrumbo.pv.data

import net.minecraft.world.item.ItemStack

/** A named item container (Inventory, Ender Chest, …) and its decoded slots (may include empties). */
data class NamedContainer(val name: String, val items: List<ItemStack>)

/** One backpack: its own icon item (the backpack head), its decoded contents, and its full slot count. */
data class Backpack(val icon: ItemStack, val items: List<ItemStack>, val slots: Int)
