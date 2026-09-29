package com.pvpkits;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KitStorage {

    private static final String SAVE_FOLDER_NAME = "pvpkits";
    private static final String KITS_KEY = "kits";
    private static final String NAME_KEY = "name";
    private static final String ITEMS_KEY = "items";
    private static final String SLOT_INDEX_KEY = "slot";
    private static final String ITEM_KEY = "item";

    public static boolean saveKit(ServerPlayer player, String name) {
        RegistryOps<Tag> ops = createOps(player);
        CompoundTag root = readRoot(player.getUUID());
        ListTag kits = root.getList(KITS_KEY).orElseGet(ListTag::new);

        ListTag filtered = new ListTag();
        for (int i = 0; i < kits.size(); i++) {
            CompoundTag kit = kits.getCompound(i).orElse(new CompoundTag());
            if (!kit.getString(NAME_KEY).orElse("").equals(name)) {
                filtered.add(kit);
            }
        }

        CompoundTag newKit = new CompoundTag();
        newKit.putString(NAME_KEY, name);
        newKit.put(ITEMS_KEY, serializeInventory(player.getInventory(), ops));
        filtered.add(newKit);

        root.put(KITS_KEY, filtered);
        writeRoot(player.getUUID(), root);
        return true;
    }

    public static boolean loadKit(ServerPlayer player, String name) {
        RegistryOps<Tag> ops = createOps(player);
        CompoundTag root = readRoot(player.getUUID());
        ListTag kits = root.getList(KITS_KEY).orElseGet(ListTag::new);

        for (int i = 0; i < kits.size(); i++) {
            CompoundTag kit = kits.getCompound(i).orElse(new CompoundTag());
            if (kit.getString(NAME_KEY).orElse("").equals(name)) {
                deserializeInventory(kit.getList(ITEMS_KEY).orElseGet(ListTag::new), player.getInventory(), ops);
                player.inventoryMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    public static boolean deleteKit(ServerPlayer player, String name) {
        CompoundTag root = readRoot(player.getUUID());
        ListTag kits = root.getList(KITS_KEY).orElseGet(ListTag::new);

        ListTag filtered = new ListTag();
        boolean removed = false;
        for (int i = 0; i < kits.size(); i++) {
            CompoundTag kit = kits.getCompound(i).orElse(new CompoundTag());
            if (kit.getString(NAME_KEY).orElse("").equals(name)) {
                removed = true;
            } else {
                filtered.add(kit);
            }
        }

        if (removed) {
            root.put(KITS_KEY, filtered);
            writeRoot(player.getUUID(), root);
        }
        return removed;
    }

    public static List<String> listKits(ServerPlayer player) {
        List<String> names = new ArrayList<>();
        CompoundTag root = readRoot(player.getUUID());
        ListTag kits = root.getList(KITS_KEY).orElseGet(ListTag::new);
        for (int i = 0; i < kits.size(); i++) {
            CompoundTag kit = kits.getCompound(i).orElse(new CompoundTag());
            names.add(kit.getString(NAME_KEY).orElse(""));
        }
        return names;
    }

    private static RegistryOps<Tag> createOps(ServerPlayer player) {
        HolderLookup.Provider registries = player.registryAccess();
        return RegistryOps.create(NbtOps.INSTANCE, registries);
    }

    private static Tag encodeStack(ItemStack stack, RegistryOps<Tag> ops) {
        return ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
    }

    private static ItemStack decodeStack(Tag tag, RegistryOps<Tag> ops) {
        return ItemStack.CODEC.parse(ops, tag).getOrThrow();
    }

    private static ListTag serializeInventory(Inventory inventory, RegistryOps<Tag> ops) {
        ListTag list = new ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            CompoundTag entry = new CompoundTag();
            entry.putInt(SLOT_INDEX_KEY, i);
            entry.put(ITEM_KEY, encodeStack(stack, ops));
            list.add(entry);
        }
        return list;
    }

    private static void deserializeInventory(ListTag list, Inventory inventory, RegistryOps<Tag> ops) {
        inventory.clearContent();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i).orElse(new CompoundTag());
            int slot = entry.getInt(SLOT_INDEX_KEY).orElse(0);
            Tag itemTag = entry.get(ITEM_KEY);
            if (itemTag != null) {
                ItemStack stack = decodeStack(itemTag, ops);
                if (slot >= 0 && slot < inventory.getContainerSize()) {
                    inventory.setItem(slot, stack);
                }
            }
        }
    }

    private static Path getFile(UUID playerId) {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve(SAVE_FOLDER_NAME);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dir.resolve(playerId + ".dat");
    }

    private static CompoundTag readRoot(UUID playerId) {
        Path file = getFile(playerId);
        if (!Files.exists(file)) return new CompoundTag();

        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            e.printStackTrace();
            return new CompoundTag();
        }
    }

    private static void writeRoot(UUID playerId, CompoundTag root) {
        Path file = getFile(playerId);
        try {
            NbtIo.writeCompressed(root, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}