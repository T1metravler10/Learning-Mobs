package com.t1metravler10.LearningMobs.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LearningMobsWorldData extends SavedData {
    public static final String DATA_NAME = "learning_mobs";

    private final Map<ResourceLocation, MobLearningRecord> mobRecords = new HashMap<>();
    private long lastProcessedDay = -1L;

    public static LearningMobsWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(LearningMobsWorldData::load, LearningMobsWorldData::new, DATA_NAME);
    }

    private LearningMobsWorldData() {
    }

    private static LearningMobsWorldData load(CompoundTag tag) {
        LearningMobsWorldData data = new LearningMobsWorldData();
        data.lastProcessedDay = tag.getLong("lastProcessedDay");

        ListTag mobsTag = tag.getList("mobs", Tag.TAG_COMPOUND);
        for (Tag entry : mobsTag) {
            CompoundTag mobTag = (CompoundTag) entry;
            ResourceLocation mobId = ResourceLocation.tryParse(mobTag.getString("id"));
            if (mobId != null) {
                MobLearningRecord record = MobLearningRecord.fromNbt(mobTag);
                data.mobRecords.put(mobId, record);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("lastProcessedDay", lastProcessedDay);
        ListTag mobsTag = new ListTag();
        for (Map.Entry<ResourceLocation, MobLearningRecord> entry : mobRecords.entrySet()) {
            CompoundTag mobTag = entry.getValue().toNbt();
            mobTag.putString("id", entry.getKey().toString());
            mobsTag.add(mobTag);
        }
        tag.put("mobs", mobsTag);
        return tag;
    }

    public MobLearningRecord getRecord(ResourceLocation mobId) {
        return mobRecords.computeIfAbsent(mobId, key -> {
            setDirty();
            return new MobLearningRecord();
        });
    }

    public void setRecord(ResourceLocation mobId, MobLearningRecord record) {
        mobRecords.put(Objects.requireNonNull(mobId), Objects.requireNonNull(record));
        setDirty();
    }

    public long getLastProcessedDay() {
        return lastProcessedDay;
    }

    public void setLastProcessedDay(long day) {
        if (day != lastProcessedDay) {
            lastProcessedDay = day;
            setDirty();
        }
    }
}
