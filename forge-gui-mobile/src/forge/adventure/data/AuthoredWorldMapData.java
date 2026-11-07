package forge.adventure.data;

import java.util.HashSet;
import java.util.Set;

/** Campaign-owned tile rows use top-left coordinates, like the saved world arrays. */
public class AuthoredWorldMapData {
    public int width;
    public int height;
    public String[] rows = new String[0];
    public Palette[] palette = new Palette[0];
    public Location[] locations = new Location[0];
    public Road[] roads = new Road[0];

    public static class Palette {
        public String symbol;
        public int biome;
        public boolean collision;
    }
    public static class Location {
        public String name;
        public int x;
        public int y;
    }
    public static class Road {
        public int radius = 1;
        public int[][] points;
    }

    public void validate(int expectedWidth, int expectedHeight, int biomeCount) {
        require(width == expectedWidth && height == expectedHeight && width > 0 && height > 0, "world dimensions differ");
        require(biomeCount > 0 && biomeCount < 63, "invalid biome count");
        require(rows != null && rows.length == height && palette != null && palette.length > 0, "missing tile rows/palette");
        Set<Character> symbols = new HashSet<>();
        for (Palette cell : palette) {
            require(cell != null && cell.symbol != null && cell.symbol.length() == 1, "invalid palette symbol");
            require(symbols.add(cell.symbol.charAt(0)), "duplicate palette symbol");
            require(cell.biome >= 0 && cell.biome < biomeCount, "unknown biome index");
        }
        for (String row : rows) {
            require(row != null && row.length() == width, "invalid tile row width");
            for (int x = 0; x < width; x++) require(symbols.contains(row.charAt(x)), "unknown tile symbol");
        }
        require(locations != null && locations.length > 0 && roads != null, "missing locations/roads");
        Set<String> names = new HashSet<>();
        for (Location anchor : locations) {
            require(anchor != null && anchor.name != null && !anchor.name.isBlank() && names.add(anchor.name), "duplicate/invalid location");
            require(inside(anchor.x, anchor.y) && anchor.x > 0 && anchor.y > 0 &&
                    anchor.x < width - 1 && anchor.y < height - 1 && !cell(anchor.x, anchor.y).collision,
                    "location outside walkable interior terrain");
            for (Location other : locations) if (other != anchor && other != null)
                require(Math.abs(anchor.x - other.x) >= 8 || Math.abs(anchor.y - other.y) >= 8,
                        "location footprints overlap: " + anchor.name);
        }
        for (Road road : roads) {
            require(road != null && road.radius >= 1 && road.radius <= 3 && road.points != null && road.points.length >= 2, "invalid road");
            for (int[] point : road.points) require(point != null && point.length == 2 && inside(point[0], point[1]), "road point out of bounds");
        }
        validateConnections(biomeCount);
    }

    private void validateConnections(int roadBiome) {
        long[][] biomes = new long[width][height]; int[][] terrain = new int[width][height];
        paint(biomes, terrain); paintRoads(biomes, terrain, roadBiome);
        boolean[][] reached = new boolean[width][height];
        java.util.ArrayDeque<Integer> pending = new java.util.ArrayDeque<>();
        Location first = locations[0]; reached[first.x][first.y] = true; pending.add(first.x + first.y * width);
        int[][] steps = {{-1,0},{1,0},{0,-1},{0,1}};
        while (!pending.isEmpty()) {
            int cell = pending.remove(), x = cell % width, y = cell / width;
            for (int[] step : steps) {
                int px = x + step[0], py = y + step[1];
                if (inside(px, py) && !reached[px][py] && (terrain[px][py] & Integer.MIN_VALUE) == 0) {
                    reached[px][py] = true; pending.add(px + py * width);
                }
            }
        }
        for (Location anchor : locations) require(reached[anchor.x][anchor.y], "disconnected location " + anchor.name);
    }

    private boolean inside(int x, int y) { return x >= 0 && y >= 0 && x < width && y < height; }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException("Authored world: " + message);
    }
    private Palette cell(int x, int y) {
        char symbol = rows[y].charAt(x);
        for (Palette cell : palette) if (cell.symbol.charAt(0) == symbol) return cell;
        throw new IllegalArgumentException("Authored world: unknown tile " + symbol);
    }
    public int biomeAt(int x, int y) { return cell(x, y).biome; }
    public Location location(String name) {
        for (Location location : locations) if (name.equals(location.name)) return location;
        throw new IllegalArgumentException("Authored world: missing location " + name);
    }
    public void paint(long[][] biomes, int[][] terrain) {
        for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) {
            Palette cell = cell(x, y);
            biomes[x][y] = 1L << cell.biome;
            terrain[x][y] = cell.collision ? Integer.MIN_VALUE : 0;
        }
    }
    public void paintRoads(long[][] biomes, int[][] terrain, int roadBiome) {
        for (Road road : roads) for (int i = 1; i < road.points.length; i++) {
            int x = road.points[i - 1][0], y = road.points[i - 1][1];
            int endX = road.points[i][0], endY = road.points[i][1];
            int dx = Math.abs(endX - x), dy = -Math.abs(endY - y);
            int sx = x < endX ? 1 : -1, sy = y < endY ? 1 : -1, error = dx + dy;
            while (true) {
                for (int px = x - road.radius; px <= x + road.radius; px++)
                    for (int py = y - road.radius; py <= y + road.radius; py++) if (inside(px, py)) {
                        biomes[px][py] |= 1L << roadBiome;
                        terrain[px][py] = 0;
                    }
                if (x == endX && y == endY) break;
                int twiceError = 2 * error;
                if (twiceError >= dy) { error += dy; x += sx; }
                if (twiceError <= dx) { error += dx; y += sy; }
            }
        }
    }
}
