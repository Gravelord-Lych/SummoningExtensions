package lych.summoningex.world;

import lych.summoningex.ClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.Objects;

public final class Area {
    private static final String NAME = "Name";
    private static final String FROM = "From";
    private static final String TO = "To";
    private static final String COLOR = "Color";

    private final String name;
    private final BlockPos from;
    private final BlockPos to;
    private final int color;

    private Area(String name, BlockPos from, BlockPos to, int color) {
        this.name = name;
        this.from = from;
        this.to = to;
        this.color = color;
    }

    private Area(CompoundTag tag) {
        this.name = tag.getString(NAME);
        this.from = NbtUtils.readBlockPos(tag.getCompound(FROM));
        this.to = NbtUtils.readBlockPos(tag.getCompound(TO));
        this.color = tag.getInt(COLOR);
    }

    public static Area create(String name, BlockPos pos1, BlockPos pos2, RandomSource random) {
        return create(name, pos1, pos2, randomColor(random));
    }

    private static int randomColor(RandomSource random) {
//      Randomly selects hue from 0 to 0.95 (step=0.05)
        float hue = 0.05f * random.nextInt(20);
//      Two types of saturation - low(0.5) and high(0.8)
        float saturation = random.nextBoolean() ? 0.5f : 0.8f;
//      Randomly selects hue from 0.5 to 0.1 (step=0.125)
        float brightness = 0.5f + 0.125f * random.nextInt(5);
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static Area create(String name, BlockPos pos1, BlockPos pos2, int color) {
        return create(name, pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ(), color);
    }

    public static Area create(String name, int x0, int y0, int z0, int x1, int y1, int z1, int color) {
        BlockPos from = new BlockPos(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1));
        BlockPos to = new BlockPos(Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1));
        return new Area(name, from, to, color);
    }

    public static Area decode(FriendlyByteBuf buf) {
        return new Area(buf.readUtf(), buf.readBlockPos(), buf.readBlockPos(), buf.readVarInt());
    }

    public static Area load(CompoundTag tag) {
        return new Area(tag);
    }

    public static String format(String name) {
        return "#" + name;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeBlockPos(from);
        buf.writeBlockPos(to);
        buf.writeVarInt(color);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NAME, name);
        tag.put(FROM, NbtUtils.writeBlockPos(from));
        tag.put(TO, NbtUtils.writeBlockPos(to));
        tag.putInt(COLOR, color);
        return tag;
    }

    public MutableComponent getDescription(ServerLevel level) {
        return ComponentUtils.wrapInSquareBrackets(getPosComponent(from).append(" -> ").append(getPosComponent(to))
                .withStyle(style -> style.withColor(calculateTextColor())
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, getCommandText(level)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))));
    }

    private int calculateTextColor() {
        float[] hsb = Color.RGBtoHSB(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, null);
//      Keeps the hue of the summoning area's color but modifies the saturation and the brightness
        hsb[1] = (float) Math.log(hsb[1] + 1) * 0.8f; // s = 0.8ln(s + 1)
        hsb[2] *= 0.92f; // b = 0.92b
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }

    private String getCommandText(ServerLevel level) {
        return "/execute in " + level.dimension().location() + " run tp @s " + getMidX() + " " + getMidY() + " " + getMidZ();
    }

    private static MutableComponent getPosComponent(BlockPos pos) {
        return Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ());
    }

    public MutableComponent getFormattedName() {
        return Component.literal(getName()).withStyle(style -> style.withColor(color));
    }

    public String getName() {
        return name;
    }

    public BlockPos from() {
        return from;
    }

    public BlockPos to() {
        return to;
    }

    public Vec3 getMid() {
        return new Vec3(getMidX(), getMidY(), getMidZ());
    }

    public double getMidX() {
        return (from.getX() + to.getX() + 1) / 2.0;
    }

    public double getMidY() {
        return (from.getY() + to.getY() + 1) / 2.0;
    }

    public double getMidZ() {
        return (from.getZ() + to.getZ() + 1) / 2.0;
    }

    public double getRadius() {
        int dx = to.getX() + 1 - from.getX();
        int dy = to.getY() + 1 - from.getY();
        int dz = to.getZ() + 1 - from.getZ();
//      Average edge length / 2.0
        return (dx + dy + dz) / 6.0;
    }

    public Vec3 getRandomBottomPos(RandomSource random) {
        return getRandomPosWithY(from.getY(), random);
    }

    public Vec3 getRandomPosWithY(double y, RandomSource random) {
        AABB aabb = asAABB();
        double x = aabb.minX + (aabb.maxX - aabb.minX) * random.nextDouble();
        double z = aabb.minZ + (aabb.maxZ - aabb.minZ) * random.nextDouble();
        return new Vec3(x, y, z);
    }

    public boolean shouldRender(Vec3 pos) {
        double maxDistance = ClientConfig.AREA_RENDER_DISTANCE.get() + getRadius();
        return pos.distanceToSqr(getMid()) <= maxDistance * maxDistance;
    }

    public int getColor() {
        return color;
    }

    public AABB asAABB() {
        return new AABB(from.getX(), from.getY(), from.getZ(), to.getX() + 1, to.getY() + 1, to.getZ() + 1);
    }

    @Override
    public String toString() {
        return "#%s [(%s) -> (%s)]".formatted(name, from.toShortString(), to.toShortString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Area area = (Area) o;
        return Objects.equals(name, area.name) && Objects.equals(from, area.from) && Objects.equals(to, area.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, from, to);
    }
}
