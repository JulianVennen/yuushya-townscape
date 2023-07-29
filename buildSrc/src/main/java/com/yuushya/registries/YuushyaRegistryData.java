package com.yuushya.registries;

import java.util.ArrayList;
import java.util.List;

public class YuushyaRegistryData{
    public List<Block> block = new ArrayList<>();
    public List<Item> item = new ArrayList<>();
    public List<Particle> particle = new ArrayList<>();
    public String version;

    public static class Block implements Cloneable{
        public String classType;
        public String renderType;
        public String name;
        public String itemGroup;
        public Properties properties;
        public Usage usage;
        public ColorTint colorTint;
        public BlockState blockstate;
        public AutoGenerated autoGenerated;
        public Texture texture;
        public String itemModel;

        @Override
        public Block clone() {
            try {
                Block clone = (Block) super.clone();
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        public static class Texture{
            public String value;
            public String type;
            public List<String> set;
            public List<String> slots;
            public List<String> forClass;
            public List<String> forSpecified;
        }
        public static class AutoGenerated{
            public boolean blockstate;
            public boolean lootTable;
            public boolean itemModel;
            public boolean recipe;
            public String collision;
        }
        public static class BlockState{
            public String kit;
            public List<List<String>> forms;
            public List<String> states;
            public List<String> models;
        }
        public static class Properties {
            public int hardness;
            public int resistance;
            public int lightLevel;
            public float ambientocclusionLightLevel;
            public String sound;
            public String material;
            public boolean isSolid;
            public boolean hasCollision;
            public boolean isDelicate;
            public String offset;
            public int lines;
            public String parent;
        }
        public static class ColorTint{
            public String colorType;
            public String colorString;
        }

        public static class Usage{
            public List<Float> sitPos;
            public List<Integer> cycleForms;
        }
    }
    public static class Item{
        public String classType;
        public String name;
        public String itemGroup;
        public Properties properties;
        public Usage usage;
        public static class Properties {
            public int maxCount;
            public int maxDamage;
            public String rarity;
            public String equipment;
            public boolean fireProof;
            public int lines;

        }

        public static class Usage{
            public String createNbt;
            public String cancelNbt;
        }
    }
    public static class Particle{
        public String classType;
        public String name;
        public List<String> textures;
        public Block spawner;
    }

}
