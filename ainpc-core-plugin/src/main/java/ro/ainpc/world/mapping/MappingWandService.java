package ro.ainpc.world.mapping;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ro.ainpc.AINPCPlugin;
import ro.ainpc.gui.GuiItemFactory;
import ro.ainpc.world.WorldAdminService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

public class MappingWandService {

    private static final int MAX_RECENT_CONFIRMED_DRAFTS = 25;

    private final AINPCPlugin plugin;
    private final NamespacedKey wandKey;
    private final MappingDraftFactory draftFactory;
    private final ConcurrentMap<UUID, MappingWandSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<MappingWandAuditEntry> recentConfirmedDrafts = new ConcurrentLinkedDeque<>();

    public MappingWandService(AINPCPlugin plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "mapping_wand");
        this.draftFactory = new MappingDraftFactory();
    }

    public MappingWandSession start(Player player, MappingWandMode mode) {
        MappingWandMode safeMode = mode != null ? mode : MappingWandMode.PLACE;
        MappingWandSession session = sessions.compute(player.getUniqueId(), (ignored, existing) -> {
            MappingWandSelection selection = existing != null ? existing.selection() : MappingWandSelection.empty();
            MappingDraft draft = existing != null ? existing.draft() : null;
            return new MappingWandSession(safeMode, selection, draft);
        });
        giveWand(player);
        return session;
    }

    public MappingWandSession ensureSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(),
            ignored -> new MappingWandSession(MappingWandMode.PLACE, MappingWandSelection.empty(), null));
    }

    public Optional<MappingWandSession> session(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public MappingWandMode mode(UUID playerId) {
        return session(playerId).map(MappingWandSession::mode).orElse(MappingWandMode.PLACE);
    }

    public MappingWandSession setMode(Player player, MappingWandMode mode) {
        return start(player, mode);
    }

    public MappingWandSession setPos1(Player player, MappingPoint point) {
        return updateSelection(player, selection -> selection.withPos1(point));
    }

    public MappingWandSession setPos2(Player player, MappingPoint point) {
        return updateSelection(player, selection -> selection.withPos2(point));
    }

    public MappingWandSession setPoint(Player player, MappingPoint point) {
        return updateSelection(player, selection -> selection.withPoint(point));
    }

    public MappingWandSession resetPos1(Player player) {
        return updateSelection(player, MappingWandSelection::withoutPos1);
    }

    public MappingWandSession resetPos2(Player player) {
        return updateSelection(player, MappingWandSelection::withoutPos2);
    }

    public MappingWandSession resetPoint(Player player) {
        return updateSelection(player, MappingWandSelection::withoutPoint);
    }

    public MappingDraft createDraft(Player player,
                                    MappingDraftKind explicitKind,
                                    String description,
                                    WorldAdminService worldAdmin) {
        MappingWandSession session = ensureSession(player);
        MappingDraftKind kind = explicitKind != null ? explicitKind : session.mode().draftKind();
        MappingDraft draft = draftFactory.createDraft(
            player.getUniqueId(),
            kind,
            session.selection(),
            description,
            worldAdmin
        );
        sessions.put(player.getUniqueId(), new MappingWandSession(session.mode(), session.selection(), draft));
        return draft;
    }

    public MappingDraftApplyResult confirmDraft(Player player, WorldAdminService worldAdmin) {
        MappingWandSession session = ensureSession(player);
        if (session.draft() == null) {
            throw new IllegalArgumentException("Nu exista draft de confirmat. Ruleaza /ainpc map <descriere>.");
        }
        MappingDraftApplyResult result = draftFactory.apply(session.draft(), worldAdmin);
        recordConfirmedDraft(player, session.draft(), result.createdId(), result.message());
        sessions.put(player.getUniqueId(), new MappingWandSession(session.mode(), session.selection(), null));
        return result;
    }

    public void recordConfirmedDraft(Player player, MappingDraft draft, String resultId, String resultMessage) {
        if (player == null || draft == null) {
            return;
        }
        recentConfirmedDrafts.addFirst(new MappingWandAuditEntry(
            Instant.now().toEpochMilli(),
            player.getUniqueId(),
            player.getName(),
            draft.kind(),
            draft.qualifiedId(),
            resultId,
            resultMessage,
            draft.worldName(),
            draft.regionId(),
            draft.placeId(),
            draft.x(),
            draft.y(),
            draft.z(),
            draft.metadata()
        ));
        while (recentConfirmedDrafts.size() > MAX_RECENT_CONFIRMED_DRAFTS) {
            recentConfirmedDrafts.pollLast();
        }
    }

    public List<MappingWandAuditEntry> recentConfirmedDrafts() {
        return List.copyOf(recentConfirmedDrafts);
    }

    public void showSelectionPreview(Player player, MappingWandSession session) {
        if (player == null || session == null) {
            return;
        }
        MappingWandSelection selection = session.selection();
        if (session.mode().usesPointSelection() && selection.hasPoint()) {
            showRadiusPreview(player, selection.point().worldName(), selection.point().x(), selection.point().y(),
                selection.point().z(), previewRadiusForMode(session.mode()));
            return;
        }
        selection.bounds().ifPresent(bounds -> showBoundsPreview(player, bounds));
    }

    public void showDraftPreview(Player player, MappingDraft draft) {
        if (player == null || draft == null) {
            return;
        }
        if (draft.isBox()) {
            showBoundsPreview(player, new MappingWandSelection.MappingBounds(
                draft.worldName(),
                draft.minX(),
                draft.minY(),
                draft.minZ(),
                draft.maxX(),
                draft.maxY(),
                draft.maxZ()
            ));
            return;
        }
        if (draft.isNode() || draft.isNpcBind() || draft.isQuestAnchor()) {
            showRadiusPreview(player, draft.worldName(), draft.x(), draft.y(), draft.z(), draft.radius());
        }
    }

    public void clear(UUID playerId) {
        sessions.remove(playerId);
    }

    public void cancelDraft(UUID playerId) {
        MappingWandSession session = sessions.get(playerId);
        if (session != null) {
            sessions.put(playerId, new MappingWandSession(session.mode(), session.selection(), null));
        }
    }

    public boolean isWandItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null
            && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public ItemStack createWandItem() {
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(GuiItemFactory.text("&dAINPC Mapping Wand"));
            meta.lore(List.of(
                GuiItemFactory.text("&7Stanga: pos1 pentru region/place"),
                GuiItemFactory.text("&7Dreapta: pos2 pentru region/place"),
                GuiItemFactory.text("&7Node/npc_bind/quest_anchor: punct semantic"),
                GuiItemFactory.text("&8/ainpc wand mode <region|place|node|npc_bind|quest_anchor>")
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void giveWand(Player player) {
        ItemStack wand = createWandItem();
        if (player.getInventory().firstEmpty() >= 0) {
            player.getInventory().addItem(wand);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), wand);
            plugin.getMessageUtils().send(player, "&eInventarul este plin; wand-ul a fost lasat langa tine.");
        }
    }

    private MappingWandSession updateSelection(Player player, SelectionUpdater updater) {
        return sessions.compute(player.getUniqueId(), (ignored, existing) -> {
            MappingWandSession safe = existing != null
                ? existing
                : new MappingWandSession(MappingWandMode.PLACE, MappingWandSelection.empty(), null);
            return new MappingWandSession(safe.mode(), updater.update(safe.selection()), safe.draft());
        });
    }

    private void showBoundsPreview(Player player, MappingWandSelection.MappingBounds bounds) {
        if (!canPreviewInPlayerWorld(player, bounds.worldName())) {
            return;
        }

        int stepX = previewStep(bounds.minX(), bounds.maxX());
        int stepY = previewStep(bounds.minY(), bounds.maxY());
        int stepZ = previewStep(bounds.minZ(), bounds.maxZ());

        for (int x = bounds.minX(); x <= bounds.maxX(); x += stepX) {
            spawnPreviewParticle(player, Particle.END_ROD, x + 0.5D, bounds.minY() + 0.5D, bounds.minZ() + 0.5D);
            spawnPreviewParticle(player, Particle.END_ROD, x + 0.5D, bounds.minY() + 0.5D, bounds.maxZ() + 0.5D);
            spawnPreviewParticle(player, Particle.END_ROD, x + 0.5D, bounds.maxY() + 0.5D, bounds.minZ() + 0.5D);
            spawnPreviewParticle(player, Particle.END_ROD, x + 0.5D, bounds.maxY() + 0.5D, bounds.maxZ() + 0.5D);
        }
        for (int y = bounds.minY(); y <= bounds.maxY(); y += stepY) {
            spawnPreviewParticle(player, Particle.END_ROD, bounds.minX() + 0.5D, y + 0.5D, bounds.minZ() + 0.5D);
            spawnPreviewParticle(player, Particle.END_ROD, bounds.maxX() + 0.5D, y + 0.5D, bounds.minZ() + 0.5D);
            spawnPreviewParticle(player, Particle.END_ROD, bounds.minX() + 0.5D, y + 0.5D, bounds.maxZ() + 0.5D);
            spawnPreviewParticle(player, Particle.END_ROD, bounds.maxX() + 0.5D, y + 0.5D, bounds.maxZ() + 0.5D);
        }
        for (int z = bounds.minZ(); z <= bounds.maxZ(); z += stepZ) {
            spawnPreviewParticle(player, Particle.END_ROD, bounds.minX() + 0.5D, bounds.minY() + 0.5D, z + 0.5D);
            spawnPreviewParticle(player, Particle.END_ROD, bounds.maxX() + 0.5D, bounds.minY() + 0.5D, z + 0.5D);
            spawnPreviewParticle(player, Particle.END_ROD, bounds.minX() + 0.5D, bounds.maxY() + 0.5D, z + 0.5D);
            spawnPreviewParticle(player, Particle.END_ROD, bounds.maxX() + 0.5D, bounds.maxY() + 0.5D, z + 0.5D);
        }
    }

    private void showRadiusPreview(Player player, String worldName, double x, double y, double z, double radius) {
        if (!canPreviewInPlayerWorld(player, worldName)) {
            return;
        }

        double safeRadius = Math.max(0.75D, Math.min(radius > 0.0D ? radius : 2.5D, 16.0D));
        double centerX = x + 0.5D;
        double centerY = y + 1.0D;
        double centerZ = z + 0.5D;
        for (int i = 0; i < 32; i++) {
            double angle = (Math.PI * 2.0D * i) / 32.0D;
            spawnPreviewParticle(
                player,
                Particle.ENCHANT,
                centerX + Math.cos(angle) * safeRadius,
                centerY,
                centerZ + Math.sin(angle) * safeRadius
            );
        }
        spawnPreviewParticle(player, Particle.HAPPY_VILLAGER, centerX, centerY, centerZ);
    }

    private boolean canPreviewInPlayerWorld(Player player, String worldName) {
        return player.getWorld() != null
            && worldName != null
            && !worldName.isBlank()
            && player.getWorld().getName().equalsIgnoreCase(worldName);
    }

    private int previewStep(int min, int max) {
        return Math.max(1, Math.max(1, max - min) / 10);
    }

    private double previewRadiusForMode(MappingWandMode mode) {
        return switch (mode) {
            case NPC_BIND -> 1.5D;
            case QUEST_ANCHOR -> 2.0D;
            case NODE -> 2.5D;
            default -> 2.5D;
        };
    }

    private void spawnPreviewParticle(Player player, Particle particle, double x, double y, double z) {
        player.spawnParticle(particle, new Location(player.getWorld(), x, y, z), 1, 0.02D, 0.02D, 0.02D, 0.0D);
    }

    @FunctionalInterface
    private interface SelectionUpdater {
        MappingWandSelection update(MappingWandSelection selection);
    }

    public record MappingWandSession(MappingWandMode mode, MappingWandSelection selection, MappingDraft draft) {
        public MappingWandSession {
            mode = mode != null ? mode : MappingWandMode.PLACE;
            selection = selection != null ? selection : MappingWandSelection.empty();
        }
    }

    public record MappingWandAuditEntry(
        long confirmedAt,
        UUID playerId,
        String playerName,
        MappingDraftKind kind,
        String qualifiedId,
        String resultId,
        String resultMessage,
        String worldName,
        String regionId,
        String placeId,
        double x,
        double y,
        double z,
        Map<String, String> metadata
    ) {
        public MappingWandAuditEntry {
            playerName = playerName == null || playerName.isBlank() ? "<necunoscut>" : playerName.trim();
            qualifiedId = qualifiedId == null ? "" : qualifiedId.trim();
            resultId = resultId == null ? "" : resultId.trim();
            resultMessage = resultMessage == null ? "" : resultMessage.trim();
            worldName = worldName == null ? "" : worldName.trim();
            regionId = regionId == null ? "" : regionId.trim();
            placeId = placeId == null ? "" : placeId.trim();
            metadata = Map.copyOf(metadata != null ? metadata : Map.of());
        }
    }
}
