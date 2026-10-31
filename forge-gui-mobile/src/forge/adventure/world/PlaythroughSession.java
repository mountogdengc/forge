package forge.adventure.world;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/** Request ordering for one activation of a run; loading the same run creates a new session. */
final class PlaythroughSession {
    final PlaythroughMetadata metadata;
    private final AtomicLong requestedSequence = new AtomicLong();
    private long committedSequence = -1;
    private long publishedSequence = -1;

    PlaythroughSession(PlaythroughMetadata metadata) {
        this.metadata = metadata.copy();
    }

    long nextRequestSequence() { return requestedSequence.incrementAndGet(); }

    synchronized SaveSnapshot writeSnapshot(PlaythroughRepository repository,
            PlaythroughRepository.WriteRequest request, PlaythroughRepository.SnapshotWriter writer,
            PlaythroughRepository.SnapshotValidator validator, long sequence) throws IOException {
        SaveSnapshot snapshot = repository.writeSnapshot(request, writer, validator);
        committedSequence = Math.max(committedSequence, sequence);
        return snapshot;
    }

    synchronized boolean publishContinue(PlaythroughRepository repository, SaveSnapshot snapshot, long sequence)
            throws IOException {
        // Several automatic writes can finish before the render thread runs
        // their callbacks. Retention may already have removed an older one;
        // only the newest successful request is eligible to become Continue.
        if (sequence < publishedSequence || sequence < committedSequence) return false;
        repository.setContinue(snapshot);
        publishedSequence = sequence;
        return true;
    }
}
