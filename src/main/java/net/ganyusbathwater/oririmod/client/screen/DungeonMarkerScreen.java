package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.dungeon.stage.DungeonStageManager;
import net.ganyusbathwater.oririmod.network.packet.OpenMarkerScreenPayload;
import net.ganyusbathwater.oririmod.network.packet.SyncMarkerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class DungeonMarkerScreen extends Screen {

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
    private CustomCycleButton modifierActionButton;

    private final String initialStageId;
    private final String initialStageType;
    private final String initialRole;
    private final String initialEnemyType;
    private final int initialCount;
    private final String initialSwitchId;
    private final String initialLootTable;
    private final String initialBossId;
    private final float initialChance;
    private final net.minecraft.core.BlockPos pos;
    private final String stageSummary;
    
    private String currentStageType;
    private String currentRole;

    private static final List<String> STAGE_TYPES = Arrays.asList(
            "KILL_ALL_ENEMIES", "ACTIVATE_SWITCHES", "SURVIVE_TIMER", "BOSS_FIGHT", "FETCH_ITEM", "PUZZLE_SOLVE"
    );

    private static final List<String> ROLES = Arrays.asList(
            DungeonStageManager.ROLE_SPAWN_POINT,
            DungeonStageManager.ROLE_INFINITE_SPAWNER,
            DungeonStageManager.ROLE_SWITCH,
            DungeonStageManager.ROLE_DOOR,
            DungeonStageManager.ROLE_STAGE_TRIGGER,
            DungeonStageManager.ROLE_AREA_MODIFIER,
            DungeonStageManager.ROLE_BOSS_SPAWN,
            DungeonStageManager.ROLE_LOOT_CHEST
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
        this.pos = payload.pos();
        this.stageSummary = payload.stageSummary();
        
        this.currentStageType = this.initialStageType;
        this.currentRole = this.initialRole;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        int midX = this.width / 2;
        int startY = 60;
        int rowSpacing = 28;

        // Column 1
        this.stageIdBox = new EditBox(this.font, midX - 160, startY, 140, 20, Component.literal("Stage ID"));
        this.stageIdBox.setValue(initialStageId);
        this.stageIdBox.setMaxLength(64);
        this.stageIdBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("The ID grouping all markers in this stage.")));
        this.addRenderableWidget(this.stageIdBox);

        this.stageTypeButton = new CustomCycleButton(
                midX - 160, startY + rowSpacing, 140, 20,
                "Stage Type", STAGE_TYPES, initialStageType,
                val -> {
                    this.currentStageType = val;
                    this.updateUIState();
                }
        );
        this.stageTypeButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("The overall goal/type for this stage.")));
        this.addRenderableWidget(this.stageTypeButton);

        this.roleButton = new CustomCycleButton(
                midX + 20, startY + rowSpacing, 140, 20,
                "Role", ROLES, initialRole,
                val -> {
                    this.currentRole = val;
                    this.updateUIState();
                }
        );
        this.roleButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("What THIS specific marker does.")));
        this.addRenderableWidget(this.roleButton);

        this.enemyTypeBox = new EditBox(this.font, midX - 160, startY + rowSpacing * 3, 140, 20, Component.literal("Enemy Type"));
        this.enemyTypeBox.setValue(initialEnemyType);
        this.enemyTypeBox.setMaxLength(128);
        this.addRenderableWidget(this.enemyTypeBox);

        this.chanceBox = new EditBox(this.font, midX - 160, startY + rowSpacing * 4, 140, 20, Component.literal("Spawn Chance"));
        this.chanceBox.setValue(String.valueOf(initialChance));
        this.chanceBox.setMaxLength(10);
        this.addRenderableWidget(this.chanceBox);

        this.countBox = new EditBox(this.font, midX + 20, startY, 140, 20, Component.literal("Count"));
        this.countBox.setValue(String.valueOf(initialCount));
        this.countBox.setMaxLength(10);
        this.addRenderableWidget(this.countBox);

        this.switchIdBox = new EditBox(this.font, midX + 20, startY + rowSpacing, 140, 20, Component.literal("Switch ID"));
        this.switchIdBox.setValue(initialSwitchId);
        this.switchIdBox.setMaxLength(64);
        this.addRenderableWidget(this.switchIdBox);

        this.modifierActionButton = new CustomCycleButton(
                midX + 20, startY + rowSpacing, 140, 20,
                "Action", Arrays.asList("fill", "destroy"), initialSwitchId.isEmpty() ? "fill" : (initialSwitchId.equals("destroy") ? "destroy" : "fill"),
                val -> {
                    this.switchIdBox.setValue(val);
                }
        );
        this.addRenderableWidget(this.modifierActionButton);

        this.lootTableBox = new EditBox(this.font, midX + 20, startY + rowSpacing * 2, 140, 20, Component.literal("Loot Table"));
        this.lootTableBox.setValue(initialLootTable);
        this.lootTableBox.setMaxLength(128);
        this.addRenderableWidget(this.lootTableBox);

        this.bossIdBox = new EditBox(this.font, midX + 20, startY + rowSpacing * 3, 140, 20, Component.literal("Boss ID"));
        this.bossIdBox.setValue(initialBossId);
        this.bossIdBox.setMaxLength(64);
        this.addRenderableWidget(this.bossIdBox);

        // Save Button
        this.addRenderableWidget(Button.builder(Component.literal("Save").withStyle(ChatFormatting.GREEN), b -> saveAndClose())
                .bounds(this.width / 2 - 115, this.height - 40, 230, 20).build());
        
        updateUIState();
    }
    
    private boolean isUpdatingUI = false;

    private void updateUIState() {
        if (enemyTypeBox == null) return; // Prevent crash if called during initialization of cycle buttons
        if (isUpdatingUI) return;
        
        isUpdatingUI = true;
        
        // Reset all visibility
        enemyTypeBox.visible = false;
        chanceBox.visible = false;
        countBox.visible = false;
        switchIdBox.visible = false;
        modifierActionButton.visible = false;
        lootTableBox.visible = false;
        bossIdBox.visible = false;
        
        java.util.List<String> validRoles = new java.util.ArrayList<>();
        validRoles.add(DungeonStageManager.ROLE_DOOR);
        validRoles.add(DungeonStageManager.ROLE_STAGE_TRIGGER);
        validRoles.add(DungeonStageManager.ROLE_AREA_MODIFIER);
        validRoles.add(DungeonStageManager.ROLE_LOOT_CHEST);

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
            }
            case "PUZZLE_SOLVE" -> {
                validRoles.add(0, DungeonStageManager.ROLE_SWITCH);
            }
            case "SPAWN_ONLY" -> {
                validRoles.add(0, DungeonStageManager.ROLE_SPAWN_POINT);
            }
        }
        
        if (roleButton != null) {
            roleButton.setValues(validRoles);
            if (!validRoles.contains(currentRole)) {
                currentRole = validRoles.get(0);
            }
        }

        boolean isModifier = DungeonStageManager.ROLE_AREA_MODIFIER.equals(currentRole);
        boolean isDoor = DungeonStageManager.ROLE_DOOR.equals(currentRole);
        boolean isTrigger = DungeonStageManager.ROLE_STAGE_TRIGGER.equals(currentRole);
        boolean isBoss = DungeonStageManager.ROLE_BOSS_SPAWN.equals(currentRole);
        boolean isChest = DungeonStageManager.ROLE_LOOT_CHEST.equals(currentRole);
        boolean isSwitch = DungeonStageManager.ROLE_SWITCH.equals(currentRole);
        boolean isSpawn = DungeonStageManager.ROLE_SPAWN_POINT.equals(currentRole);
        boolean isSpawner = DungeonStageManager.ROLE_INFINITE_SPAWNER.equals(currentRole);

        if (isSpawn || isSpawner) {
            enemyTypeBox.visible = true;
            countBox.visible = true;
            chanceBox.visible = true;
        } else if (isModifier) {
            enemyTypeBox.visible = true;
            countBox.visible = true;
            modifierActionButton.visible = true;
        } else if (isDoor) {
            switchIdBox.visible = true;
            countBox.visible = true;
        } else if (isTrigger) {
            countBox.visible = true;
            switchIdBox.visible = true;
        } else if (isSwitch) {
            switchIdBox.visible = true;
        } else if (isBoss) {
            bossIdBox.visible = true;
        } else if (isChest) {
            lootTableBox.visible = true;
        }

        if (currentStageType.equals("SURVIVE_TIMER") && !isModifier && !isDoor && !isTrigger) {
            countBox.visible = true;
        }

        // Layout visible boxes dynamically in two columns to prevent holes
        int midX = this.width / 2;
        int startY = 116;
        int rowSpacing = 36;
        
        net.minecraft.client.gui.components.AbstractWidget[] col1 = { enemyTypeBox, chanceBox, bossIdBox };
        net.minecraft.client.gui.components.AbstractWidget[] col2 = { countBox, switchIdBox, modifierActionButton, lootTableBox };
        
        int row1 = 0;
        for (net.minecraft.client.gui.components.AbstractWidget box : col1) {
            if (box.visible) {
                box.setPosition(midX - 160, startY + (rowSpacing * row1));
                row1++;
            }
        }
        
        int row2 = 0;
        for (net.minecraft.client.gui.components.AbstractWidget box : col2) {
            if (box.visible) {
                box.setPosition(midX + 20, startY + (rowSpacing * row2));
                row2++;
            }
        }
        
        isUpdatingUI = false;
    }

    private void saveAndClose() {
        int count = 0;
        try { count = Integer.parseInt(countBox.getValue()); } catch (NumberFormatException ignored) {}
        
        float chance = 1.0f;
        try { chance = Float.parseFloat(chanceBox.getValue()); } catch (NumberFormatException ignored) {}

        PacketDistributor.sendToServer(new SyncMarkerDataPayload(
                entityId,
                stageIdBox.getValue(),
                currentStageType,
                currentRole,
                enemyTypeBox.getValue(),
                count,
                DungeonStageManager.ROLE_AREA_MODIFIER.equals(currentRole) ? (modifierActionButton.values.get(modifierActionButton.currentIndex)) : switchIdBox.getValue(),
                lootTableBox.getValue(),
                bossIdBox.getValue(),
                chance
        ));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int midX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), this.width / 2, 15, 0xFFFFFF);
        String coordText = "XYZ: " + this.pos.getX() + ", " + this.pos.getY() + ", " + this.pos.getZ();
        guiGraphics.drawCenteredString(this.font, Component.literal(coordText).withStyle(ChatFormatting.GRAY), this.width / 2, 30, 0xFFFFFF);
        
        // Render Summary text as a line
        guiGraphics.drawCenteredString(this.font, Component.literal("In this stage: " + this.stageSummary.replace("\n", " | ")).withStyle(ChatFormatting.DARK_GREEN), this.width / 2, 45, 0xFFFFFF);

        // Fixed header labels
        guiGraphics.drawString(this.font, "Stage ID", this.stageIdBox.getX(), this.stageIdBox.getY() - 10, 0xDDDDDD);

        // Dynamic labels based on role
        if (this.enemyTypeBox.visible) {
            String label = DungeonStageManager.ROLE_AREA_MODIFIER.equals(currentRole) ? "Block to Place/Destroy (e.g. minecraft:stone)" : "Enemy Type (e.g. minecraft:zombie)";
            guiGraphics.drawString(this.font, label, this.enemyTypeBox.getX(), this.enemyTypeBox.getY() - 10, 0xDDDDDD);
        }
        if (this.chanceBox.visible) {
            guiGraphics.drawString(this.font, "Spawn Chance (0.0 to 1.0)", this.chanceBox.getX(), this.chanceBox.getY() - 10, 0xDDDDDD);
        }
        if (this.bossIdBox.visible) {
            guiGraphics.drawString(this.font, "Boss ID (e.g. blizza)", this.bossIdBox.getX(), this.bossIdBox.getY() - 10, 0xDDDDDD);
        }
        if (this.countBox.visible) {
            String label = "Count";
            if (DungeonStageManager.ROLE_AREA_MODIFIER.equals(currentRole) || DungeonStageManager.ROLE_STAGE_TRIGGER.equals(currentRole)) label = "Radius (Blocks)";
            else if (DungeonStageManager.ROLE_DOOR.equals(currentRole)) label = "Required Switches";
            else if (currentStageType.equals("SURVIVE_TIMER")) label = "Timer (Seconds)";
            guiGraphics.drawString(this.font, label, this.countBox.getX(), this.countBox.getY() - 10, 0xDDDDDD);
        }
        if (this.switchIdBox.visible) {
            String label = "Switch ID";
            if (DungeonStageManager.ROLE_DOOR.equals(currentRole)) label = "Group ID (optional)";
            else if (DungeonStageManager.ROLE_STAGE_TRIGGER.equals(currentRole)) label = "Key Drop Stage ID (optional)";
            guiGraphics.drawString(this.font, label, this.switchIdBox.getX(), this.switchIdBox.getY() - 10, 0xDDDDDD);
        }
        if (this.modifierActionButton.visible) {
            guiGraphics.drawString(this.font, "Action", this.modifierActionButton.getX(), this.modifierActionButton.getY() - 10, 0xDDDDDD);
        }
        if (this.lootTableBox.visible) {
            guiGraphics.drawString(this.font, "Loot Table Path", this.lootTableBox.getX(), this.lootTableBox.getY() - 10, 0xDDDDDD);
        }

        if (this.enemyTypeBox.visible && this.enemyTypeBox.isFocused()) {
            String input = this.enemyTypeBox.getValue();
            if (!input.isEmpty()) {
                String suggestion = getEntitySuggestion(input);
                if (suggestion != null) {
                    guiGraphics.drawString(this.font, suggestion, this.enemyTypeBox.getX() + 4, this.enemyTypeBox.getY() + 22, ChatFormatting.DARK_GRAY.getColor(), false);
                    guiGraphics.drawString(this.font, "[TAB] to autocomplete", this.enemyTypeBox.getX() + 4, this.enemyTypeBox.getY() + 32, ChatFormatting.YELLOW.getColor(), false);
                }
            }
        }

        if (this.bossIdBox.visible && this.bossIdBox.isFocused()) {
            String input = this.bossIdBox.getValue();
            if (!input.isEmpty()) {
                String suggestion = getEntitySuggestion(input);
                if (suggestion != null) {
                    guiGraphics.drawString(this.font, suggestion, this.bossIdBox.getX() + 4, this.bossIdBox.getY() + 22, ChatFormatting.DARK_GRAY.getColor(), false);
                    guiGraphics.drawString(this.font, "[TAB] to autocomplete", this.bossIdBox.getX() + 4, this.bossIdBox.getY() + 32, ChatFormatting.YELLOW.getColor(), false);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
            if (this.enemyTypeBox.isFocused()) {
                String suggestion = getEntitySuggestion(this.enemyTypeBox.getValue());
                if (suggestion != null) {
                    this.enemyTypeBox.setValue(suggestion);
                    this.enemyTypeBox.setCursorPosition(suggestion.length());
                    return true;
                }
            }
            if (this.bossIdBox.isFocused()) {
                String suggestion = getEntitySuggestion(this.bossIdBox.getValue());
                if (suggestion != null) {
                    this.bossIdBox.setValue(suggestion);
                    this.bossIdBox.setCursorPosition(suggestion.length());
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String getEntitySuggestion(String input) {
        if (input.isEmpty()) return null;
        for (net.minecraft.resources.ResourceLocation rl : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet()) {
            String id = rl.toString();
            // Automatically prefix with minecraft: if missing to make it easier for the user
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

    private class CustomCycleButton extends AbstractButton {
        private final String prefix;
        private List<String> values;
        private int currentIndex;
        private final java.util.function.Consumer<String> onValueChange;

        public CustomCycleButton(int x, int y, int width, int height, String prefix, List<String> values, String initialValue, java.util.function.Consumer<String> onValueChange) {
            super(x, y, width, height, Component.literal(""));
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
                if (button == 0) { // Left click
                    this.playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
                    this.onPress();
                    return true;
                } else if (button == 1) { // Right click
                    this.playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
                    this.currentIndex = (this.currentIndex - 1 + this.values.size()) % this.values.size();
                    this.updateText();
                    return true;
                }
            }
            return false;
        }

        private void updateText() {
            this.setMessage(Component.literal(prefix + ": " + this.values.get(this.currentIndex)));
            this.onValueChange.accept(this.values.get(this.currentIndex));
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
