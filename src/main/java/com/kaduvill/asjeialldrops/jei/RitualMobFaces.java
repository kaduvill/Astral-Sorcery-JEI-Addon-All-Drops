package com.kaduvill.asjeialldrops.jei;

import java.util.HashMap;
import java.util.Map;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** Cached vanilla texture crops; mob eggs remain hidden navigation identities. */
final class RitualMobFaces {
    private final Map<String, Face> faces = new HashMap<>();

    RitualMobFaces(IGuiHelper helper) {
        add(helper, "cow", "cow/cow", 64, 32, 6, 6, 8, 8);
        add(helper, "pig", "pig/pig", 64, 32, 8, 8, 8, 8);
        faces.get("pig").detail = crop(helper, "pig/pig", 64, 32, 17, 17, 4, 3);
        faces.get("pig").detailX = 2;
        faces.get("pig").detailY = 4;
        add(helper, "chicken", "chicken", 64, 32, 3, 3, 4, 6);
        faces.get("chicken").detail = crop(helper, "chicken", 64, 32, 16, 2, 4, 2);
        faces.get("chicken").detailY = 2;
        add(helper, "sheep", "sheep/sheep", 64, 32, 8, 8, 6, 6);
        rabbit(helper);
        add(helper, "squid", "squid", 64, 32, 12, 12, 12, 16);
        add(helper, "zombie", "zombie/zombie", 64, 64, 8, 8, 8, 8);
        add(helper, "zombie_pigman", "zombie_pigman", 64, 64, 8, 8, 8, 8);
        add(helper, "skeleton", "skeleton/skeleton", 64, 32, 8, 8, 8, 8);
        add(helper, "wither_skeleton", "skeleton/wither_skeleton", 64, 32, 8, 8, 8, 8);
        add(helper, "stray", "skeleton/stray", 64, 32, 8, 8, 8, 8);
        add(helper, "spider", "spider/spider", 64, 32, 40, 12, 8, 8);
        add(helper, "creeper", "creeper/creeper", 64, 32, 8, 8, 8, 8);
        add(helper, "blaze", "blaze", 64, 32, 8, 8, 8, 8);
        add(helper, "ghast", "ghast/ghast", 64, 32, 16, 16, 16, 16);
        parrot(helper);
        horse(helper, "horse", "horse/horse_brown", true);
        horse(helper, "skeleton_horse", "horse/horse_skeleton", false);
        villager(helper, "villager", "villager/villager", 64);
        villager(helper, "witch", "witch", 128);
    }

    private void parrot(IGuiHelper helper) {
        String path = "parrot/parrot_red_blue";
        IDrawable tail = crop(helper, path, 32, 32, 23, 2, 3, 4);
        IDrawable foot = crop(helper, path, 32, 32, 17, 19, 1, 2);
        IDrawable body = crop(helper, path, 32, 32, 5, 11, 3, 6);
        IDrawable wing = crop(helper, path, 32, 32, 19, 11, 3, 5);
        IDrawable crest = crop(helper, path, 32, 32, 2, 22, 4, 5);
        IDrawable headCap = crop(helper, path, 32, 32, 10, 4, 4, 1);
        IDrawable head = crop(helper, path, 32, 32, 2, 4, 2, 3);
        IDrawable beak = crop(helper, path, 32, 32, 12, 8, 1, 2);
        // Draw the full body directly from Minecraft's texture; no combined image or dynamic texture.
        faces.put("parrot", new Face(new IDrawable() {
            @Override public int getWidth() { return 8; }
            @Override public int getHeight() { return 16; }
            @Override public void draw(Minecraft minecraft, int x, int y) {
                tail.draw(minecraft, x + 1, y + 12);
                foot.draw(minecraft, x + 4, y + 13);
                foot.draw(minecraft, x + 6, y + 13);
                body.draw(minecraft, x + 4, y + 7);
                wing.draw(minecraft, x + 2, y + 8);
                crest.draw(minecraft, x + 3, y);
                headCap.draw(minecraft, x + 3, y + 3);
                head.draw(minecraft, x + 5, y + 4);
                beak.draw(minecraft, x + 7, y + 5);
            }
        }, 8, 16));
    }

    private void rabbit(IGuiHelper helper) {
        // The vanilla salt variant has contrasting eyes in its front panel.
        IDrawable head = crop(helper, "rabbit/salt", 64, 32, 37, 5, 5, 4);
        IDrawable leftEar = crop(helper, "rabbit/salt", 64, 32, 53, 1, 2, 5);
        IDrawable rightEar = crop(helper, "rabbit/salt", 64, 32, 59, 1, 2, 5);
        IDrawable nose = crop(helper, "rabbit/salt", 64, 32, 33, 10, 1, 1);
        // Cache the vanilla texture pieces once, keeping the ears inside the face's 16px bounds.
        faces.put("rabbit", new Face(new IDrawable() {
            @Override public int getWidth() { return 5; }
            @Override public int getHeight() { return 9; }
            @Override public void draw(Minecraft minecraft, int x, int y) {
                leftEar.draw(minecraft, x, y);
                rightEar.draw(minecraft, x + 3, y);
                head.draw(minecraft, x, y + 5);
                nose.draw(minecraft, x + 2, y + 6);
            }
        }, 5, 9));
    }

    private void horse(IGuiHelper helper, String id, String path, boolean hasEars) {
        // ModelHorse's side panels retain the eye, muzzle and ear in a compact profile.
        IDrawable head = crop(helper, path, 128, 128, 0, 7, 7, 5);
        IDrawable upperMouth = crop(helper, path, 128, 128, 24, 24, 6, 3);
        IDrawable lowerMouth = crop(helper, path, 128, 128, 24, 32, 5, 2);
        // The skeleton horse's vanilla ear panel is transparent.
        IDrawable ear = hasEars ? crop(helper, path, 128, 128, 1, 1, 2, 3) : null;
        int headY = hasEars ? 3 : 0;
        faces.put(id, new Face(new IDrawable() {
            @Override public int getWidth() { return 13; }
            @Override public int getHeight() { return headY + 5; }
            @Override public void draw(Minecraft minecraft, int x, int y) {
                head.draw(minecraft, x + 6, y + headY);
                upperMouth.draw(minecraft, x, y + headY);
                lowerMouth.draw(minecraft, x + 1, y + headY + 3);
                if (ear != null) ear.draw(minecraft, x + 11, y);
            }
        }, 13, headY + 5));
    }

    private void villager(IGuiHelper helper, String id, String path, int height) {
        add(helper, id, path, 64, height, 8, 8, 8, 10);
        Face face = faces.get(id);
        face.detail = crop(helper, path, 64, height, 26, 2, 2, 4);
        face.detailX = 3;
        face.detailY = 7;
        face.height = 11;
    }

    private void add(IGuiHelper helper, String id, String path, int textureWidth, int textureHeight,
                     int u, int v, int width, int height) {
        faces.put(id, new Face(crop(helper, path, textureWidth, textureHeight, u, v, width, height), width, height));
    }

    private static IDrawable crop(IGuiHelper helper, String path, int textureWidth, int textureHeight,
                                  int u, int v, int width, int height) {
        return helper.drawableBuilder(new ResourceLocation("minecraft", "textures/entity/" + path + ".png"),
                u, v, width, height).setTextureSize(textureWidth, textureHeight).build();
    }

    boolean draw(Minecraft minecraft, ResourceLocation id, int x, int y) {
        Face face = "minecraft".equals(id.getNamespace()) ? faces.get(id.getPath()) : null;
        if (face == null) return false;
        float scale = 16F / Math.max(face.width, face.height);
        GlStateManager.disableLighting();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x + (16 - face.width * scale) / 2, y + (16 - face.height * scale) / 2, 0);
            GlStateManager.scale(scale, scale, 1);
            face.base.draw(minecraft);
            if (face.detail != null) face.detail.draw(minecraft, face.detailX, face.detailY);
        } finally {
            GlStateManager.popMatrix();
            GlStateManager.color(1, 1, 1, 1);
        }
        return true;
    }

    private static final class Face {
        final IDrawable base;
        final int width;
        int height;
        IDrawable detail;
        int detailX;
        int detailY;
        Face(IDrawable base, int width, int height) { this.base = base; this.width = width; this.height = height; }
    }
}
