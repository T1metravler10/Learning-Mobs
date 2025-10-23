package com.t1metravler10.LearningMobs.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LearningMobsWorldData extends SavedData {
    public static final String DATA_NAME = "learning_mobs";
    private static final Codec<LearningMobsWorldData> CODEC = CompoundTag.CODEC.xmap(
            LearningMobsWorldData::readNbt,
            LearningMobsWorldData::writeNbt
    );
    public static final SavedDataType<LearningMobsWorldData> TYPE = new SavedDataType<>(
            DATA_NAME,
            ctx -> new LearningMobsWorldData(),
            c -> CODEC,
            (DataFixTypes) null
    );

    private final Map<ResourceLocation, MobLearningRecord> mobRecords = new HashMap<>();
    private long lastProcessedDay = -1L;

    public static LearningMobsWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private LearningMobsWorldData() {
    }

    private static LearningMobsWorldData readNbt(CompoundTag tag) {
        LearningMobsWorldData data = new LearningMobsWorldData();
        data.lastProcessedDay = tag.getLong("lastProcessedDay").orElse(-1L);

        ListTag mobsTag = tag.getList("mobs").orElseGet(ListTag::new);
        for (int i = 0; i < mobsTag.size(); i++) {
            net.minecraft.nbt.Tag entry = mobsTag.get(i);
            if (!(entry instanceof CompoundTag mobTag)) {
                continue;
            }
            String mobIdString = mobTag.getString("id").orElse("");
            if (mobIdString.isEmpty()) {
                continue;
            }
            ResourceLocation mobId = ResourceLocation.tryParse(mobIdString);
            if (mobId != null) {
                MobLearningRecord record = MobLearningRecord.fromNbt(mobTag);
                data.mobRecords.put(mobId, record);
            }
        }
        return data;
    }

    private CompoundTag writeNbt() {
        CompoundTag tag = new CompoundTag();
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
