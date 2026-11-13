package com.carrot123.yixiangshouronglu.gene.research;

import com.carrot123.yixiangshouronglu.util.SoulEntityUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PlayerGeneResearch {
    private CompoundTag records = new CompoundTag();

    public boolean available(ResourceLocation id) {
        return record(id).getBoolean("Available");
    }

    public int progress(ResourceLocation id) {
        return Math.max(0, Math.min(100, record(id).getInt("Progress")));
    }

    public int uses(GeneResearchDefinition definition) {
        return Math.max(0, Math.min(definition.limit(), record(definition.id()).getInt("Uses")));
    }

    public Set<String> used(ResourceLocation id) {
        Set<String> result = new LinkedHashSet<>();
        ListTag list = record(id).getList("UsedExtracts", Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            String extract = list.getString(index);
            ResourceLocation key = extract.isEmpty() ? null : ResourceLocation.tryParse(extract);
            if (key != null) {
                result.add(SoulEntityUtil.normalizeExtractEntityId(key.toString()));
            }
        }
        return result;
    }

    public void research(ResourceLocation id, String extract, int increment) {
        Set<String> used = used(id);
        used.add(extract);
        ListTag list = new ListTag();
        used.forEach(value -> list.add(StringTag.valueOf(value)));
        CompoundTag entry = record(id);
        entry.putInt("Progress", Math.min(100, progress(id) + increment));
        entry.put("UsedExtracts", list);
        records.put(id.toString(), entry);
    }

    public void setUses(GeneResearchDefinition definition, int uses) {
        CompoundTag entry = record(definition.id());
        entry.putInt("Uses", Math.max(0, Math.min(uses, definition.limit())));
        records.put(definition.id().toString(), entry);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1);
        tag.put("Definitions", records.copy());
        return tag;
    }

    public CompoundTag snapshot() {
        CompoundTag tag = new CompoundTag();
        CompoundTag known = new CompoundTag();
        for (GeneResearchDefinition definition : GeneResearchRegistry.values()) {
            ResourceLocation id = definition.id();
            CompoundTag entry = new CompoundTag();
            entry.putInt("Progress", progress(id));
            entry.putInt("Uses", uses(definition));
            entry.putBoolean("Available", definition.available());
            ListTag used = new ListTag();
            used(id).stream().limit(100).forEach(value -> used.add(StringTag.valueOf(value)));
            entry.put("UsedExtracts", used);
            known.put(id.toString(), entry);
        }
        tag.putInt("Version", 1);
        tag.put("Definitions", known);
        return tag;
    }

    public void load(CompoundTag tag) {
        CompoundTag incoming = tag.getCompound("Definitions");
        records = new CompoundTag();
        for (String key : incoming.getAllKeys()) {
            if (!key.isEmpty() && ResourceLocation.tryParse(key) != null && incoming.contains(key, Tag.TAG_COMPOUND)) {
                records.put(key, incoming.getCompound(key).copy());
            }
        }
    }

    private CompoundTag record(ResourceLocation id) {
        return records.getCompound(id.toString());
    }
}
