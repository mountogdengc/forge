"""Author Bellwake's native TMX tile composition and original procedural ambience.

Run --build to regenerate assets. Optional --preview-dir renders diagnostic views
(Pillow); audio regeneration uses NumPy. Neither is a game/runtime dependency.
"""
import argparse
import base64
import json
from pathlib import Path
import struct
import wave
import xml.etree.ElementTree as ET
import zlib

ROOT = Path(__file__).resolve().parents[2]
ADVENTURE = ROOT / "forge-gui/res/adventure"
MAPS = ADVENTURE / "common/maps/map/bellwake"
SIZE = 30
OFFSET = 13
NC = 11904  # main-nocollide firstgid minus main firstgid


def encode(cells):
    return base64.b64encode(zlib.compress(struct.pack("<" + str(len(cells)) + "I", *cells))).decode("ascii")


def author_map(name):
    path = MAPS / (name + ".tmx")
    root = ET.parse(path).getroot()
    old_height = int(root.get("height"))
    for obj in root.findall("./objectgroup/object"):
        obj.set("y", format(float(obj.get("y")) + (SIZE - old_height) * 16, "g"))
    root.set("height", str(SIZE))
    root.set("nextlayerid", "6")
    for layer in root.findall("layer"):
        root.remove(layer)
    if not any(t.get("firstgid") == "11905" for t in root.findall("tileset")):
        node = ET.Element("tileset", firstgid="11905", source="../../tileset/main-nocollide.tsx")
        root.insert(len(root.findall("tileset")), node)
    layers = {key: [0] * (SIZE * SIZE) for key in ("Background", "Ground", "Foreground", "AboveSprites")}
    layers["Background"] = [1478] * (SIZE * SIZE)

    def put(layer, x, y, gid):
        layers[layer][(y + OFFSET) * SIZE + x] = gid

    def floor(x, y, gid):
        put("Background", x, y, NC + 1478)  # water underneath the raised surface has no collision
        put("Ground", x, y, gid)

    def area(x0, y0, x1, y1, tiles):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                floor(x, y, tiles[(x * 7 + y * 11) % len(tiles)])

    def house(x, y):
        for dx in (0, 1):
            put("AboveSprites", x + dx, y, 11287 + dx)
            put("Foreground", x + dx, y + 1, 11315 + dx)

    # Distant isolated reed banks fill the tall viewport without adding a route.
    for y in range(3, 10):
        for x in range(0, 5):
            if (x - 1) ** 2 + (y - 6) ** 2 < 12:
                layers["Background"][y * SIZE + x] = 1797
                if (x + y) % 3 == 0:
                    layers["Foreground"][y * SIZE + x] = 209 if x % 2 else 3455
    for y in range(4, 9):
        for x in range(26, 30):
            if (x - 29) ** 2 + (y - 6) ** 2 < 8:
                layers["Background"][y * SIZE + x] = 1797
                layers["Foreground"][y * SIZE + x] = 210 if y % 2 else 52

    if name == "quay":
        area(2, 1, 23, 13, [1797])
        area(4, 3, 21, 8, [1230])
        area(3, 8, 22, 13, [6611, 6611, 6611, 6638, 6611, 6640])
        area(12, 5, 14, 13, [6611, 6638])
        area(5, 10, 9, 13, [1230])
        area(10, 14, 11, 15, [6611, 6638])
        area(12, 14, 14, 16, [NC + 6135])
        house(5, 4)
        house(13, 3)
        house(19, 5)
        # Unbroken three-tile landing, with mooring posts and lamps on its flanks.
        for x in list(range(2, 10)) + list(range(16, 24)):
            put("Foreground", x, 14, 5692)
        for x in (11, 15):
            floor(x, 16, 6611)
            put("Foreground", x, 16, 6182)
        for x in (10, 16):
            floor(x, 13, 6611)
            put("Foreground", x, 13, 6017)
        for x, y, gid in [(5, 10, 5703), (9, 10, 5704), (5, 12, 5703),
                          (19, 9, 6182), (20, 9, 6183), (21, 10, 6026),
                          (3, 4, 3455),
                          (22, 2, 3456), (3, 7, 3456), (18, 2, 209)]:
            put("Foreground", x, y, gid)
        # A moored timber ferry is scenery, not an extra walkable island.
        for x in range(17, 21):
            for y in (15, 16):
                put("Foreground", x, y, NC + 6135)
        for x in (17, 20):
            put("AboveSprites", x, 15, 5703)
        put("AboveSprites", 18, 15, 6184)
        put("AboveSprites", 19, 16, 6182)
        # Visual sign aligns with the interaction, and does not prevent contact.
        put("AboveSprites", 11, 15, NC + 2350)
    else:
        area(9, 3, 20, 9, [6611, 6611, 6638, 6640])
        area(12, 10, 14, 16, [6611, 6611, 6638])
        area(9, 14, 11, 15, [6611, 6638])
        area(12, 5, 15, 7, [6611, 6638])
        for x in range(8, 22):
            if x not in (11, 17):
                put("Foreground", x, 2, 5692)
        for y in range(3, 10):
            if y not in (5, 8):
                put("Foreground", 8, y, 5697)
                put("Foreground", 21, y, 5699)
        for x in list(range(8, 12)) + list(range(15, 22)):
            put("Foreground", x, 10, 5692)
        for x, y, gid in [(9, 3, 5702), (20, 3, 5702), (18, 8, 6183),
                          (9, 8, 5702), (22, 4, 209), (7, 7, 210),
                          (17, 12, 52), (8, 13, 209), (20, 14, 210),
                          (7, 11, 3455), (22, 11, 3456)]:
            put("Foreground", x, y, gid)
        for x in (11, 15):
            put("Foreground", x, 11, 5697)
            put("Foreground", x, 12, 5699)
        put("AboveSprites", 10, 14, NC + 6656)
    insertion = list(root).index(root.find("objectgroup"))
    for index, (key, cells) in enumerate(layers.items()):
        node = ET.Element("layer", id=str([1, 2, 3, 5][index]), name=key, width="30", height="30")
        if key == "Foreground":
            props = ET.SubElement(node, "properties")
            ET.SubElement(props, "property", name="spriteLayer", type="bool", value="true")
        ET.SubElement(node, "data", encoding="base64", compression="zlib").text = "\n   " + encode(cells) + "\n  "
        root.insert(insertion + index, node)
    ET.indent(root, space=" ")
    ET.ElementTree(root).write(path, encoding="utf-8", xml_declaration=True)


TRACKS = {
    "town": ("river-lanterns", 101, 294., (12., 36.)),
    "black": ("reed-water", 202, 0., ()),
    "cave": ("last-fare", 303, 147., (9., 29., 41.)),
    "boss": ("keeper-pulse", 404, 110., (16., 39.)),
    "match": ("tor-training", 505, 220., (24.,)),
}


def author_audio():
    import numpy as np
    rate, duration = 22050, 48
    length = rate * duration
    time = np.arange(length) / rate
    frequencies = np.fft.rfftfreq(length, 1 / rate)
    for context, (name, seed, bell, events) in TRACKS.items():
        rng = np.random.default_rng(seed)

        def noise(low, high):
            spectrum = np.fft.rfft(rng.standard_normal(length))
            shape = (1 - np.exp(-(frequencies / low) ** 4)) * np.exp(-(frequencies / high) ** 4)
            signal = np.fft.irfft(spectrum * shape, n=length)
            return signal / np.std(signal)

        water = noise(180, 1500)
        wind = noise(35, 320)
        signal = .012 * water * (.65 + .25 * np.sin(2 * np.pi * time / 8.7))
        signal += .011 * wind * (.7 + .2 * np.sin(2 * np.pi * time / 17.3))
        if context == "town":
            signal += .0025 * noise(900, 2900) * (.6 + .3 * np.sin(2 * np.pi * time / 3.1))
        if context == "cave":
            signal += .004 * np.sin(2 * np.pi * 55 * time) * (.5 + .3 * np.sin(time / 4))
        for start in events:
            elapsed = np.maximum(0, time - start)
            envelope = (time >= start) * (1 - np.exp(-elapsed / .025)) * np.exp(-elapsed / 2.7)
            tone = sum(gain * np.sin(2 * np.pi * bell * ratio * elapsed)
                       for ratio, gain in [(1, .047), (2.71, .016), (5.8, .006)])
            signal += envelope * tone
            if context == "cave":
                for delay, gain in [(.27, .25), (.61, .12)]:
                    shift = int(delay * rate)
                    signal[shift:] += gain * (envelope * tone)[:-shift]
        if context in ("boss", "match"):
            spacing = 2.4 if context == "boss" else 1.6
            for start in np.arange(1., duration - 1., spacing):
                elapsed = np.maximum(0, time - start)
                signal += .027 * (time >= start) * np.exp(-elapsed / .12) * np.sin(2 * np.pi * 82 * elapsed)
        signal -= np.mean(signal)
        signal *= min(1., .18 / np.max(np.abs(signal)))
        fade = np.sin(np.linspace(0, np.pi / 2, int(rate * .2))) ** 2
        signal[:len(fade)] *= fade
        signal[-len(fade):] *= fade[::-1]
        pcm = np.round(signal * 32767).astype("<i2")
        destination = ADVENTURE / "Bellwake/music" / context / (name + ".wav")
        destination.parent.mkdir(parents=True, exist_ok=True)
        with wave.open(str(destination), "wb") as output:
            output.setparams((1, 2, rate, 0, "NONE", "not compressed"))
            output.writeframes(pcm.tobytes())
        print(context, "peak", round(float(np.max(np.abs(signal))), 4),
              "RMS", round(float(np.sqrt(np.mean(signal ** 2))), 4))


def preview(directory):
    from PIL import Image
    directory.mkdir(parents=True, exist_ok=True)
    for name in ("quay", "toll"):
        path = MAPS / (name + ".tmx")
        root = ET.parse(path).getroot()
        width, height = int(root.get("width")), int(root.get("height"))
        image = Image.new("RGBA", (width * 16, height * 16))
        tilesets = []
        for reference in root.findall("tileset"):
            source = path.parent / reference.get("source")
            data = ET.parse(source).getroot()
            atlas = Image.open(source.parent / data.find("image").get("source")).convert("RGBA")
            tilesets.append((int(reference.get("firstgid")), atlas, int(data.get("columns"))))
        for layer in root.findall("layer"):
            cells = struct.unpack("<" + str(width * height) + "I", zlib.decompress(base64.b64decode(layer.find("data").text)))
            for index, gid in enumerate(cells):
                gid &= 0x1fffffff
                if not gid:
                    continue
                first, atlas, columns = max((t for t in tilesets if t[0] <= gid), key=lambda t: t[0])
                local = gid - first
                x, y = local % columns * 16, local // columns * 16
                image.alpha_composite(atlas.crop((x, y, x + 16, y + 16)), (index % width * 16, index // width * 16))
        image.resize((width * 32, height * 32), Image.Resampling.NEAREST).save(directory / (name + ".png"))
        image.crop((0, max(0, height - 17) * 16, width * 16, height * 16)).resize((width * 32, 544), Image.Resampling.NEAREST).save(directory / (name + "-landscape.png"))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--build", action="store_true")
    parser.add_argument("--preview-dir", type=Path)
    args = parser.parse_args()
    if args.build:
        for map_name in ("quay", "toll"):
            author_map(map_name)
        author_audio()
        manifest = ADVENTURE / "Bellwake/campaign.json"
        data = json.loads(manifest.read_text(encoding="utf-8"))
        data["version"] = "0.3.0"
        manifest.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    if args.preview_dir:
        preview(args.preview_dir)
