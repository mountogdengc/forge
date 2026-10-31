package forge.adventure.world;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.testng.Assert.*;

/** Exercises persistent isolation and failure boundaries without requiring native game graphics. */
public class PlaythroughRepositoryTest {
    private Path root;
    private PlaythroughRepository repository;

    @BeforeMethod
    public void setup() throws IOException {
        root = Files.createTempDirectory("forge-playthrough-test-");
        repository = new PlaythroughRepository(root);
    }

    @AfterMethod
    public void cleanup() throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
        }
    }

    private PlaythroughMetadata run(String campaign) throws IOException {
        return repository.createPlaythrough(campaign, "1.0", campaign, "Shared plane", "Same run name", "Same character");
    }

    private SaveSnapshot save(PlaythroughMetadata run, SaveKind kind, String state) throws IOException {
        return write(repository.prepareSave(run, kind, state, "Town"), state);
    }

    private SaveSnapshot write(PlaythroughRepository.WriteRequest request, String state) throws IOException {
        return repository.writeSnapshot(request,
                path -> Files.write(path, state.getBytes(StandardCharsets.UTF_8)),
                path -> { if (Files.size(path) == 0) throw new IOException("empty payload"); });
    }

    private String state(SaveSnapshot snapshot) throws IOException {
        return new String(Files.readAllBytes(repository.validatedPayload(snapshot)), StandardCharsets.UTF_8);
    }

    @Test
    public void twoCampaignsAndThreeSameNamedRunsKeepIndependentStateAndCheckpoints() throws IOException {
        PlaythroughMetadata a1 = run("campaign.a");
        PlaythroughMetadata a2 = run("campaign.a");
        PlaythroughMetadata b1 = run("campaign.b");
        assertNotEquals(a1.playthroughId, a2.playthroughId);
        List<SaveSnapshot> all = new ArrayList<>();
        for (PlaythroughMetadata run : new PlaythroughMetadata[]{a1, a2, b1}) {
            all.add(save(run, SaveKind.INITIAL, run.playthroughId + ":start"));
            all.add(save(run, SaveKind.MANUAL, run.playthroughId + ":quest-choice-left"));
            all.add(save(run, SaveKind.MANUAL, run.playthroughId + ":quest-choice-right"));
            all.add(save(run, SaveKind.AUTO, run.playthroughId + ":auto"));
            all.add(save(run, SaveKind.QUICK, run.playthroughId + ":quick"));
            assertEquals(repository.listSnapshots(run.playthroughId).size(), 5);
        }
        repository.setContinue(all.get(all.size() - 1));
        repository = new PlaythroughRepository(root); // Application restart.
        assertEquals(repository.listPlaythroughs().size(), 3);
        assertEquals(repository.getContinue().playthroughId, b1.playthroughId);
        for (SaveSnapshot snapshot : all) {
            assertEquals(state(snapshot), snapshot.label);
            assertEquals(snapshot.location, "Town");
            assertEquals(snapshot.campaignVersion, "1.0");
            assertTrue(snapshot.label.startsWith(snapshot.playthroughId));
        }
    }

    @Test
    public void queuedWriteKeepsItsCapturedDestinationAfterAnotherRunIsSelected() throws Exception {
        PlaythroughMetadata a = run("campaign.a");
        PlaythroughMetadata b = run("campaign.b");
        PlaythroughRepository.WriteRequest queued = repository.prepareSave(a, SaveKind.AUTO, "A captured");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<SaveSnapshot> future = worker.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return write(queued, "A state");
            });
            SaveSnapshot bSave = save(b, SaveKind.MANUAL, "B state");
            repository.setContinue(bSave);
            // Neither caller-owned metadata nor an inspection copy can redirect
            // the already queued request's run or destination.
            a.playthroughId = b.playthroughId;
            a.campaignId = b.campaignId;
            SaveSnapshot view = queued.getSnapshot();
            view.playthroughId = b.playthroughId;
            view.campaignId = b.campaignId;
            start.countDown();
            SaveSnapshot aSave = future.get(10, TimeUnit.SECONDS);
            assertNotEquals(aSave.playthroughId, b.playthroughId);
            assertEquals(aSave.campaignId, "campaign.a");
            assertEquals(state(aSave), "A state");
            assertEquals(state(bSave), "B state");
            // Writing a captured run never changes Continue by itself; the UI
            // completion path must explicitly publish it for the active session.
            assertEquals(repository.getContinue().snapshotId, bSave.snapshotId);
        } finally { start.countDown(); worker.shutdownNow(); }
    }

    @Test
    public void olderAutosaveCompletionCannotReplaceANewerManualContinueInTheSameSession() throws Exception {
        PlaythroughMetadata run = run("campaign.a");
        PlaythroughSession session = new PlaythroughSession(run);
        long autoSequence = session.nextRequestSequence();
        PlaythroughRepository.WriteRequest queued = repository.prepareSave(run, SaveKind.AUTO, "older state");
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<SaveSnapshot> pending = worker.submit(() -> {
                assertTrue(release.await(10, TimeUnit.SECONDS));
                return write(queued, "older state");
            });
            long manualSequence = session.nextRequestSequence();
            SaveSnapshot newer = save(run, SaveKind.MANUAL, "newer decision");
            assertTrue(session.publishContinue(repository, newer, manualSequence));
            release.countDown();
            SaveSnapshot older = pending.get(10, TimeUnit.SECONDS);
            assertFalse(session.publishContinue(repository, older, autoSequence));
            assertEquals(repository.getContinue().snapshotId, newer.snapshotId);
            assertEquals(state(older), "older state");
            assertEquals(state(repository.getContinue()), "newer decision");
        } finally { release.countDown(); worker.shutdownNow(); }
    }

    @Test
    public void delayedCallbacksSkipOlderAutosavesAlreadyPrunedByNewerSuccessfulWrites() throws IOException {
        PlaythroughMetadata run = run("campaign.a");
        PlaythroughSession session = new PlaythroughSession(run);
        List<SaveSnapshot> completed = new ArrayList<>();
        List<Long> sequences = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            long sequence = session.nextRequestSequence();
            sequences.add(sequence);
            PlaythroughRepository.WriteRequest request = repository.prepareSave(run, SaveKind.AUTO, "queued " + i);
            completed.add(session.writeSnapshot(repository, request,
                    path -> Files.write(path, new byte[]{1}), path -> { }, sequence));
        }
        for (int i = 0; i < completed.size() - 1; i++) {
            assertFalse(session.publishContinue(repository, completed.get(i), sequences.get(i)));
        }
        int newest = completed.size() - 1;
        assertTrue(session.publishContinue(repository, completed.get(newest), sequences.get(newest)));
        assertEquals(repository.getContinue().snapshotId, completed.get(newest).snapshotId);
    }

    @Test
    public void failedWritesAndValidationKeepThePreviousSaveAndContinue() throws IOException {
        PlaythroughMetadata run = run("campaign.a");
        SaveSnapshot previous = save(run, SaveKind.AUTO, "known good");
        repository.setContinue(previous);
        PlaythroughRepository.WriteRequest interrupted = repository.prepareSave(run, SaveKind.AUTO, "partial");
        expectThrows(IOException.class, () -> repository.writeSnapshot(interrupted, path -> {
            Files.write(path, new byte[]{1, 2});
            throw new IOException("simulated interrupted write");
        }, path -> { }));
        PlaythroughRepository.WriteRequest invalid = repository.prepareSave(run, SaveKind.AUTO, "invalid");
        expectThrows(IOException.class, () -> repository.writeSnapshot(invalid,
                path -> Files.write(path, new byte[]{1}),
                path -> { throw new IOException("simulated invalid payload"); }));
        assertEquals(repository.listSnapshots(run.playthroughId).size(), 1);
        assertEquals(state(previous), "known good");
        assertEquals(repository.getContinue().snapshotId, previous.snapshotId);
        try (Stream<Path> files = Files.walk(root)) {
            assertFalse(files.anyMatch(path -> path.toString().endsWith(".tmp")));
        }
    }

    @Test
    public void checksumAndCampaignIdentityRejectWrongOrDamagedPayloads() throws IOException {
        PlaythroughMetadata a = run("campaign.a");
        PlaythroughMetadata b = run("campaign.b");
        SaveSnapshot snapshot = save(a, SaveKind.MANUAL, "A state");
        SaveLocator wrongCampaign = new SaveLocator(snapshot);
        wrongCampaign.campaignId = b.campaignId;
        expectThrows(IOException.class, () -> repository.resolve(wrongCampaign));
        SaveLocator traversal = new SaveLocator(snapshot);
        traversal.playthroughId = "../../other";
        expectThrows(IOException.class, () -> repository.resolve(traversal));
        Path payload = repository.validatedPayload(snapshot);
        Files.write(payload, "damaged".getBytes(StandardCharsets.UTF_8));
        expectThrows(IOException.class, () -> repository.validatedPayload(snapshot));
        assertEquals(repository.listSnapshots(a.playthroughId).size(), 1); // Remains discoverable for diagnosis.
    }

    @Test
    public void automaticRetentionIsPerRunAndNeverPrunesManualCheckpoints() throws IOException {
        PlaythroughMetadata a = run("campaign.a");
        PlaythroughMetadata b = run("campaign.a");
        SaveSnapshot bAuto = save(b, SaveKind.AUTO, "other run");
        for (int i = 0; i < 14; i++) save(a, SaveKind.MANUAL, "manual " + i);
        for (int i = 0; i < 6; i++) {
            save(a, SaveKind.AUTO, "auto " + i);
            save(a, SaveKind.QUICK, "quick " + i);
        }
        List<SaveSnapshot> snapshots = repository.listSnapshots(a.playthroughId);
        assertEquals(snapshots.stream().filter(save -> save.kind == SaveKind.MANUAL).count(), 14L);
        assertEquals(snapshots.stream().filter(save -> save.kind == SaveKind.AUTO).count(), 3L);
        assertEquals(snapshots.stream().filter(save -> save.kind == SaveKind.QUICK).count(), 3L);
        assertEquals(repository.listSnapshots(b.playthroughId).size(), 1);
        assertEquals(state(bAuto), "other run");
    }

    @Test
    public void continueRecoveryPointSurvivesUntilBackgroundCompletionPublishesANewOne() throws IOException {
        PlaythroughMetadata run = run("campaign.a");
        SaveSnapshot first = save(run, SaveKind.AUTO, "resume me");
        repository.setContinue(first);
        for (int i = 0; i < 5; i++) save(run, SaveKind.AUTO, "queued " + i);
        assertEquals(state(repository.getContinue()), "resume me");
        assertTrue(repository.listSnapshots(run.playthroughId).stream().anyMatch(save -> save.snapshotId.equals(first.snapshotId)));
    }

    @Test
    public void manualReplacementKeepsNewSaveVisibleAndOldPayloadRecoverable() throws IOException {
        PlaythroughMetadata run = run("campaign.a");
        SaveSnapshot original = save(run, SaveKind.MANUAL, "before choice");
        Path recovery = repository.validatedPayload(original);
        SaveSnapshot replacement = save(run, SaveKind.MANUAL, "after choice");
        repository.setContinue(replacement);
        repository.archiveManual(original);
        assertEquals(repository.listSnapshots(run.playthroughId).size(), 1);
        assertEquals(state(repository.getContinue()), "after choice");
        assertTrue(Files.isRegularFile(recovery));
        SaveSnapshot automatic = save(run, SaveKind.AUTO, "automatic");
        expectThrows(IOException.class, () -> repository.archiveManual(automatic));
    }

    @Test
    public void legacyImportCopiesPayloadWithoutGroupingOrChangingOriginals() throws IOException {
        Path legacy = root.resolve("legacy-slot.sav");
        byte[] source = "legacy payload".getBytes(StandardCharsets.UTF_8);
        Files.write(legacy, source);
        for (int i = 0; i < 2; i++) {
            PlaythroughMetadata importedRun = run("legacy.campaign");
            repository.writeSnapshot(repository.prepareSave(importedRun, SaveKind.IMPORTED, "Imported slot"),
                    target -> Files.copy(legacy, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING), path -> { });
        }
        assertEquals(repository.listPlaythroughs().size(), 2);
        assertEquals(Files.readAllBytes(legacy), source);
        for (PlaythroughMetadata run : repository.listPlaythroughs()) {
            assertEquals(state(repository.listSnapshots(run.playthroughId).get(0)), "legacy payload");
        }
    }

    @Test
    public void renamedLabelsDoNotChangeIdentityAndUnknownVersionsDoNotLoad() throws IOException {
        PlaythroughMetadata run = run("campaign.a");
        SaveSnapshot snapshot = save(run, SaveKind.MANUAL, "checkpoint");
        run.runName = "Renamed in UI";
        run.campaignTitle = "Different title";
        assertEquals(state(snapshot), "checkpoint");
        SaveLocator incompatible = new SaveLocator(snapshot);
        incompatible.campaignVersion = "2.0";
        expectThrows(IOException.class, () -> repository.resolve(incompatible));
        Path metadata = root.resolve("playthroughs").resolve(run.playthroughId).resolve("playthrough.json");
        String json = new String(Files.readAllBytes(metadata), StandardCharsets.UTF_8);
        json = json.replaceFirst("\"formatVersion\"\\s*:\\s*1", "\"formatVersion\":99");
        Files.write(metadata, json.getBytes(StandardCharsets.UTF_8));
        expectThrows(IOException.class, () -> repository.resolve(new SaveLocator(snapshot)));
    }
}
