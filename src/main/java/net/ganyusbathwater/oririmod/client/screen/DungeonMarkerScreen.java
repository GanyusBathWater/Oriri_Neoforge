package net.ganyusbathwater.oririmod.client.screen;

import net.ganyusbathwater.oririmod.dungeon.stage.DungeonStageManager;
import net.ganyusbathwater.oririmod.network.packet.OpenMarkerScreenPayload;
import net.ganyusbathwater.oririmod.network.packet.SyncMarkerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
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
    private CycleButton<String> stageTypeButton;
    private CycleButton<String> roleButton;
    private EditBox enemyTypeBox;
    private EditBox countBox;
    private EditBox switchIdBox;
    private EditBox lootTableBox;
    private EditBox bossIdBox;

    private final String initialStageId;
    private final String initialStageType;
    private final String initialRole;
    private final String initialEnemyType;
    private final int initialCount;
    private final String initialSwitchId;
    private final String initialLootTable;
    private final String initialBossId;
    private final net.minecraft.core.BlockPos pos;
    private final String stageSummary;
    
    private String currentStageType;
    private String currentRole;

    private static final List<String> STAGE_TYPES = Arrays.asList(
            "KILL_ALL_ENEMIES", "ACTIVATE_SWITCHES", "SURVIVE_TIMER", "BOSS_FIGHT", "FETCH_ITEM", "PUZZLE_SOLVE"
    );

    private static final List<String> ROLES = Arrays.asList(
            DungeonStageManager.ROLE_SPAWN_POINT,
            DungeonStageManager.ROLE_SWITCH,
            DungeonStageManager.ROLE_DOOR,
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

        this.stageTypeButton = CycleButton.builder(Component::literal)
                .withValues(STAGE_TYPES)
                .withInitialValue(STAGE_TYPES.contains(initialStageType) ? initialStageType : STAGE_TYPES.get(0))
                .withTooltip(val -> net.minecraft.client.gui.components.Tooltip.create(Component.literal("The overall goal/type for this stage.")))
                .create(midX - 160, startY + rowSpacing, 140, 20, Component.literal("Stage Type"), (btn, val) -> this.currentStageType = val);
        this.addRenderableWidget(this.stageTypeButton);

        this.roleButton = CycleButton.builder(Component::literal)
                .withValues(ROLES)
                .withInitialValue(ROLES.contains(initialRole) ? initialRole : ROLES.get(0))
                .withTooltip(val -> net.minecraft.client.gui.components.Tooltip.create(Component.literal("What THIS specific marker does.")))
                .create(midX - 160, startY + rowSpacing * 2, 140, 20, Component.literal("Role"), (btn, val) -> this.currentRole = val);
        this.addRenderableWidget(this.roleButton);

        this.enemyTypeBox = new EditBox(this.font, midX - 160, startY + rowSpacing * 3, 140, 20, Component.literal("Enemy Type"));
        this.enemyTypeBox.setValue(initialEnemyType);
        this.enemyTypeBox.setMaxLength(128);
        this.enemyTypeBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("e.g. 'minecraft:zombie' for spawn points.")));
        this.addRenderableWidget(this.enemyTypeBox);

        // Column 2
        this.countBox = new EditBox(this.font, midX + 20, startY, 140, 20, Component.literal("Count"));
        this.countBox.setValue(String.valueOf(initialCount));
        this.countBox.setMaxLength(10);
        this.countBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Amount of enemies, time to survive, etc.")));
        this.addRenderableWidget(this.countBox);

        this.switchIdBox = new EditBox(this.font, midX + 20, startY + rowSpacing, 140, 20, Component.literal("Switch ID"));
        this.switchIdBox.setValue(initialSwitchId);
        this.switchIdBox.setMaxLength(64);
        this.switchIdBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Matches a switch with its corresponding door.")));
        this.addRenderableWidget(this.switchIdBox);

        this.lootTableBox = new EditBox(this.font, midX + 20, startY + rowSpacing * 2, 140, 20, Component.literal("Loot Table"));
        this.lootTableBox.setValue(initialLootTable);
        this.lootTableBox.setMaxLength(128);
        this.lootTableBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("The loot table applied to chests/rewards.")));
        this.addRenderableWidget(this.lootTableBox);

        this.bossIdBox = new EditBox(this.font, midX + 20, startY + rowSpacing * 3, 140, 20, Component.literal("Boss ID"));
        this.bossIdBox.setValue(initialBossId);
        this.bossIdBox.setMaxLength(64);
        this.bossIdBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Boss Entity ID if this is a boss stage.")));
        this.addRenderableWidget(this.bossIdBox);

        // Stage Summary Info Button
        Button summaryBtn = Button.builder(Component.literal("Stage Info"), b -> {})
                .bounds(midX - 160, startY + rowSpacing * 5, 80, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(this.stageSummary)))
                .build();
        summaryBtn.active = false; // Just for hovering
        this.addRenderableWidget(summaryBtn);

        // Save Button
        this.addRenderableWidget(Button.builder(Component.literal("Save").withStyle(ChatFormatting.GREEN), b -> saveAndClose())
                .bounds(midX - 70, startY + rowSpacing * 5, 230, 20).build());
    }

    private void saveAndClose() {
        int count = 0;
        try { count = Integer.parseInt(countBox.getValue()); } catch (NumberFormatException ignored) {}

        PacketDistributor.sendToServer(new SyncMarkerDataPayload(
                entityId,
                stageIdBox.getValue(),
                currentStageType,
                currentRole,
                enemyTypeBox.getValue(),
                count,
                switchIdBox.getValue(),
                lootTableBox.getValue(),
                bossIdBox.getValue()
        ));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int midX = this.width / 2;
        int startY = 60;
        int rowSpacing = 28;

        guiGraphics.drawCenteredString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), this.width / 2, 15, 0xFFFFFF);
        String coordText = "XYZ: " + this.pos.getX() + ", " + this.pos.getY() + ", " + this.pos.getZ();
        guiGraphics.drawCenteredString(this.font, Component.literal(coordText).withStyle(ChatFormatting.GRAY), this.width / 2, 30, 0xFFFFFF);

        // Labels for EditBoxes
        guiGraphics.drawString(this.font, "Stage ID", midX - 160, startY - 10, 0xDDDDDD);
        guiGraphics.drawString(this.font, "Enemy Type (e.g. minecraft:zombie)", midX - 160, startY + rowSpacing * 3 - 10, 0xDDDDDD);
        
        guiGraphics.drawString(this.font, "Count / Timer (Ticks)", midX + 20, startY - 10, 0xDDDDDD);
        guiGraphics.drawString(this.font, "Switch / Door ID", midX + 20, startY + rowSpacing - 10, 0xDDDDDD);
        guiGraphics.drawString(this.font, "Loot Table (e.g. oririmod:...)", midX + 20, startY + rowSpacing * 2 - 10, 0xDDDDDD);
        guiGraphics.drawString(this.font, "Boss ID (e.g. blizza)", midX + 20, startY + rowSpacing * 3 - 10, 0xDDDDDD);
    }
}
