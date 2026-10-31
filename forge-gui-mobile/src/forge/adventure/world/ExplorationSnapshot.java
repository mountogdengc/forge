package forge.adventure.world;

import forge.adventure.util.SaveFileData;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Detached gameplay values only; scenes, native resources and callbacks are rebuilt on load. */
public final class ExplorationSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    public int version = 1;
    public boolean interior;
    public String poiId = "";
    public String map = "";
    public String sourceMap = "";
    public float playerX;
    public float playerY;
    public String facing = "None";
    public Random random;
    public List<ActorState> actors = new ArrayList<>();

    public static final class MovementState implements Serializable {
        private static final long serialVersionUID = 1L;
        public String destination = "";
        public float duration;
        public boolean hasTarget;
        public float targetX;
        public float targetY;
    }

    public static final class ActorState implements Serializable {
        private static final long serialVersionUID = 1L;
        public int id;
        public float x;
        public float y;
        public String facing = "None";
        public boolean frozen;
        public boolean aggro;
        public boolean hidden;
        public boolean inactive;
        public float movementTimer;
        public List<MovementState> movements = new ArrayList<>();
    }

    public SaveFileData toSaveData() throws IOException {
        validate();
        SaveFileData data = new SaveFileData();
        // storeObject serializes now, so later live mutation cannot change a queued save.
        data.storeObject("snapshot", this);
        if (data.containsKey("IOException")) throw new IOException("Could not capture exploration state.");
        return data;
    }

    public static ExplorationSnapshot fromSaveData(SaveFileData data) throws IOException {
        if (data == null || !data.containsKey("snapshot") || data.containsKey("IOException")) {
            throw new IOException("Missing or damaged exploration state.");
        }
        Object value = data.readObject("snapshot");
        if (!(value instanceof ExplorationSnapshot)) throw new IOException("Invalid exploration state.");
        ExplorationSnapshot state = (ExplorationSnapshot) value;
        state.validate();
        return state;
    }

    private void validate() throws IOException {
        if (version != 1) throw new IOException("Unsupported exploration snapshot version.");
        requirePosition(playerX, playerY);
        requireFacing(facing);
        if (actors == null) throw new IOException("Missing actor state.");
        if (interior) {
            if (poiId == null || poiId.isEmpty()) throw new IOException("Missing interior location.");
            requireMap(map, false);
            requireMap(sourceMap, true);
        } else if (!actors.isEmpty()) {
            throw new IOException("Interior actors cannot belong to an overworld snapshot.");
        }
        Set<Integer> ids = new HashSet<>();
        for (ActorState actor : actors) {
            if (actor == null || !ids.add(actor.id)) throw new IOException("Missing or duplicate map actor.");
            requirePosition(actor.x, actor.y);
            requireFacing(actor.facing);
            if (!Float.isFinite(actor.movementTimer) || actor.movementTimer < 0 || actor.movements == null) {
                throw new IOException("Invalid actor movement state.");
            }
            for (MovementState movement : actor.movements) {
                if (movement == null || movement.destination == null || !Float.isFinite(movement.duration)
                        || movement.duration < 0) throw new IOException("Invalid movement behavior.");
                requirePosition(movement.targetX, movement.targetY);
            }
        }
    }

    private static void requirePosition(float x, float y) throws IOException {
        if (!Float.isFinite(x) || !Float.isFinite(y)) throw new IOException("Invalid exploration coordinates.");
    }

    private static void requireMap(String map, boolean optional) throws IOException {
        if (map == null || (!optional && map.isEmpty()) || map.indexOf('\0') >= 0
                || map.startsWith("/") || map.startsWith("\\") || map.contains(":")) {
            throw new IOException("Invalid map resource identity.");
        }
    }

    private static void requireFacing(String facing) throws IOException {
        if (facing == null || !facing.matches("None|Left|Right|Up|Down|LeftUp|LeftDown|RightUp|RightDown")) {
            throw new IOException("Invalid actor facing.");
        }
    }
}
