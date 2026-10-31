package forge.adventure.world;

import com.badlogic.gdx.utils.TimeUtils;
import forge.Forge;
import com.badlogic.gdx.Gdx;
import forge.adventure.data.DifficultyData;
import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.campaign.CampaignManager;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.scene.MapViewScene;
import forge.adventure.scene.GameScene;
import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.PointOfInterestMapSprite;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.*;
import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.deck.Deck;
import forge.localinstance.properties.ForgeConstants;
import forge.player.GamePlayerUtil;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Represents everything that will be saved, like the player and the world.
 */
public class WorldSave {

    static final public int AUTO_SAVE_SLOT = -1;
    static final public int QUICK_SAVE_SLOT = -2;
    static final public int INVALID_SAVE_SLOT = -3;
    static final WorldSave currentSave = new WorldSave();
    public WorldSaveHeader header = new WorldSaveHeader();
    private final AdventurePlayer player = new AdventurePlayer();
    private final World world = new World();
    private final PointOfInterestChanges.Map pointOfInterestChanges = new PointOfInterestChanges.Map();


    private final SignalList onLoadList = new SignalList();
    private static final PlaythroughRepository REPOSITORY = new PlaythroughRepository(
            java.nio.file.Paths.get(ForgeConstants.USER_ADVENTURE_DIR, "campaign-saves"));
    private static volatile PlaythroughSession activePlaythrough;
    private static boolean worldAvailable;
    private static boolean restoring;
    private static String lastError = "";

    private static final class LoadedPayload {
        final WorldSaveHeader header;
        final SaveFileData data;
        LoadedPayload(WorldSaveHeader header, SaveFileData data) {
            this.header = header;
            this.data = data;
        }
    }

    private static long lastPreviewTimestamp = 0L;
    private static final long COOLDOWN_WINDOW_MS = 800L;
    private static boolean firstCapture = true;
    private static final ExecutorService SAVE_EXECUTOR =
        Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "Forge-Adventure-Save");
            thread.setDaemon(true);
            return thread;
        });

    public final World getWorld() {
        return world;
    }

    public AdventurePlayer getPlayer() {
        return player;
    }

    public void onLoad(Runnable run) {
        onLoadList.add(run);
    }

    public PointOfInterestChanges getPointOfInterestChanges(String id) {
        if (id == null) { // fallback
            return new PointOfInterestChanges();
        }

        PointOfInterestChanges changes = pointOfInterestChanges.get(id);
        if (changes == null) {
            changes = new PointOfInterestChanges();
            pointOfInterestChanges.put(id, changes);
        }

        return changes;
    }

    /** Read persisted objective evidence without creating map state or loading map assets. */
    public Integer getStoredMapFlag(String poiRootId, String flagName) {
        return pointOfInterestChanges.getMapFlag(poiRootId, flagName);
    }

    public static String getLastError() { return lastError; }

    public static boolean hasActivePlaythrough() {
        return worldAvailable && activePlaythrough != null && !restoring;
    }

    public static PlaythroughMetadata getCurrentPlaythrough() {
        return activePlaythrough == null ? null : activePlaythrough.metadata.copy();
    }

    public static List<PlaythroughMetadata> listPlaythroughs() { return REPOSITORY.listPlaythroughs(); }
    public static List<SaveSnapshot> listSnapshots(String playthroughId) { return REPOSITORY.listSnapshots(playthroughId); }

    public static void clearLoadListeners() { currentSave.onLoadList.clear(); }

    public static void beginPlaythrough(CampaignDefinition campaign, String runName, String characterName) {
        lastError = "";
        try {
            PlaythroughMetadata metadata = REPOSITORY.createPlaythrough(campaign.getId(), campaign.getVersion(),
                    campaign.getTitle(), campaign.getPlane(), runName, characterName);
            activePlaythrough = new PlaythroughSession(metadata);
            worldAvailable = false;
            firstCapture = true;
        } catch (IOException e) {
            lastError = "Could not create the playthrough: " + e.getMessage();
            throw new IllegalStateException(lastError, e);
        }
    }

    /** Preserve this run before a final New Game/campaign transition. */
    public static boolean checkpointAndSuspend() {
        if (!hasActivePlaythrough()) return true;
        if (TileMapScene.instance().currentMap().isInMap()) {
            lastError = "Return to the overworld before switching playthroughs; saving inside a location is not supported yet.";
            return false;
        }
        if (!currentSave.saveSnapshot("Before switching playthroughs", SaveKind.AUTO)) return false;
        discardFailedPlaythrough();
        return true;
    }

    /** Does not save or delete anything: a failed generated world must not become a checkpoint. */
    public static void discardFailedPlaythrough() {
        activePlaythrough = null;
        worldAvailable = false;
        restoring = false;
    }

    public static void beginNewGamePlus(String runName) {
        if (!hasActivePlaythrough()) throw new IllegalStateException("Load a playthrough before starting New Game+.");
        String character = currentSave.player.getName();
        beginPlaythrough(CampaignManager.current(), runName, character);
        // The caller applies the campaign's reset policy to the already loaded
        // character/world. The source run and all of its snapshots are untouched.
        worldAvailable = true;
    }

    public static boolean saveInitialSnapshot() {
        return currentSave.saveSnapshot("Beginning of adventure", SaveKind.INITIAL);
    }

    public static boolean saveManual(String label) {
        return currentSave.saveSnapshot(label, SaveKind.MANUAL);
    }

    public static boolean replaceManual(SaveSnapshot target, String label) {
        lastError = "";
        if (!hasActivePlaythrough() || target == null || target.kind != SaveKind.MANUAL
                || !activePlaythrough.metadata.playthroughId.equals(target.playthroughId)) {
            lastError = "Only a manual save in the active playthrough can be replaced.";
            return false;
        }
        try {
            // Resolve before writing, so an invalid/stale target cannot create an
            // unexpected replacement. Commit the new save before archiving the old one.
            if (REPOSITORY.resolve(new SaveLocator(target)).kind != SaveKind.MANUAL) {
                throw new IOException("Only a manual checkpoint can be replaced.");
            }
            if (!saveManual(label)) return false;
            REPOSITORY.archiveManual(target);
            return true;
        } catch (IOException e) {
            lastError = "Could not finish replacing the checkpoint: " + e.getMessage();
            return false;
        }
    }

    public static void clearContinue() {
        try { REPOSITORY.clearContinue(); }
        catch (IOException e) { System.err.println("Could not clear Continue: " + e.getMessage()); }
        Config.instance().getSettingData().lastAdventureSave = null;
        Config.instance().getSettingData().lastActiveSave = null;
        Config.instance().saveSettings();
    }

    public static boolean canContinue() {
        try { if (REPOSITORY.getContinue() != null) return true; }
        catch (IOException e) { lastError = e.getMessage(); }
        if (Config.instance().getSettingData().lastAdventureSave != null) return true;
        String filename = Config.instance().getSettingData().lastActiveSave;
        return isSafeFile(filename) && new File(getLegacySaveDir(), filename).isFile();
    }

    public static boolean loadContinue() {
        lastError = "";
        try {
            SaveSnapshot snapshot = REPOSITORY.getContinue();
            if (snapshot != null) return loadSnapshot(snapshot);
            if (Config.instance().getSettingData().lastAdventureSave != null) {
                return loadSnapshot(REPOSITORY.resolve(Config.instance().getSettingData().lastAdventureSave));
            }
            String filename = Config.instance().getSettingData().lastActiveSave;
            if (isSafeFile(filename)) return load(filenameToSlot(filename));
            lastError = "No saved playthrough is available to continue.";
        } catch (IOException e) { lastError = e.getMessage(); }
        return false;
    }

    public static boolean loadSnapshot(SaveSnapshot selection) {
        lastError = "";
        if (hasActivePlaythrough() && TileMapScene.instance().currentMap().isInMap()) {
            lastError = "Return to the overworld before loading another checkpoint; the current interior state cannot yet be restored safely if loading fails.";
            return false;
        }
        LoadedPayload incoming = null;
        LoadedPayload previous = null;
        WorldSaveHeader priorHeader = currentSave.header;
        PlaythroughSession priorSession = activePlaythrough;
        CampaignDefinition priorCampaign = null;
        boolean wasAvailable = worldAvailable;
        boolean activationAttempted = false;
        try {
            priorCampaign = CampaignManager.current();
            SaveSnapshot snapshot = REPOSITORY.resolve(new SaveLocator(selection));
            CampaignDefinition campaign = CampaignRegistry.discover().find(snapshot.campaignId, snapshot.campaignVersion);
            if (campaign == null) {
                throw new IOException("Install campaign " + snapshot.campaignTitle + " (" + snapshot.campaignId
                        + ") version " + snapshot.campaignVersion + " to load this save.");
            }
            Path payload = REPOSITORY.validatedPayload(snapshot);
            incoming = readPayload(payload.toFile());
            validatePayload(incoming.data);
            PlaythroughMetadata targetRun = REPOSITORY.readPlaythrough(snapshot.playthroughId);
            // Validate and capture before replacing any live configuration. The
            // detached old state lets a later resource/scene restoration failure
            // return to the existing run instead of creating a substitute world.
            if (wasAvailable && priorSession != null) previous = new LoadedPayload(currentSave.header, captureLiveData());
            restoring = true;
            activationAttempted = true;
            CampaignManager.activate(campaign);
            restorePayload(incoming);
            activePlaythrough = new PlaythroughSession(targetRun);
            worldAvailable = true;
            Forge.invokeWorldSave = true;
            rememberContinue(snapshot, activePlaythrough, 0);
            if (priorHeader != currentSave.header) priorHeader.dispose();
            return true;
        } catch (Exception e) {
            lastError = "Could not load the playthrough: " + e.getMessage();
            e.printStackTrace();
            if (activationAttempted && priorCampaign != null) {
                try {
                    CampaignManager.activate(priorCampaign);
                    if (previous != null) restorePayload(previous);
                    activePlaythrough = priorSession;
                    worldAvailable = wasAvailable;
                } catch (Exception recoveryError) {
                    recoveryError.printStackTrace();
                    activePlaythrough = null;
                    worldAvailable = false;
                    lastError += " The prior session could not be restored in memory; its saved checkpoints remain intact.";
                }
            }
            currentSave.header = priorHeader;
            if (incoming != null && incoming.header != currentSave.header) incoming.header.dispose();
            return false;
        } finally { restoring = false; }
    }

    /** Compatibility for old hotkeys/slot callers; new UI addresses snapshot identities. */
    static public boolean load(int currentSlot) {
        if (hasActivePlaythrough()) {
            SaveKind kind = currentSlot == QUICK_SAVE_SLOT ? SaveKind.QUICK
                    : currentSlot == AUTO_SAVE_SLOT ? SaveKind.AUTO : SaveKind.MANUAL;
            for (SaveSnapshot snapshot : listSnapshots(activePlaythrough.metadata.playthroughId)) {
                if (snapshot.kind == kind) return loadSnapshot(snapshot);
            }
            lastError = "This playthrough has no matching saved checkpoint.";
            return false;
        }
        CampaignDefinition campaign = CampaignManager.current();
        if (campaign != null) {
            for (LegacySave legacy : listLegacySaves()) {
                if (campaign.getId().equals(legacy.campaignId) && filename(currentSlot).equals(legacy.fileName)) {
                    SaveSnapshot imported = importLegacy(legacy, "Imported " + legacy.fileName);
                    return imported != null && loadSnapshot(imported);
                }
            }
        }
        lastError = "No matching saved checkpoint exists.";
        return false;
    }

    public static List<LegacySave> listLegacySaves() {
        List<LegacySave> result = new ArrayList<>();
        for (CampaignDefinition campaign : CampaignRegistry.discover().getCampaigns()) {
            File directory = legacyDirectory(campaign.getPlane());
            File[] files = directory.listFiles();
            if (files == null) continue;
            for (File file : files) {
                if (!file.isFile() || !isSafeFile(file.getName())) continue;
                LegacySave legacy = new LegacySave();
                legacy.campaignId = campaign.getId();
                legacy.campaignVersion = campaign.getVersion();
                legacy.campaignTitle = campaign.getTitle();
                legacy.plane = campaign.getPlane();
                legacy.fileName = file.getName();
                legacy.modifiedAt = file.lastModified();
                result.add(legacy);
            }
        }
        result.sort(Comparator.comparingLong((LegacySave legacy) -> legacy.modifiedAt).reversed());
        return result;
    }

    public static SaveSnapshot importLegacy(LegacySave legacy, String runName) {
        lastError = "";
        LoadedPayload payload = null;
        try {
            CampaignDefinition campaign = CampaignRegistry.discover().find(legacy.campaignId, legacy.campaignVersion);
            if (campaign == null || !campaign.getPlane().equals(legacy.plane) || !isSafeFile(legacy.fileName)) {
                throw new IOException("The legacy save's campaign is unavailable or its filename is invalid.");
            }
            File source = new File(legacyDirectory(campaign.getPlane()), legacy.fileName);
            payload = readPayload(source);
            validatePayload(payload.data);
            String characterName = payload.data.readSubData("player").readString("name");
            PlaythroughMetadata run = REPOSITORY.createPlaythrough(campaign.getId(), campaign.getVersion(),
                    campaign.getTitle(), campaign.getPlane(), runName, characterName);
            PlaythroughRepository.WriteRequest request = REPOSITORY.prepareSave(run, SaveKind.IMPORTED,
                    payload.header.name == null ? "Imported checkpoint" : payload.header.name);
            // Copy the complete original binary payload; never rename, edit, or
            // delete its source, and never guess relationships between legacy slots.
            return REPOSITORY.writeSnapshot(request,
                    temporary -> Files.copy(source.toPath(), temporary, StandardCopyOption.REPLACE_EXISTING),
                    WorldSave::validateSaveFile);
        } catch (Exception e) {
            lastError = "Could not import the legacy save: " + e.getMessage();
            e.printStackTrace();
            return null;
        } finally { if (payload != null) payload.header.dispose(); }
    }

    private static File legacyDirectory(String plane) {
        return new File(ForgeConstants.USER_ADVENTURE_DIR, plane.replace("<user>", "user_"));
    }

    private static String getLegacySaveDir() { return legacyDirectory(Config.instance().getPlane()).getPath(); }

    private static LoadedPayload readPayload(File file) throws IOException, ClassNotFoundException {
        Object header = null;
        try (FileInputStream stream = new FileInputStream(file);
             InflaterInputStream inflater = new InflaterInputStream(stream);
             ObjectInputStream input = new ObjectInputStream(inflater)) {
            header = input.readObject();
            Object data = input.readObject();
            if (!(header instanceof WorldSaveHeader) || !(data instanceof SaveFileData)) {
                throw new IOException("Invalid save payload.");
            }
            return new LoadedPayload((WorldSaveHeader) header, (SaveFileData) data);
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            if (header instanceof WorldSaveHeader) ((WorldSaveHeader) header).dispose();
            throw e;
        }
    }

    private static void validatePayload(SaveFileData data) throws IOException {
        String error = data.readString("IOException");
        if (error != null) throw new IOException(error);
        for (String section : new String[]{"player", "world", "worldStage", "pointOfInterestChanges"}) {
            SaveFileData sectionData = data.readSubData(section);
            if (sectionData == null) throw new IOException("Missing or damaged " + section + " save data.");
            String sectionError = sectionData.readString("IOException");
            if (sectionError != null) throw new IOException("Damaged " + section + " data: " + sectionError);
        }
    }

    private static void restorePayload(LoadedPayload payload) throws IOException {
        validatePayload(payload.data);
        JSONStringLoader.clearCache();
        CardUtil.clearPriceCache();
        currentSave.player.load(payload.data.readSubData("player"));
        currentSave.world.load(payload.data.readSubData("world"));
        currentSave.pointOfInterestChanges.clear();
        currentSave.pointOfInterestChanges.load(payload.data.readSubData("pointOfInterestChanges"));
        WorldStage.getInstance().load(payload.data.readSubData("worldStage"));
        currentSave.header = payload.header;
        GamePlayerUtil.getGuiPlayer().setName(currentSave.player.getName());
        GameHUD.getInstance();
        currentSave.onLoadList.emit();
        try { AdventureQuestController.instance().activateNextStages(); }
        catch (RuntimeException e) {
            System.err.println("Unable to reconcile saved quest progress; restored world retained.");
            e.printStackTrace();
        }
    }

    private static SaveFileData captureLiveData() throws IOException {
        SaveFileData player = currentSave.player.save();
        SaveFileData world = currentSave.world.save();
        SaveFileData stage = WorldStage.getInstance().save();
        SaveFileData changes = currentSave.pointOfInterestChanges.save();
        String errors = currentSave.getExceptionMessage(player, world, stage, changes);
        if (!errors.isEmpty()) throw new IOException(errors);
        SaveFileData data = new SaveFileData();
        data.store("player", player);
        data.store("world", world);
        data.store("worldStage", stage);
        data.store("pointOfInterestChanges", changes);
        String error = data.readString("IOException");
        if (error != null) throw new IOException(error);
        return data;
    }

    public static boolean isSafeFile(String name) {
        return filenameToSlot(name) != INVALID_SAVE_SLOT;
    }

    static public int filenameToSlot(String name) {
        if ("auto_save.sav".equals(name)) return AUTO_SAVE_SLOT;
        if ("quick_save.sav".equals(name)) return QUICK_SAVE_SLOT;
        if (name == null || !name.matches("[1-9][0-9]*_save_slot\\.sav")) return INVALID_SAVE_SLOT;
        try { return Integer.parseInt(name.substring(0, name.indexOf('_'))); }
        catch (NumberFormatException e) { return INVALID_SAVE_SLOT; }
    }

    static public String filename(int slot) {
        if (slot == AUTO_SAVE_SLOT) return "auto_save.sav";
        if (slot == QUICK_SAVE_SLOT) return "quick_save.sav";
        return slot + "_save_slot.sav";
    }

    /** Legacy location only; the new browser uses repository metadata across all campaigns. */
    public static String getSaveDir() { return getLegacySaveDir(); }
    public static String getSaveFile(int slot) { return new File(getLegacySaveDir(), filename(slot)).getPath(); }

    public static WorldSave getCurrentSave() {
        return currentSave;
    }

    public static WorldSave generateNewWorld(String name, boolean male, int race, int avatarIndex, ColorSet startingColorIdentity, DifficultyData diff, AdventureModes mode, int customDeckIndex, CardEdition starterEdition, long seed) {
        if (activePlaythrough == null) beginPlaythrough(CampaignManager.current(), name, name);
        worldAvailable = false;
        Forge.getLocalizer().loadAdventureBundle(Config.instance().getPlanePath(Config.instance().getSettingData().plane) + "languages/");
        if (!currentSave.world.generateNew(seed)) throw new IllegalStateException("The selected campaign could not generate its world.");
        currentSave.pointOfInterestChanges.clear();
        Deck starterDeck = Config.instance().starterDeck(startingColorIdentity, diff, mode, customDeckIndex, starterEdition);
        currentSave.player.create(name, starterDeck, male, race, avatarIndex, mode == AdventureModes.Chaos, mode == AdventureModes.Custom, diff, mode);
        currentSave.player.setWorldPosY((int) (currentSave.world.getData().playerStartPosY * currentSave.world.getData().height * currentSave.world.getTileSize()));
        currentSave.player.setWorldPosX((int) (currentSave.world.getData().playerStartPosX * currentSave.world.getData().width * currentSave.world.getTileSize()));
        WorldSaveHeader priorHeader = currentSave.header;
        currentSave.header = new WorldSaveHeader();
        priorHeader.dispose();
        GameHUD.getInstance();
        currentSave.onLoadList.emit();
        worldAvailable = true;
        return currentSave;
    }

    public boolean autoSave() { return saveSnapshot("Autosave", SaveKind.AUTO); }
    public boolean quickSave() { return saveSnapshot("Quicksave", SaveKind.QUICK); }

    public boolean quickLoad() {
        if (!hasActivePlaythrough()) return false;
        for (SaveSnapshot snapshot : listSnapshots(activePlaythrough.metadata.playthroughId)) {
            if (snapshot.kind == SaveKind.QUICK) return loadSnapshot(snapshot);
        }
        lastError = "This playthrough has no quicksave yet.";
        return false;
    }

    public boolean save(String text, int currentSlot) {
        return saveSnapshot(text, currentSlot == AUTO_SAVE_SLOT ? SaveKind.AUTO
                : currentSlot == QUICK_SAVE_SLOT ? SaveKind.QUICK : SaveKind.MANUAL);
    }

    private boolean saveSnapshot(String text, SaveKind kind) {
        lastError = "";
        if (!hasActivePlaythrough()) {
            lastError = "There is no complete active playthrough to save.";
            return false;
        }
        if (TileMapScene.instance().currentMap().isInMap()) {
            lastError = "Return to the overworld before saving; exact interior-map restoration is not supported yet.";
            return false;
        }
        PlaythroughSession session = activePlaythrough;
        long sequence = session.nextRequestSequence();
        try {
            // Initial saves precede entering the world stage, whose sprite has
            // not yet loaded the character's coordinates for location lookup.
            String location = kind == SaveKind.INITIAL ? "Campaign opening" : currentLocation();
            PlaythroughRepository.WriteRequest request = REPOSITORY.prepareSave(session.metadata, kind, text, location);
            header.name = text;
            header.saveDate = new Date();
            WorldSaveHeader capturedHeader = header.snapshotForAsyncSave();
            SaveFileData capturedData = captureLiveData();
            SaveSnapshot saved = session.writeSnapshot(REPOSITORY, request,
                    temporary -> writePayload(temporary, capturedHeader, capturedData), WorldSave::validateSaveFile, sequence);
            rememberContinue(saved, session, sequence);
            return true;
        } catch (Exception e) {
            lastError = "Could not save the playthrough: " + e.getMessage();
            e.printStackTrace();
            finish(lastError);
            return false;
        }
    }

    private static void writePayload(Path target, WorldSaveHeader saveHeader, SaveFileData data) throws IOException {
        try (FileOutputStream file = new FileOutputStream(target.toFile());
             DeflaterOutputStream deflater = new DeflaterOutputStream(file);
             ObjectOutputStream output = new ObjectOutputStream(deflater)) {
            output.writeObject(saveHeader);
            output.writeObject(data);
            output.flush();
            deflater.finish();
            file.getFD().sync();
        }
    }

    private static void validateSaveFile(Path file) throws IOException {
        LoadedPayload payload = null;
        try {
            payload = readPayload(file.toFile());
            validatePayload(payload.data);
        } catch (ClassNotFoundException e) { throw new IOException("Could not read the save payload.", e); }
        finally { if (payload != null) payload.header.dispose(); }
    }

    private static void rememberContinue(SaveSnapshot snapshot, PlaythroughSession capturedSession, long sequence) throws IOException {
        // A queued save owns its run forever, but only the still-current session
        // may change the user's Continue selection.
        if (activePlaythrough != capturedSession) return;
        if (!capturedSession.publishContinue(REPOSITORY, snapshot, sequence)) return;
        Config.instance().getSettingData().lastAdventureSave = new SaveLocator(snapshot);
        Config.instance().saveSettings();
    }

    private static String currentLocation() {
        try { return GameScene.instance().getAdventurePlayerLocation(true, true); }
        catch (RuntimeException e) { return "Overworld"; }
    }

    public boolean autoSaveAsync() {
        if (!hasActivePlaythrough()) return false;
        final PlaythroughSession capturedSession = activePlaythrough;
        final long sequence = capturedSession.nextRequestSequence();
        final PlaythroughRepository.WriteRequest request;
        final WorldSaveHeader saveHeader;
        final SaveFileData playerData;
        final SaveFileData worldStageData;
        final SaveFileData poiData;
        final World.AsyncSaveSnapshot worldSnapshot;
        World.AsyncSaveSnapshot capturedWorld = null;
        try {
            if (TileMapScene.instance().currentMap().isInMap()) return false;
            final String text = "Autosave";
            request = REPOSITORY.prepareSave(capturedSession.metadata, SaveKind.AUTO, text, currentLocation());
            header.name = text;
            playerData = currentSave.player.save();
            capturedWorld = currentSave.world.snapshotForAsyncSave();
            worldStageData = WorldStage.getInstance().save();
            poiData = currentSave.pointOfInterestChanges.save();
            String message = getExceptionMessage(playerData, worldStageData, poiData);
            if (!message.isEmpty()) throw new IOException(message);
            header.saveDate = new Date();
            saveHeader = header.snapshotForAsyncSave();
            worldSnapshot = capturedWorld;
        } catch (Exception e) {
            if (capturedWorld != null) capturedWorld.dispose();
            lastError = "Could not capture the autosave: " + e.getMessage();
            e.printStackTrace();
            finish(lastError);
            return false;
        }
        try {
            SAVE_EXECUTOR.execute(() -> {
                SaveSnapshot saved = null;
                String error = null;
                try {
                    SaveFileData worldData = worldSnapshot.encode();
                    String message = getExceptionMessage(playerData, worldData, worldStageData, poiData);
                    if (!message.isEmpty()) throw new IOException(message);
                    SaveFileData data = new SaveFileData();
                    data.store("player", playerData);
                    data.store("world", worldData);
                    data.store("worldStage", worldStageData);
                    data.store("pointOfInterestChanges", poiData);
                    if (data.readString("IOException") != null) throw new IOException("Could not assemble save data.");
                    saved = capturedSession.writeSnapshot(REPOSITORY, request,
                            temporary -> writePayload(temporary, saveHeader, data), WorldSave::validateSaveFile, sequence);
                } catch (Exception e) {
                    e.printStackTrace();
                    error = "Autosave failed: " + e.getMessage();
                } finally { worldSnapshot.dispose(); }
                final SaveSnapshot completed = saved;
                final String result = error;
                Gdx.app.postRunnable(() -> {
                    if (activePlaythrough != capturedSession) return;
                    if (result == null) {
                        try { rememberContinue(completed, capturedSession, sequence); }
                        catch (IOException e) { lastError = e.getMessage(); finish(lastError); }
                    } else { lastError = result; finish(result); }
                });
            });
        } catch (RuntimeException e) {
            worldSnapshot.dispose();
            lastError = "Could not queue the autosave: " + e.getMessage();
            finish(lastError);
            return false;
        }
        return true;
    }

    private void finish(String errors) {
        if (errors != null)
            announceError(errors);
    }

    public void restoreBackup(String oldFilename, String currentFilename) {
        File f = new File(currentFilename);
        if (f.exists())
            f.delete();
        File b = new File(oldFilename);
        if (b.exists())
            b.renameTo(new File(currentFilename));
    }

    public String getExceptionMessage(SaveFileData... datas) {
        StringBuilder message = new StringBuilder();

        for (SaveFileData data : datas) {
          String s = data.readString("IOException");
          if (s != null)
              message.append(s).append("\n");
        }

        return message.toString();
    }

    private void announceError(String message) {
        currentSave.player.getCurrentGameStage().setExtraAnnouncement("Error Saving File!\n" + message);
    }

    public void clearChanges() {
        pointOfInterestChanges.clear();
    }

    public void clearBookmarks() {
        for (PointOfInterest poi : currentSave.world.getAllPointOfInterest()) {
            if (poi == null)
                continue;
            PointOfInterestMapSprite mapSprite = WorldStage.getInstance().getMapSprite(poi);
            if (mapSprite != null)
                mapSprite.setBookmarked(false, poi);
            PointOfInterestChanges p = pointOfInterestChanges.get(poi.getID());
            if (p == null)
                continue;
            p.setIsBookmarked(false);
            p.save();
        }
        MapViewScene.instance().clearBookMarks();
    }

    // prevent spam of preview if user repeatedly/accidentally reopen scene that request previews
    public static void requestPreview() {
        final long currentTimestamp = TimeUtils.millis();

        if (firstCapture) {
            firstCapture = false;
            // init once
            currentSave.header.createPreview();
            return;
        }

        // If 800ms has not passed since the last successful generation, skip
        if (currentTimestamp - lastPreviewTimestamp < COOLDOWN_WINDOW_MS) {
            return;
        }

        // Update the timestamp immediately to lock out parallel thread spam on the spot
        lastPreviewTimestamp = currentTimestamp;
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                currentSave.header.createPreview();
            }
        });
    }

    public static void dispose() {
        discardFailedPlaythrough();
        Forge.safeDispose(currentSave.world);
    }
}
