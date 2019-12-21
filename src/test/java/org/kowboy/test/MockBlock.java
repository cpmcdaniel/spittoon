package org.kowboy.test;

import com.destroystokyo.paper.block.BlockSoundGroup;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

public class MockBlock implements Block {

    private final Location location;
    private final Material type;

    public MockBlock(Location location, Material type) {
        this.location = location;
        this.type = type;
    }

    public byte getData() {
        return 0;
    }

    public BlockData getBlockData() {
        return null;
    }

    public Block getRelative(int modX, int modY, int modZ) {
        return null;
    }

    public Block getRelative(BlockFace face) {
        return null;
    }

    public Block getRelative(BlockFace face, int distance) {
        return null;
    }

    public Material getType() {
        return type;
    }

    public byte getLightLevel() {
        return 0;
    }

    public byte getLightFromSky() {
        return 0;
    }

    public byte getLightFromBlocks() {
        return 0;
    }

    public World getWorld() {
        return location.getWorld();
    }

    public int getX() {
        return location.getBlockX();
    }

    public int getY() {
        return location.getBlockY();
    }

    public int getZ() {
        return location.getBlockZ();
    }

    public Location getLocation() {
        return location;
    }

    public Location getLocation(Location loc) {
        if (loc != null) {
            loc.setX(this.getX());
            loc.setY(this.getY());
            loc.setZ(this.getZ());
        }
        return loc;
    }

    public Chunk getChunk() {
        return null;
    }

    public void setBlockData(BlockData data) {

    }

    public void setBlockData(BlockData data, boolean applyPhysics) {

    }

    public void setType(Material type) {

    }

    public void setType(Material type, boolean applyPhysics) {

    }

    public BlockFace getFace(Block block) {
        return null;
    }

    public BlockState getState() {
        return null;
    }

    public BlockState getState(boolean useSnapshot) {
        return null;
    }

    public Biome getBiome() {
        return null;
    }

    public void setBiome(Biome bio) {

    }

    public boolean isBlockPowered() {
        return false;
    }

    public boolean isBlockIndirectlyPowered() {
        return false;
    }

    public boolean isBlockFacePowered(BlockFace face) {
        return false;
    }

    public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
        return false;
    }

    public int getBlockPower(BlockFace face) {
        return 0;
    }

    public int getBlockPower() {
        return 0;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isLiquid() {
        return false;
    }

    public double getTemperature() {
        return 0;
    }

    public double getHumidity() {
        return 0;
    }

    public PistonMoveReaction getPistonMoveReaction() {
        return null;
    }

    public boolean breakNaturally() {
        return false;
    }

    public boolean breakNaturally(ItemStack tool) {
        return false;
    }

    public Collection<ItemStack> getDrops() {
        return null;
    }

    public Collection<ItemStack> getDrops(ItemStack tool) {
        return null;
    }

    public boolean isPassable() {
        return false;
    }

    public RayTraceResult rayTrace(Location start, Vector direction, double maxDistance, FluidCollisionMode fluidCollisionMode) {
        return null;
    }

    public BoundingBox getBoundingBox() {
        Location corner2 = location.clone();
        corner2.add(1, 1, 1);
        return BoundingBox.of(location, corner2);
    }

    public BlockSoundGroup getSoundGroup() {
        return null;
    }

    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {

    }

    public List<MetadataValue> getMetadata(String metadataKey) {
        return null;
    }

    public boolean hasMetadata(String metadataKey) {
        return false;
    }

    public void removeMetadata(String metadataKey, Plugin owningPlugin) {

    }
}
