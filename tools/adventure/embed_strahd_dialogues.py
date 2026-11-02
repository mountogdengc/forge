"""Embed authoring dialogue into the literal TMX properties used by MapDialog."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def embed(catalog, maps, check=False):
    catalog = Path(catalog)
    entries = json.loads(catalog.read_text(encoding="utf-8"))
    if not isinstance(entries, dict):
        raise ValueError("dialogue catalog must be an object")
    trees, seen, changes = [], set(), []
    for path in sorted(Path(maps).glob("*.tmx")):
        tree = ET.parse(path)
        trees.append((path, tree))
        for actor in tree.findall(".//object"):
            props = actor.find("properties")
            if props is None:
                continue
            key_prop = props.find("property[@name='cosDialogKey']")
            if key_prop is None:
                continue
            key = key_prop.get("value") or key_prop.text
            if key in seen:
                raise ValueError(f"duplicate dialogue key {key}")
            seen.add(key)
            if key not in entries:
                raise ValueError(f"missing dialogue catalog entry {key}")
            entry = entries[key]
            if not isinstance(entry, dict) or not isinstance(entry.get("dialog"), list):
                raise ValueError(f"invalid dialog array for {key}")
            for field in ("dialog", "defeatDialog", "removeWhenQuestFlag"):
                if field not in entry:
                    continue
                if field == "removeWhenQuestFlag":
                    if not isinstance(entry[field], str) or not entry[field]:
                        raise ValueError(f"invalid removal flag for {key}")
                    value = entry[field]
                elif not isinstance(entry[field], list):
                    raise ValueError(f"invalid {field} array for {key}")
                else:
                    value = json.dumps(entry[field], ensure_ascii=False, separators=(",", ":"))
                prop = props.find(f"property[@name='{field}']")
                old = None if prop is None else (prop.get("value") or prop.text or "")
                if old != value:
                    changes.append(f"{path.name}:{key}:{field}")
                    if prop is None:
                        prop = ET.SubElement(props, "property", name=field)
                    prop.set("value", value)
                    prop.text = None
    unused = set(entries) - seen
    if unused:
        raise ValueError(f"unused dialogue catalog keys: {sorted(unused)}")
    if check and changes:
        raise ValueError(f"stale embedded dialogue: {', '.join(changes)}")
    if not check:
        for path, tree in trees:
            ET.indent(tree, space=" ")
            tree.write(path, encoding="utf-8", xml_declaration=True)
    return len(seen)


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    count = embed(root / "forge-gui/res/adventure/CurseOfStrahd/story/dialogues.json",
                  root / "forge-gui/res/adventure/common/maps/map/strahd", args.check)
    print(f"{'Checked' if args.check else 'Embedded'} {count} dialogue actors")
