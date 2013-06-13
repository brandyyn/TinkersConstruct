package mods.tinker.tconstruct.items.tools;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import mods.tinker.tconstruct.common.TContent;
import mods.tinker.tconstruct.library.ActiveToolMod;
import mods.tinker.tconstruct.library.TConstructRegistry;
import mods.tinker.tconstruct.library.tools.AbilityHelper;
import mods.tinker.tconstruct.library.tools.HarvestTool;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class LumberAxe extends HarvestTool
{
    public LumberAxe(int itemID)
    {
        super(itemID, 0);
        this.setUnlocalizedName("InfiTool.LumberAxe");
    }

    @Override
    protected Material[] getEffectiveMaterials ()
    {
        return materials;
    }

    @Override
    protected String getHarvestType ()
    {
        return "axe";
    }

    @Override
    public float getRepairCost ()
    {
        return 4.0f;
    }

    public float getDurabilityModifier ()
    {
        return 2.5f;
    }

    public String getToolName ()
    {
        return "Lumber Axe";
    }

    @Override
    public boolean onBlockDestroyed (ItemStack itemstack, World world, int bID, int x, int y, int z, EntityLiving player)
    {
        Block block = Block.blocksList[bID];
        if (block != null && block.blockMaterial == Material.leaves)
            return false;

        return AbilityHelper.onBlockChanged(itemstack, world, bID, x, y, z, player, random);
    }

    static Material[] materials = { Material.wood, Material.vine, Material.circuits, Material.cactus, Material.pumpkin };

    /* Lumber axe specific */

    @Override
    public float getStrVsBlock (ItemStack stack, Block block, int meta)
    {
        if (!stack.hasTagCompound())
            return 1.0f;
        
        NBTTagCompound tags = stack.getTagCompound().getCompoundTag("InfiTool");
        if (tags.getBoolean("Broken"))
            return 0.1f;

        Material[] materials = getEffectiveMaterials();
        for (int i = 0; i < materials.length; i++)
        {
            if (materials[i] == block.blockMaterial)
            {
                float speed = tags.getInteger("MiningSpeed");
                speed /= 200f;
                int hlvl = MinecraftForge.getBlockHarvestLevel(block, meta, getHarvestType());
                int durability = tags.getInteger("Damage");

                float shoddy = tags.getFloat("Shoddy");
                speed += shoddy * durability / 100f;

                if (hlvl <= tags.getInteger("HarvestLevel"))
                    return speed;
                return 0.1f;
            }
        }
        return super.getStrVsBlock(stack, block, meta);
    }
    
    @Override
    public boolean onBlockStartBreak (ItemStack stack, int x, int y, int z, EntityPlayer player)
    {
        World world = player.worldObj;
        int woodID = world.getBlockId(x, y, z);
        Block wood = Block.blocksList[woodID];
        if (wood.isWood(world, x, y, z))
        {
            int height = y;
            boolean foundTop = false;
            do
            {
                height++;
                int blockID = world.getBlockId(x, height, z);
                if (blockID != woodID)
                {
                    height--;
                    foundTop = true;
                }
            } while (!foundTop);

            int numLeaves = 0;
            if (height - y < 30)
            {
                for (int xPos = x - 1; xPos <= x + 1; xPos++)
                {
                    for (int yPos = height - 1; yPos <= height + 1; yPos++)
                    {
                        for (int zPos = z - 1; zPos <= z + 1; zPos++)
                        {
                            Block leaves = Block.blocksList[world.getBlockId(xPos, yPos, zPos)];
                            if (leaves != null && leaves.isLeaves(world, xPos, yPos, zPos))
                                numLeaves++;
                        }
                    }
                }
            }

            NBTTagCompound tags = stack.getTagCompound().getCompoundTag("InfiTool");
            int meta = world.getBlockMetadata(x, y, z);
            if (numLeaves > 3)
                breakTree(world, x, y, z, stack, tags, woodID, meta, player);
            else
                destroyWood(world, x, y, z, stack, tags, player);

            if (!world.isRemote)
                world.playAuxSFX(2001, x, y, z, woodID + (meta << 12));
        }
        return super.onBlockStartBreak(stack, x, y, z, player);
    }

    void breakTree (World world, int x, int y, int z, ItemStack stack, NBTTagCompound tags, int bID, int meta, EntityPlayer player)
    {
        for (int xPos = x - 1; xPos <= x + 1; xPos++)
        {
            for (int yPos = y; yPos <= y + 1; yPos++)
            {
                for (int zPos = z - 1; zPos <= z + 1; zPos++)
                {
                    if (!(tags.getBoolean("Broken")))
                    {
                        boolean cancelHarvest = false;
                        for (ActiveToolMod mod : TConstructRegistry.activeModifiers)
                        {
                            if (mod.beforeBlockBreak(this, stack, xPos, yPos, zPos, player))
                                cancelHarvest = true;
                        }

                        if (cancelHarvest)
                        {
                            breakTree(world, xPos, yPos, zPos, stack, tags, bID, meta, player);
                        }
                        else
                        {
                            int localID = world.getBlockId(xPos, yPos, zPos);
                            if (localID == bID && world.getBlockMetadata(xPos, yPos, zPos) % 4 == meta % 4)
                            {
                                world.setBlock(xPos, yPos, zPos, 0, 0, 4);
                                if (!player.capabilities.isCreativeMode)
                                {
                                    Block.blocksList[bID].harvestBlock(world, player, xPos, yPos, zPos, meta);
                                    onBlockDestroyed(stack, world, bID, xPos, yPos, zPos, player);
                                }
                                breakTree(world, xPos, yPos, zPos, stack, tags, bID, meta, player);
                            }
                            /*else
                            {
                                Block leaves = Block.blocksList[localID];
                                if (leaves != null && leaves.isLeaves(world, xPos, yPos, zPos))
                                {
                                    world.setBlockToAir(xPos, yPos, zPos);
                                    if (!player.capabilities.isCreativeMode)
                                    {
                                        Block.blocksList[bID].harvestBlock(world, player, xPos, yPos, zPos, meta);
                                        onBlockDestroyed(stack, world, bID, xPos, yPos, zPos, player);
                                    }
                                }
                            }*/
                        }
                    }
                }
            }
        }
    }

    void destroyWood (World world, int x, int y, int z, ItemStack stack, NBTTagCompound tags, EntityPlayer player)
    {
        for (int xPos = x - 1; xPos <= x + 1; xPos++)
        {
            for (int yPos = y - 1; yPos <= y + 1; yPos++)
            {
                for (int zPos = z - 1; zPos <= z + 1; zPos++)
                {
                    if (!(tags.getBoolean("Broken")))
                    {
                        boolean cancelHarvest = false;
                        for (ActiveToolMod mod : TConstructRegistry.activeModifiers)
                        {
                            if (mod.beforeBlockBreak(this, stack, xPos, yPos, zPos, player))
                                cancelHarvest = true;
                        }

                        if (!cancelHarvest)
                        {
                            int blockID = world.getBlockId(xPos, yPos, zPos);
                            Block block = Block.blocksList[blockID];
                            if (block != null && block.blockMaterial == Material.wood)
                            {
                                int meta = world.getBlockMetadata(xPos, yPos, zPos);
                                world.setBlockToAir(xPos, yPos, zPos);
                                if (!player.capabilities.isCreativeMode)
                                {
                                    Block.blocksList[blockID].harvestBlock(world, player, xPos, yPos, zPos, meta);
                                    onBlockDestroyed(stack, world, blockID, xPos, yPos, zPos, player);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Item getHeadItem ()
    {
        return TContent.broadAxeHead;
    }

    @Override
    public Item getHandleItem ()
    {
        return TContent.toughRod;
    }

    @Override
    public Item getAccessoryItem ()
    {
        return TContent.heavyPlate;
    }

    @Override
    public Item getExtraItem ()
    {
        return TContent.toughBinding;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderPasses (int metadata)
    {
        return 10;
    }

    @Override
    public int getPartAmount ()
    {
        return 4;
    }

    @Override
    public String getIconSuffix (int partType)
    {
        switch (partType)
        {
        case 0:
            return "_lumberaxe_head";
        case 1:
            return "_lumberaxe_head_broken";
        case 2:
            return "_lumberaxe_handle";
        case 3:
            return "_lumberaxe_shield";
        case 4:
            return "_lumberaxe_binding";
        default:
            return "";
        }
    }

    @Override
    public String getEffectSuffix ()
    {
        return "_lumberaxe_effect";
    }

    @Override
    public String getDefaultFolder ()
    {
        return "lumberaxe";
    }

    @Override
    public int durabilityTypeAccessory ()
    {
        return 2;
    }

    @Override
    public int durabilityTypeExtra ()
    {
        return 1;
    }
}