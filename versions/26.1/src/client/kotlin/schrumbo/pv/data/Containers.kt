package schrumbo.pv.data

import net.minecraft.world.item.ItemStack

/** A named item container (Inventory, Ender Chest, …) and its decoded slots (may include empties). */
data class NamedContainer(val name: String, val items: List<ItemStack>)
