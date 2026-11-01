package forge.sound;

import forge.util.MyRandom;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

public enum MusicPlaylist {
    BLACK       ("black/"),
    BLUE        ("blue/"),
    RED         ("red/"),
    GREEN       ("green/"),
    WHITE       ("white/"),
    COLORLESS   ("colorless/"),
    CASTLE      ("castle/"),
    CAVE        ("cave/"),
    TOWN        ("town/"),
    BOSS        ("boss/"),
    MENUS       ("menus/"),
    MATCH       ("match/");

    private final String subDir;
    private int mostRecentTrackIdx = -1;
    private File[] filenames;
    private static final AtomicLong resourceGeneration = new AtomicLong();
    private long loadedGeneration = -1;

    MusicPlaylist(String subDir0) {
        subDir = subDir0;
    }

    public static void invalidateMusicPlaylist() {
        resourceGeneration.incrementAndGet();
    }

    public String getSubDir() {
        return this.subDir;
    }

    public synchronized String getRandomFilename() {
        // Each playlist must observe a campaign/profile change independently.
        // One shared boolean was consumed by the first playlist that refreshed.
        long generation = resourceGeneration.get();
        if (filenames == null || loadedGeneration != generation) {
            try {
                File musicDir = SoundSystem.findMusicDirectory(this);
                if(musicDir != null)
                    filenames = musicDir.listFiles(SoundSystem.PLAYABLE_AUDIO);
                else
                    filenames = new File[0];

                if (filenames == null)
                    filenames = new File[0];
            }
            catch (Exception e) {
                e.printStackTrace();
                filenames = new File[0];
            }
            loadedGeneration = generation;
            mostRecentTrackIdx = -1;
        }

        if (filenames.length == 0) { return null; }
        else if (filenames.length == 1) {
            mostRecentTrackIdx = 0;
        }
        else {
            int index = MyRandom.getRandom().nextInt(filenames.length - 1);
            if(index >= mostRecentTrackIdx)
                index += 1;
            mostRecentTrackIdx = index;
        }

        return filenames[mostRecentTrackIdx].getPath();
    }
}
