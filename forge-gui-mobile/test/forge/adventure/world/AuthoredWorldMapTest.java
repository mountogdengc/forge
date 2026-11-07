package forge.adventure.world;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import forge.adventure.data.AuthoredWorldMapData;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.testng.Assert.*;

public class AuthoredWorldMapTest {
    private AuthoredWorldMapData sample() {
        return new Json().fromJson(AuthoredWorldMapData.class, """
                {"width":7,"height":5,"rows":["gggwggg","gggwggg","gggwggg","gggwggg","gggwggg"],
                 "palette":[{"symbol":"g","biome":0},{"symbol":"w","biome":1,"collision":true}],
                 "locations":[{"name":"Spawn","x":1,"y":2}],
                 "roads":[{"radius":1,"points":[[1,2],[5,2]]}]}
                """);
    }

    @Test public void bridgeIsPassableWithoutDrainingTheRestOfTheRiver() {
        var data = sample(); data.validate(7, 5, 2);
        long[][] biomes = new long[7][5]; int[][] terrain = new int[7][5];
        data.paint(biomes, terrain);
        assertTrue((terrain[3][0] & Integer.MIN_VALUE) != 0);
        data.paintRoads(biomes, terrain, 2);
        assertEquals(terrain[3][2], 0);
        assertTrue((biomes[3][2] & (1L << 2)) != 0);
        assertTrue((terrain[3][0] & Integer.MIN_VALUE) != 0);
        assertEquals(biomes[3][0], 1L << 1);
        assertEquals(data.location("Spawn").x, 1);
    }

    @Test public void paintingAndRoadsAreIndependentOfWorldRandomness() {
        var data = sample(); data.validate(7, 5, 2);
        long[][] first = new long[7][5], second = new long[7][5];
        int[][] a = new int[7][5], b = new int[7][5];
        data.paint(first, a); data.paintRoads(first, a, 2);
        data.paint(second, b); data.paintRoads(second, b, 2);
        assertTrue(Arrays.deepEquals(first, second)); assertTrue(Arrays.deepEquals(a, b));
    }

    @Test public void invalidDimensionsSymbolsAnchorsAndRoadsAreRejectedBeforeGeneration() {
        var shortRows = sample(); shortRows.rows[1] = "short";
        assertThrows(IllegalArgumentException.class, () -> shortRows.validate(7, 5, 2));
        var unknown = sample(); unknown.rows[1] = "gggxggg";
        assertThrows(IllegalArgumentException.class, () -> unknown.validate(7, 5, 2));
        var duplicate = sample(); duplicate.locations = new AuthoredWorldMapData.Location[]{duplicate.locations[0], duplicate.locations[0]};
        assertThrows(IllegalArgumentException.class, () -> duplicate.validate(7, 5, 2));
        var outside = sample(); outside.roads[0].points[1][0] = 7;
        assertThrows(IllegalArgumentException.class, () -> outside.validate(7, 5, 2));
        var blocked = sample(); blocked.locations[0].x = 3;
        assertThrows(IllegalArgumentException.class, () -> blocked.validate(7, 5, 2));
        var badBiome = sample(); badBiome.palette[0].biome = 2;
        assertThrows(IllegalArgumentException.class, () -> badBiome.validate(7, 5, 2));
        var overlap = sample();
        var neighbor = new AuthoredWorldMapData.Location(); neighbor.name = "Other"; neighbor.x = 2; neighbor.y = 2;
        overlap.locations = new AuthoredWorldMapData.Location[]{overlap.locations[0], neighbor};
        assertThrows(IllegalArgumentException.class, () -> overlap.validate(7, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> sample().location("Missing POI"));
    }

    @Test public void ordinaryBlockedTerrainIsNotAStructure() throws Exception {
        World world = new World(); Field height = World.class.getDeclaredField("height"); height.setAccessible(true); height.setInt(world, 2);
        world.terrainMap = new int[][]{{Integer.MIN_VALUE, 1 << 30}, {1, 0}};
        assertFalse(world.isStructure(0, 1), "Water/mountains may still have decorative sprites");
        assertFalse(world.isStructure(1, 1), "Terrain variant is not a structure");
        assertTrue(world.isStructure(0, 0));
    }

    @Test public void authoredTerrainIsNotClearedByProceduralSpawnPadding() throws Exception {
        World world = new World();
        var data = new forge.adventure.data.WorldData(); data.authoredMap = "world/fixed.json";
        Field config = World.class.getDeclaredField("data"); config.setAccessible(true); config.set(world, data);
        Field height = World.class.getDeclaredField("height"); height.setAccessible(true); height.setInt(world, 5);
        world.terrainMap = new int[5][5]; for (int[] column : world.terrainMap) Arrays.fill(column, Integer.MIN_VALUE);
        var clear = World.class.getDeclaredMethod("clearTerrain", int.class, int.class, int.class); clear.setAccessible(true);
        clear.invoke(world, 2, 2, 2);
        assertEquals(world.terrainMap[2][2], Integer.MIN_VALUE, "Authored water must stay blocked outside its bridges");
        data.authoredMap = " "; clear.invoke(world, 2, 2, 2);
        assertEquals(world.terrainMap[2][2], 0, "Blank authored field retains procedural padding");
    }

    @Test public void edgeAndDisconnectedAnchorsFailBeforeWorldPlacement() {
        var edge = sample(); edge.locations[0].y = edge.height - 1;
        assertThrows(IllegalArgumentException.class, () -> edge.validate(7, 5, 2));
        var divided = new Json().fromJson(AuthoredWorldMapData.class, """
                {"width":15,"height":5,"rows":["gggggggwggggggg","gggggggwggggggg","gggggggwggggggg","gggggggwggggggg","gggggggwggggggg"],
                 "palette":[{"symbol":"g","biome":0},{"symbol":"w","biome":1,"collision":true}],
                 "locations":[{"name":"Spawn","x":1,"y":2},{"name":"Other","x":13,"y":2}],"roads":[]}
                """);
        assertThrows(IllegalArgumentException.class, () -> divided.validate(15, 5, 2));
        var road = new AuthoredWorldMapData.Road(); road.points = new int[][]{{1,2},{13,2}};
        divided.roads = new AuthoredWorldMapData.Road[]{road}; divided.validate(15, 5, 2);
    }

    @Test public void negativeSeedsStillChooseARealPixelSprite() throws Exception {
        BiomeSprites data = new BiomeSprites();
        Field cache = BiomeSprites.class.getDeclaredField("spriteBuffer"); cache.setAccessible(true);
        @SuppressWarnings("unchecked") ObjectMap<String, Array<Sprite>> sprites = (ObjectMap<String, Array<Sprite>>) cache.get(data);
        Sprite first = new Sprite(), second = new Sprite(); sprites.put("WoodTree", Array.with(first, second));
        assertSame(data.getSprite("WoodTree", -1), second);
        assertNotNull(data.getSprite("WoodTree", Integer.MIN_VALUE));
    }
}
