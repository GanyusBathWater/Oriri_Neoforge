package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.dungeon.stage.DungeonStageManager;
import net.ganyusbathwater.oririmod.network.packet.OpenMarkerScreenPayload;
import net.ganyusbathwater.oririmod.network.packet.SyncMarkerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static net.ganyusbathwater.oririmod.client.screen.DungeonGuiStyle.*;

/**
 * Dungeon Architect Console for configuring Dungeon Stage Markers.
 * Preserves all role/stage visibility rules, right-click reverse cycling, and TAB autocomplete.
 */
@OnlyIn(Dist.CLIENT)
public class DungeonMarkerScreen extends Screen {

    private static final int PANEL_W = 376;

    private final int entityId;

    private EditBox stageIdBox;
    private CustomCycleButton stageTypeButton;
    private CustomCycleButton roleButton;
    private EditBox enemyTypeBox;
    private EditBox countBox;
    private EditBox switchIdBox;
    private EditBox lootTableBox;
    private EditBox bossIdBox;
    private EditBox chanceBox;
    private CustomCycleButton dropsKeyButton;
    private CustomCycleButton modifierActionButton;
    private CustomCycleButton triggerBehaviorButton;
    private EditBox objectiveBox;

    private final String initialStageId;
    private final String initialStageType;
    private final String initialRole;
    private final String initialEnemyType;
    private final int initialCount;
    private final String initialSwitchId;
    private final String initialLootTable;
    private final String initialBossId;
    private final float initialChance;
    private final String initialTriggerBehavior;
    private final String initialObjective;
    private final BlockPos pos;
    private final String stageSummary;

    private String currentStageType;
    private String currentRole;
    private boolean isUpdatingUI = false;

    private int panelLeft;
    private int panelTop;
    private int panelH;

    private static final List<String> STAGE_TYPES = Arrays.asList(
            "KILL_ALL_ENEMIES", "ACTIVATE_SWITCHES", "SURVIVE_TIMER", "BOSS_FIGHT",
            "MINI_BOSS_FIGHT", "PUZZLE_SOLVE", "REACH_GOAL", "SPAWN_ONLY"
    );

    private static final List<String> ROLES = Arrays.asList(
            DungeonStageManager.ROLE_SPAWN_POINT,
            DungeonStageManager.ROLE_INFINITE_SPAWNER,
            DungeonStageManager.ROLE_SWITCH,
            DungeonStageManager.ROLE_DOOR,
            DungeonStageManager.ROLE_STAGE_TRIGGER,
            DungeonStageManager.ROLE_GOAL_AREA,
            DungeonStageManager.ROLE_BLOCK_MATCH,
            DungeonStageManager.ROLE_AREA_MODIFIER,
            DungeonStageManager.ROLE_BOSS_SPAWN,
            DungeonStageManager.ROLE_MINI_BOSS_SPAWN,
            DungeonStageManager.ROLE_LOOT_CHEST,
            DungeonStageManager.ROLE_SECRET_CHEST_SPAWN,
            DungeonStageManager.ROLE_SECRET_BUTTON_SPAWN,
            DungeonStageManager.ROLE_PLAYER_SPAWN,
            DungeonStageManager.ROLE_DEATH_AREA
    );

    public DungeonMarkerScreen(OpenMarkerScreenPayload payload) {
        super(Component.literal("Configure Dungeon Marker"));
        this.entityId = payload.entityId();
        this.initialStageId = payload.stageId();
        this.initialStageType = payload.stageType().isBlank() ? "KILL_ALL_ENEMIES" : payload.stageType();
        this.initialRole = payload.role().isBlank() ? DungeonStageManager.ROLE_SPAWN_POINT : payload.role();
        this.initialEnemyType = payload.enemyType();
        this.initialCount = payload.count();
        this.initialSwitchId = payload.switchId();
        this.initialLootTable = payload.lootTable();
        this.initialBossId = payload.bossId();
        this.initialChance = payload.spawnChance();
        this.initialTriggerBehavior = payload.triggerBehavior().isBlank() ? "NO_TELEPORT" : payload.triggerBehavior();
        this.initialObjective = payload.objectiveText();
        this.pos = payload.pos();
        this.stageSummary = payload.stageSummary();

        this.currentStageType = this.initialStageType;
        this.currentRole = this.initialRole;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();

        this.panelH = Math.min(314, this.height - 16);
        this.panelLeft = (this.width - PANEL_W) / 2;
        this.panelTop = (this.height - this.panelH) / 2;

        int midX = this.width / 2;
        int col1X = midX - 166;
        int col2X = midX + 6;
        int colW = 160;
        int headerFieldsY = this.panelTop + 62;

        // Top fixed header fields
        this.stageIdBox = createStyledBox(col1X, headerFieldsY, colW, 18, "Stage ID", 64, initialStageId);
        this.stageIdBox.setTooltip(Tooltip.create(Component.literal("The ID grouping all markers in this stage.")));
        this.addRenderableWidget(this.stageIdBox);

        this.stageTypeButton = new CustomCycleButton(
                col1X, headerFieldsY + 32, colW, 18,
                "Type", STAGE_TYPES, currentStageType,
                val -> {
                    this.currentStageType = val;
                    this.updateUIState();
                }
        );
        this.stageTypeButton.setTooltip(Tooltip.create(Component.literal("The overall goal/type for this stage. Left/Right click to cycle.")));
        this.addRenderableWidget(this.stageTypeButton);

        this.roleButton = new CustomCycleButton(
                col2X, headerFieldsY + 32, colW, 18,
                "Role", ROLES, currentRole,
                val -> {
                    this.currentRole = val;
                    this.updateUIState();
                }
        );
        this.roleButton.setTooltip(Tooltip.create(Component.literal("What THIS specific marker does. Left/Right click to cycle.")));
        this.addRenderableWidget(this.roleButton);

        // Dynamic fields (repositioned in updateUIState)
        this.enemyTypeBox = createStyledBox(col1X, headerFieldsY + 64, colW, 18, "Enemy Type", 128, initialEnemyType);
        this.addRenderableWidget(this.enemyTypeBox);

        this.chanceBox = createStyledBox(col1X, headerFieldsY + 96, colW, 18, "Spawn Chance", 10, String.valueOf(initialChance));
        this.addRenderableWidget(this.chanceBox);

        this.countBox = createStyledBox(col2X, headerFieldsY, colW, 18, "Count", 10, String.valueOf(initialCount));
        this.addRenderableWidget(this.countBox);

        this.switchIdBox = createStyledBox(col2X, headerFieldsY + 64, colW, 18, "Switch ID", 64, initialSwitchId);
        this.addRenderableWidget(this.switchIdBox);

        this.dropsKeyButton = new CustomCycleButton(
                col1X, headerFieldsY + 128, colW, 18,
                "Drops Key", Arrays.asList("Yes", "No"), initialLootTable.isEmpty() ? "No" : "Yes",
                val -> {
                    if (this.lootTableBox != null) {
                        this.lootTableBox.setValue("Yes".equals(val) ? "oririmod:mana_destabilizer" : "");
                    }
                }
        );
        this.addRenderableWidget(this.dropsKeyButton);

        String initAction = initialSwitchId.isEmpty() ? "fill"
                : ("destroy".equals(initialSwitchId) ? "destroy"
                : ("place_teleporter".equals(initialSwitchId) ? "place_teleporter" : "fill"));
        this.modifierActionButton = new CustomCycleButton(
                col2X, headerFieldsY + 64, colW, 18,
                "Action", Arrays.asList("fill", "destroy", "place_teleporter"), initAction,
                val -> {
                    if (this.switchIdBox != null) {
                        this.switchIdBox.setValue(val);
                    }
                }
        );
        this.addRenderableWidget(this.modifierActionButton);

        this.triggerBehaviorButton = new CustomCycleButton(
                col2X, headerFieldsY + 64, colW, 18,
                "Behavior", Arrays.asList("NO_TELEPORT", "TELEPORT_PARTY", "REQUIRE_PARTY"),
                initialTriggerBehavior,
                val -> {}
        );
        this.addRenderableWidget(this.triggerBehaviorButton);

        this.lootTableBox = createStyledBox(col2X, headerFieldsY + 96, colW, 18, "Loot Table", 128, initialLootTable);
        this.addRenderableWidget(this.lootTableBox);

        this.bossIdBox = createStyledBox(col2X, headerFieldsY + 128, colW, 18, "Boss ID", 64, initialBossId);
        this.addRenderableWidget(this.bossIdBox);

        this.objectiveBox = createStyledBox(col1X, headerFieldsY + 160, 332, 18, "Objective Text", 1024,
                initialObjective != null ? initialObjective : "");
        this.addRenderableWidget(this.objectiveBox);

        // Footer buttons
        int footerY = this.panelTop + this.panelH - 28;
        this.addRenderableWidget(new SanctuaryButton(
                this.panelLeft + 14,
                footerY,
                104,
                20,
                Component.translatable("gui.oririmod.teleporter.cancel"),
                ButtonTone.NEUTRAL,
                this::onClose
        ));

        this.addRenderableWidget(new SanctuaryButton(
                this.panelLeft + PANEL_W - 224,
                footerY,
                210,
                20,
                Component.literal("Save Marker"),
                ButtonTone.EMERALD,
                this::saveAndClose
        ));

        updateUIState();
    }

    private EditBox createStyledBox(int x, int y, int w, int h, String label, int maxLen, String value) {
        EditBox box = new EditBox(this.font, x + 4, y + 5, w - 8, h - 6, Component.literal(label));
        box.setBordered(false);
        box.setMaxLength(maxLen);
        box.setTextColor(TEXT_PRIMARY);
        box.setTextColorUneditable(TEXT_MUTED);
        box.setValue(value != null ? value : "");
        return box;
    }

    private void updateUIState() {
        if (enemyTypeBox == null || isUpdatingUI) return;
        isUpdatingUI = true;

        enemyTypeBox.visible = false;
        chanceBox.visible = false;
        countBox.visible = false;
        switchIdBox.visible = false;
        dropsKeyButton.visible = false;
        modifierActionButton.visible = false;
        triggerBehaviorButton.visible = false;
        lootTableBox.visible = false;
        bossIdBox.visible = false;
        objectiveBox.visible = false;

        boolean isGlobal = DungeonStageManager.ROLE_LOOT_CHEST.equals(currentRole)
                || DungeonStageManager.ROLE_SECRET_CHEST_SPAWN.equals(currentRole)
                || DungeonStageManager.ROLE_SECRET_BUTTON_SPAWN.equals(currentRole);
        this.stageIdBox.visible = !isGlobal;
        if (this.stageTypeButton != null) {
            this.stageTypeButton.visible = !isGlobal;
        }

        List<String> validRoles = new ArrayList<>();
        validRoles.add(DungeonStageManager.ROLE_DOOR);
        validRoles.add(DungeonStageManager.ROLE_STAGE_TRIGGER);
        validRoles.add(DungeonStageManager.ROLE_AREA_MODIFIER);
        validRoles.add(DungeonStageManager.ROLE_LOOT_CHEST);
        validRoles.add(DungeonStageManager.ROLE_SECRET_CHEST_SPAWN);
        validRoles.add(DungeonStageManager.ROLE_SECRET_BUTTON_SPAWN);
        validRoles.add(DungeonStageManager.ROLE_PLAYER_SPAWN);
        validRoles.add(DungeonStageManager.ROLE_DEATH_AREA);

        switch (currentStageType) {
            case "KILL_ALL_ENEMIES", "SURVIVE_TIMER" -> {
                validRoles.add(0, DungeonStageManager.ROLE_SPAWN_POINT);
                validRoles.add(1, DungeonStageManager.ROLE_INFINITE_SPAWNER);
            }
            case "ACTIVATE_SWITCHES" -> {
                validRoles.add(0, DungeonStageManager.ROLE_SWITCH);
                validRoles.add(1, DungeonStageManager.ROLE_SPAWN_POINT);
                validRoles.add(2, DungeonStageManager.ROLE_INFINITE_SPAWNER);
            }
            case "BOSS_FIGHT" -> {
                validRoles.add(0, DungeonStageManager.ROLE_BOSS_SPAWN);
                validRoles.add(1, DungeonStageManager.ROLE_INFINITE_SPAWNER);
            }
            case "MINI_BOSS_FIGHT" -> {
                validRoles.add(0, DungeonStageManager.ROLE_MINI_BOSS_SPAWN);
                validRoles.add(1, DungeonStageManager.ROLE_INFINITE_SPAWNER);
            }
            case "PUZZLE_SOLVE" -> validRoles.add(0, DungeonStageManager.ROLE_BLOCK_MATCH);
            case "REACH_GOAL" -> validRoles.add(0, DungeonStageManager.ROLE_GOAL_AREA);
            case "SPAWN_ONLY" -> validRoles.add(0, DungeonStageManager.ROLE_SPAWN_POINT);
        }

        if (roleButton != null) {
            roleButton.setValues(validRoles);
            if (!validRoles.contains(currentRole)) {
                currentRole = validRoles.get(0);
            }
        }

        if ((DungeonStageManager.ROLE_SWITCH.equals(currentRole)
                || DungeonStageManager.ROLE_DOOR.equals(currentRole)
                || DungeonStageManager.ROLE_STAGE_TRIGGER.equals(currentRole)) && switchIdBox != null) {
            String val = switchIdBox.getValue();
            if ("destroy".equals(val) || "fill".equals(val) || "place_teleporter".equals(val)) {
                switchIdBox.setValue("");
            }
        }

        boolean isModifier = DungeonStageManager.ROLE_AREA_MODIFIER.equals(currentRole);
        boolean isDoor = DungeonStageManager.ROLE_DOOR.equals(currentRole);
        boolean isTrigger = DungeonStageManager.ROLE_STAGE_TRIGGER.equals(currentRole);
        boolean isBlockMatch = DungeonStageManager.ROLE_BLOCK_MATCH.equals(currentRole);
        boolean isBoss = DungeonStageManager.ROLE_BOSS_SPAWN.equals(currentRole)
                || DungeonStageManager.ROLE_MINI_BOSS_SPAWN.equals(currentRole);
        boolean isChest = DungeonStageManager.ROLE_LOOT_CHEST.equals(currentRole);
        boolean isSwitch = DungeonStageManager.ROLE_SWITCH.equals(currentRole);
        boolean isSpawn = DungeonStageManager.ROLE_SPAWN_POINT.equals(currentRole);
        boolean isSpawner = DungeonStageManager.ROLE_INFINITE_SPAWNER.equals(currentRole);
        boolean isGoalArea = DungeonStageManager.ROLE_GOAL_AREA.equals(currentRole);
        boolean isDeathArea = DungeonStageManager.ROLE_DEATH_AREA.equals(currentRole);
        boolean isSecretChest = DungeonStageManager.ROLE_SECRET_CHEST_SPAWN.equals(currentRole);
        boolean isSecretButton = DungeonStageManager.ROLE_SECRET_BUTTON_SPAWN.equals(currentRole);

        if (isSpawn || isSpawner) {
            enemyTypeBox.visible = true;
            countBox.visible = true;
            chanceBox.visible = true;
            dropsKeyButton.visible = true;
        } else if (isModifier) {
            enemyTypeBox.visible = true;
            countBox.visible = true;
            modifierActionButton.setValues(Arrays.asList("fill", "destroy", "place_teleporter"));
            modifierActionButton.visible = true;
        } else if (isTrigger) {
            countBox.visible = true;
            switchIdBox.visible = true;
            triggerBehaviorButton.setValues(Arrays.asList("NO_TELEPORT", "TELEPORT_PARTY", "REQUIRE_PARTY"));
            triggerBehaviorButton.visible = true;
        } else if (isGoalArea) {
            countBox.visible = true;
        } else if (isBlockMatch) {
            enemyTypeBox.visible = true;
            countBox.visible = true;
        } else if (isSwitch) {
            switchIdBox.visible = true;
        } else if (isBoss) {
            bossIdBox.visible = true;
            dropsKeyButton.visible = true;
        } else if (isChest || isSecretChest) {
            lootTableBox.visible = true;
        } else if (isDeathArea) {
            enemyTypeBox.visible = true;
            if (enemyTypeBox.getValue().isBlank()) enemyTypeBox.setValue("10,10,10");
            modifierActionButton.setValues(Arrays.asList("BOX", "SPHERE", "CYLINDER"));
            modifierActionButton.visible = true;
            triggerBehaviorButton.setValues(Arrays.asList("STAGE", "GLOBAL"));
            triggerBehaviorButton.visible = true;
        } else if (isSecretButton) {
            enemyTypeBox.visible = true;
            lootTableBox.visible = true;
        }

        if ("SURVIVE_TIMER".equals(currentStageType) && !isModifier && !isDoor && !isTrigger) {
            countBox.visible = true;
        }

        if (!isGlobal) {
            objectiveBox.visible = true;
        }

        // Dynamic compact layout inside panel
        int midX = this.width / 2;
        int col1X = midX - 166;
        int col2X = midX + 6;
        int startY = this.panelTop + 130;
        int rowSpacing = 32;

        AbstractWidget[] col1 = { enemyTypeBox, chanceBox, bossIdBox, dropsKeyButton };
        AbstractWidget[] col2 = { countBox, switchIdBox, triggerBehaviorButton, modifierActionButton, lootTableBox };

        int row1 = 0;
        for (AbstractWidget w : col1) {
            if (w.visible) {
                int y = startY + rowSpacing * row1;
                w.setPosition(w instanceof EditBox ? col1X + 4 : col1X, w instanceof EditBox ? y + 5 : y);
                row1++;
            }
        }

        int row2 = 0;
        for (AbstractWidget w : col2) {
            if (w.visible) {
                int y = startY + rowSpacing * row2;
                w.setPosition(w instanceof EditBox ? col2X + 4 : col2X, w instanceof EditBox ? y + 5 : y);
                row2++;
            }
        }

        if (objectiveBox.visible) {
            int objY = Math.min(this.panelTop + this.panelH - 58, startY + rowSpacing * Math.max(row1, row2));
            objectiveBox.setPosition(col1X + 4, objY + 5);
        }

        isUpdatingUI = false;
    }

    private void saveAndClose() {
        int count = 0;
        try { count = Integer.parseInt(countBox.getValue()); } catch (NumberFormatException ignored) {}

        float chance = 1.0F;
        try { chance = Float.parseFloat(chanceBox.getValue()); } catch (NumberFormatException ignored) {}

        String switchIdOrAction = switchIdBox.getValue();
        if (DungeonStageManager.ROLE_AREA_MODIFIER.equals(currentRole)
                || DungeonStageManager.ROLE_DEATH_AREA.equals(currentRole)) {
            switchIdOrAction = modifierActionButton.values.get(modifierActionButton.currentIndex);
        }

        PacketDistributor.sendToServer(new SyncMarkerDataPayload(
                entityId,
                stageIdBox.getValue(),
                currentStageType,
                currentRole,
                enemyTypeBox.getValue(),
                count,
                switchIdOrAction,
                lootTableBox.getValue(),
                bossIdBox.getValue(),
                chance,
                triggerBehaviorButton.values.get(triggerBehaviorButton.currentIndex),
                objectiveBox.getValue()
        ));
        this.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(gfx);

        int midX = this.width / 2;
        drawSanctuaryPanel(gfx, panelLeft, panelTop, PANEL_W, panelH, CYAN, 0xFF2B4D5E);

        // Header title & XYZ pill
        gfx.drawString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD),
                panelLeft + 14, panelTop + 10, TEXT_TITLE, true);
        String coordStr = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        int pillW = font.width(coordStr) + 10;
        drawPill(gfx, font, coordStr, panelLeft + PANEL_W - pillW - 14, panelTop + 8, CYAN_DARK, CYAN, CYAN_LIGHT);

        // Stage summary line
        String summary = truncate(this.font, "Stage: " + this.stageSummary.replace("\n", " | "), PANEL_W - 28);
        gfx.drawString(this.font, summary, panelLeft + 14, panelTop + 26, EMERALD_LIGHT, false);

        drawGildedDivider(gfx, panelLeft + 12, panelTop + 40, PANEL_W - 24, CYAN);

        // Draw recessed input wells & labels behind all visible EditBoxes
        drawEditBoxChrome(gfx, stageIdBox, "Stage ID");
        drawCycleLabel(gfx, stageTypeButton, "Stage Type");
        drawCycleLabel(gfx, roleButton, "Marker Role");

        if (enemyTypeBox != null && enemyTypeBox.visible) {
            String label = DungeonStageManager.ROLE_AREA_MODIFIER.equals(currentRole) ? "Target Block / Teleporter ID"
                    : DungeonStageManager.ROLE_BLOCK_MATCH.equals(currentRole) ? "Target Block ID"
                    : DungeonStageManager.ROLE_SECRET_BUTTON_SPAWN.equals(currentRole) ? "Button Block ID"
                    : DungeonStageManager.ROLE_DEATH_AREA.equals(currentRole) ? "Dimensions (X,Y,Z or R)"
                    : "Enemy Type (#tag or ID)";
            drawEditBoxChrome(gfx, enemyTypeBox, label);
        }
        drawEditBoxChrome(gfx, chanceBox, "Spawn Chance (0.0–1.0)");
        drawEditBoxChrome(gfx, bossIdBox, "Boss ID (e.g. blizza)");

        if (countBox != null && countBox.visible) {
            String label = "Count";
            if (DungeonStageManager.ROLE_STAGE_TRIGGER.equals(currentRole)
                    || DungeonStageManager.ROLE_GOAL_AREA.equals(currentRole)
                    || DungeonStageManager.ROLE_AREA_MODIFIER.equals(currentRole)) {
                label = "Radius";
            } else if (DungeonStageManager.ROLE_INFINITE_SPAWNER.equals(currentRole)) {
                label = "Cooldown (s)";
            } else if (DungeonStageManager.ROLE_DOOR.equals(currentRole)) {
                label = "Required Switches";
            } else if (DungeonStageManager.ROLE_BLOCK_MATCH.equals(currentRole)) {
                label = "State (e.g. lit=true)";
            }
            if ("SURVIVE_TIMER".equals(currentStageType) && DungeonStageManager.ROLE_STAGE_TRIGGER.equals(currentRole)) {
                label = "Timer (Seconds)";
            }
            drawEditBoxChrome(gfx, countBox, label);
        }

        if (switchIdBox != null && switchIdBox.visible) {
            String label = DungeonStageManager.ROLE_DOOR.equals(currentRole) ? "Group ID (optional)"
                    : DungeonStageManager.ROLE_STAGE_TRIGGER.equals(currentRole) ? "Key Drop Stage ID (optional)"
                    : "Switch ID";
            drawEditBoxChrome(gfx, switchIdBox, label);
        }

        if (modifierActionButton != null && modifierActionButton.visible) {
            drawCycleLabel(gfx, modifierActionButton,
                    DungeonStageManager.ROLE_DEATH_AREA.equals(currentRole) ? "Shape" : "Action");
        }
        if (triggerBehaviorButton != null && triggerBehaviorButton.visible) {
            drawCycleLabel(gfx, triggerBehaviorButton,
                    DungeonStageManager.ROLE_DEATH_AREA.equals(currentRole) ? "Scope" : "Trigger Behavior");
        }
        drawCycleLabel(gfx, dropsKeyButton, "Drops Key on Complete");
        drawEditBoxChrome(gfx, lootTableBox, "Loot Table Path");
        drawEditBoxChrome(gfx, objectiveBox, "Objective Text (Shown to players on stage start)");

        // Footer divider
        drawGildedDivider(gfx, panelLeft + 12, panelTop + panelH - 34, PANEL_W - 24, GOLD_DIM);
    }

    private void drawEditBoxChrome(GuiGraphics gfx, EditBox box, String label) {
        if (box == null || !box.visible) return;
        int bx = box.getX() - 4;
        int by = box.getY() - 5;
        int bw = box.getWidth() + 8;
        int bh = 18;
        gfx.drawString(this.font, label, bx, by - 10, TEXT_BODY, false);
        drawRecessedWell(gfx, bx, by, bw, bh, box.isFocused() ? CYAN_LIGHT : BRONZE);
    }

    private void drawCycleLabel(GuiGraphics gfx, CustomCycleButton btn, String label) {
        if (btn == null || !btn.visible) return;
        gfx.drawString(this.font, label, btn.getX(), btn.getY() - 10, TEXT_BODY, false);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);

        // Render floating autocomplete popup above all widgets so it never clashes with lower rows
        renderAutocompletePopup(gfx, enemyTypeBox,
                DungeonStageManager.ROLE_AREA_MODIFIER.equals(currentRole)
                        ? getBlockSuggestion(enemyTypeBox != null ? enemyTypeBox.getValue() : "")
                        : getEntitySuggestion(enemyTypeBox != null ? enemyTypeBox.getValue() : ""));
        renderAutocompletePopup(gfx, bossIdBox,
                getEntitySuggestion(bossIdBox != null ? bossIdBox.getValue() : ""));
        renderAutocompletePopup(gfx, lootTableBox,
                getLootTableSuggestion(lootTableBox != null ? lootTableBox.getValue() : ""));
    }

    private void renderAutocompletePopup(GuiGraphics gfx, EditBox box, String suggestion) {
        if (box == null || !box.visible || !box.isFocused() || suggestion == null || suggestion.isEmpty()) return;
        int px = box.getX() - 4;
        int py = box.getY() + 15;
        String hint = "[TAB] ";
        int w = Math.max(box.getWidth() + 8, this.font.width(hint + suggestion) + 12);
        int h = 15;

        gfx.fill(px, py, px + w, py + h, 0xF50B0A14);
        drawBorder(gfx, px, py, w, h, CYAN);
        gfx.drawString(this.font, hint, px + 5, py + 4, GOLD_BRIGHT, false);
        gfx.drawString(this.font, suggestion, px + 5 + this.font.width(hint), py + 4, CYAN_LIGHT, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (this.enemyTypeBox != null && this.enemyTypeBox.isFocused()) {
                String suggestion = DungeonStageManager.ROLE_AREA_MODIFIER.equals(currentRole)
                        ? getBlockSuggestion(this.enemyTypeBox.getValue())
                        : getEntitySuggestion(this.enemyTypeBox.getValue());
                if (suggestion != null) {
                    this.enemyTypeBox.setValue(suggestion);
                    this.enemyTypeBox.setCursorPosition(suggestion.length());
                    return true;
                }
            }
            if (this.bossIdBox != null && this.bossIdBox.isFocused()) {
                String suggestion = getEntitySuggestion(this.bossIdBox.getValue());
                if (suggestion != null) {
                    this.bossIdBox.setValue(suggestion);
                    this.bossIdBox.setCursorPosition(suggestion.length());
                    return true;
                }
            }
            if (this.lootTableBox != null && this.lootTableBox.isFocused()) {
                String suggestion = getLootTableSuggestion(this.lootTableBox.getValue());
                if (suggestion != null) {
                    this.lootTableBox.setValue(suggestion);
                    this.lootTableBox.setCursorPosition(suggestion.length());
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String getEntitySuggestion(String input) {
        if (input == null || input.isEmpty()) return null;
        boolean isTag = input.startsWith("#");
        String searchInput = isTag ? input.substring(1) : input;

        if (isTag) {
            for (TagKey<EntityType<?>> tagKey : BuiltInRegistries.ENTITY_TYPE.getTags().map(com.mojang.datafixers.util.Pair::getFirst).toList()) {
                String id = "#" + tagKey.location();
                if (!searchInput.contains(":") && tagKey.location().getNamespace().equals("oririmod")) {
                    if (tagKey.location().getPath().startsWith(searchInput) && !tagKey.location().getPath().equals(searchInput)) {
                        return id;
                    }
                }
                if (id.startsWith(input) && !id.equals(input)) {
                    return id;
                }
            }
        } else {
            for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                String id = rl.toString();
                if (!input.contains(":") && rl.getNamespace().equals("minecraft")) {
                    if (rl.getPath().startsWith(input) && !rl.getPath().equals(input)) {
                        return id;
                    }
                }
                if (id.startsWith(input) && !id.equals(input)) {
                    return id;
                }
            }
        }
        return null;
    }

    private String getBlockSuggestion(String input) {
        if (input == null || input.isEmpty()) return null;
        for (ResourceLocation rl : BuiltInRegistries.BLOCK.keySet()) {
            String id = rl.toString();
            if (!input.contains(":") && rl.getNamespace().equals("minecraft")) {
                if (rl.getPath().startsWith(input) && !rl.getPath().equals(input)) {
                    return id;
                }
            }
            if (id.startsWith(input) && !id.equals(input)) {
                return id;
            }
        }
        return null;
    }

    private String getLootTableSuggestion(String input) {
        if (input == null || input.isEmpty()) return null;
        try {
            for (ResourceKey<LootTable> key : BuiltInLootTables.all()) {
                ResourceLocation rl = key.location();
                String id = rl.toString();
                if (!input.contains(":") && rl.getNamespace().equals("minecraft")) {
                    if (rl.getPath().startsWith(input) && !rl.getPath().equals(input)) {
                        return id;
                    }
                }
                if (id.startsWith(input) && !id.equals(input)) {
                    return id;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private class CustomCycleButton extends AbstractButton {
        private final String prefix;
        private List<String> values;
        private int currentIndex;
        private final Consumer<String> onValueChange;

        public CustomCycleButton(int x, int y, int width, int height, String prefix,
                                 List<String> values, String initialValue, Consumer<String> onValueChange) {
            super(x, y, width, height, Component.empty());
            this.prefix = prefix;
            this.values = values;
            this.currentIndex = Math.max(0, values.indexOf(initialValue));
            this.onValueChange = onValueChange;
            this.updateText();
        }

        public void setValues(List<String> newValues) {
            String currentVal = this.values.get(this.currentIndex);
            this.values = newValues;
            this.currentIndex = Math.max(0, this.values.indexOf(currentVal));
            this.updateText();
        }

        @Override
        public void onPress() {
            this.currentIndex = (this.currentIndex + 1) % this.values.size();
            this.updateText();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.active && this.visible && this.clicked(mouseX, mouseY)) {
                if (button == 0) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.onPress();
                    return true;
                } else if (button == 1) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.currentIndex = (this.currentIndex - 1 + this.values.size()) % this.values.size();
                    this.updateText();
                    return true;
                }
            }
            return false;
        }

        private void updateText() {
            this.setMessage(Component.literal(this.values.get(this.currentIndex)));
            this.onValueChange.accept(this.values.get(this.currentIndex));
        }

        @Override
        protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            boolean hovered = this.active && this.isHoveredOrFocused();

            gfx.fill(x, y, x + w, y + h, OUTER_SHADOW);
            gfx.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1,
                    hovered ? 0xEE1F3140 : 0xDD151726,
                    hovered ? 0xEE121E29 : 0xDD0D0E1A);
            drawBorder(gfx, x, y, w, h, hovered ? CYAN_LIGHT : GOLD_DIM);

            Font f = Minecraft.getInstance().font;
            String val = truncate(f, prefix + ": " + this.values.get(this.currentIndex), w - 20);
            gfx.drawString(f, "‹", x + 4, y + (h - 8) / 2, hovered ? CYAN_LIGHT : GOLD_DIM, false);
            gfx.drawCenteredString(f, val, x + w / 2, y + (h - 8) / 2, hovered ? GOLD_BRIGHT : TEXT_PRIMARY);
            gfx.drawString(f, "›", x + w - 8, y + (h - 8) / 2, hovered ? CYAN_LIGHT : GOLD_DIM, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
