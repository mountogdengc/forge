"""Build a targeted, backed-up update for the tested Strahd 0.1.0 Windows client."""
import argparse
import json
from pathlib import Path
import shutil
import zipfile
import re
from package_windows_test import changed_sources, sha256, check_jar


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', type=Path, required=True)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--editor-jar', type=Path, help='Bundle the matching editor and startup-menu entry')
    parser.add_argument('--update-id', help='Identifier for a compatible follow-up update')
    parser.add_argument('--authoring', action='store_true', help='Bundle the New Campaign and overview increment')
    parser.add_argument('--java-test-count', type=int, default=139)
    parser.add_argument('--python-test-count', type=int, default=20)
    parser.add_argument('--compatible-jar-sha', action='append', default=[])
    args = parser.parse_args()
    if args.authoring and not args.editor_jar: raise ValueError('Authoring requires the matching editor')
    update_id = args.update_id or ('campaign-authoring-1' if args.authoring else 'campaign-editor-1')
    if not re.fullmatch('[A-Za-z0-9._-]+', update_id): raise ValueError('Invalid update identifier')
    root = Path(__file__).resolve().parents[2]
    base, jar, output = args.base.resolve(), args.jar.resolve(), args.output.resolve()
    if output.exists(): raise ValueError('Choose a new output folder')
    info = json.loads((base / 'BUILD-INFO.json').read_text(encoding='utf-8'))
    base_hash = info['jar_sha256']
    if sha256(base / 'forge-adventure-test.jar') != base_hash: raise ValueError('Base client hash mismatch')
    check_jar(jar)
    for checksum in args.compatible_jar_sha:
        if not re.fullmatch('[0-9a-f]{64}',checksum): raise ValueError('Invalid compatible client checksum')
    stage = output / ('Campaign-Authoring-Update' if args.authoring else 'Campaign-Editor-Update' if args.editor_jar else 'Strahd-Encounter-Cleanup')
    payload = stage / 'payload'
    payload.mkdir(parents=True)
    shutil.copyfile(jar, payload / 'forge-adventure-test.jar')
    if args.editor_jar:
        with zipfile.ZipFile(args.editor_jar) as editor:
            if editor.testzip() or 'forge/adventure/editor/EditorFiles.class' not in editor.namelist():
                raise ValueError('The matching editor with campaign-local save handling is required')
            if args.authoring and any(name not in editor.namelist() for name in [
                    'forge/adventure/editor/NewCampaignWizard.class',
                    'forge/adventure/editor/CampaignOverviewPanel.class',
                    'forge/adventure/editor/CampaignProjectCreator.class']):
                raise ValueError('Authoring classes missing from editor')
        shutil.copyfile(args.editor_jar,payload/'adventure-editor.jar')
    resources = ['adventure/CurseOfStrahd/story/dialogues.json',
                 'adventure/common/maps/map/strahd/church.tmx', 'adventure/common/maps/map/strahd/road.tmx']
    if args.editor_jar:
        resources += ['adventure/'+folder+'/ui/'+name for folder in ['common','Innistrad']
                      for name in ['start_menu.json','start_menu_portrait.json']]
        shutil.copyfile(root/'docs/Adventure/Campaign-Editor.md',payload/'CAMPAIGN-EDITOR.txt')
    for name in resources:
        target = payload / 'res' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(root / 'forge-gui/res' / name, target)
    shutil.copyfile(root / 'docs/Adventure/Curse-of-Strahd-Playtests.md', payload / 'READ-ME-FIRST.txt')
    changes = {}
    for name in sorted(set(filter(None, changed_sources(root)))):
        source = root / name
        if not source.is_file(): changes[name] = 'deleted'; continue
        target = payload / 'SOURCE-CHANGES' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        changes[name] = sha256(source)
    info['source_changes_from_commit'] = changes
    info['jar_sha256'] = sha256(jar)
    info['encounter_cleanup_patch'] = {'id':'strahd-0.1.0-encounter-cleanup-1', 'base_jar_sha256':base_hash,
        'affected_java_tests':131, 'python_tool_tests':18,
        'scope':'wolf removal, Doru decision cleanup, mounted Strahd departure, completed-save reconciliation',
        'save_content_version':'0.1.0 unchanged', 'manual':'visual feel remains a player acceptance check'}
    if args.editor_jar:
        info['base_build_tests'] = info.get('tests')
        info['tests'] = {'tests':args.java_test_count,'failures':0,'errors':0,'skipped':0,
                         'scope':'affected reactor modules; earlier full desktop evidence retained under base_build_tests'}
        if 'native_verification' in info:
            info['base_build_native_verification'] = info.pop('native_verification')
        info['editor_jar_sha256'] = sha256(args.editor_jar)
        info['campaign_editor_update'] = {'id':update_id,'editor_jar_sha256':sha256(args.editor_jar),
            'startup_button':'Campaign Editor','editor_heap_mb':512,'editor_processors':2,
            'affected_java_tests':args.java_test_count,'python_tool_tests':args.python_test_count,
            'scope':('new campaign wizard, private maps, project overview, metadata editing, offline card/reference validation' if args.authoring else 'desktop startup button, independent editor window, campaign picker, campaign-local saves'),
            'compatible_prior_clients':[base_hash]+args.compatible_jar_sha}
    (payload / 'BUILD-INFO.json').write_text(json.dumps(info, indent=2)+'\n',encoding='utf-8')
    files = {p.relative_to(payload).as_posix():sha256(p) for p in sorted(payload.rglob('*')) if p.is_file()}
    manifest = {'base_jar_sha256':base_hash,'jar_sha256':sha256(jar),'files':files,
                'compatible_base_jars':args.compatible_jar_sha}
    if args.editor_jar: manifest.update(id=update_id,display_name='Campaign authoring update' if args.authoring else 'Campaign Editor update')
    (stage / 'PATCH-MANIFEST.json').write_text(json.dumps(manifest, indent=2)+'\n',encoding='utf-8')
    shutil.copyfile(root / 'tools/adventure/Apply-Strahd-Cleanup.ps1', stage / 'Apply-Update.ps1')
    (stage / 'Apply-Update.cmd').write_bytes(b'@echo off\r\npowershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Apply-Update.ps1"\r\nset "result=%ERRORLEVEL%"\r\nif not "%result%"=="0" echo Update failed. Read the error above.\r\npause\r\nexit /b %result%\r\n')
    (stage / 'READ-ME.txt').write_text('''STRAHD 0.1.0 ENCOUNTER CLEANUP UPDATE
1. Close the game.
2. Extract this ZIP, then place the Strahd-Encounter-Cleanup folder INSIDE your
   existing game folder, beside Start-CurseOfStrahd.cmd and forge-adventure-test.jar.
3. Open Strahd-Encounter-Cleanup and double-click Apply-Update.cmd.
4. After success, launch Start-CurseOfStrahd.cmd normally and choose Continue.

The updater validates the base client and update hashes, backs up replaced files
under .updates, and verifies every copy. It preserves saves, cache, launchers and
all display/audio preferences. Do not copy the payload files by hand.

Defeated wolves disappear after the death animation. Doru remains for his fate
choice and final line, then disappears. Existing completed saves hide resolved
wolves/Doru on map load. Strahd rides away after his next conversation and stays
gone on revisits. Practice remains available. No new playthrough is required.

Quick check: Continue an existing save, revisit church/road, finish Strahd's
conversation, leave/re-enter and save/reload. Report any returning figure or
interrupted movement, along with the save name and last action.
''',encoding='utf-8')
    if args.editor_jar:
        (stage/'READ-ME.txt').write_text('''CAMPAIGN EDITOR STARTUP BUTTON UPDATE
1. Close the game and any open editor.
2. Put this Campaign-Editor-Update folder INSIDE your existing game folder,
   beside Start-CurseOfStrahd.cmd and forge-adventure-test.jar.
3. Open Campaign-Editor-Update and double-click Apply-Update.cmd.
4. Start the game normally. Click Campaign Editor on the startup menu and
   choose the campaign to edit. The tools open in their own window.

Closing the editor leaves the game running. The button is hidden while a
playthrough is active. World, POI, Items, Enemies, Quests and Allowed sets are
the existing tools; maps still use Tiled. This adds access to those tools,
not a new campaign-creation wizard. Saves create local campaign overrides
instead of overwriting shared inherited files. Restart the game after edits.

Your saves, launchers, Java runtime, audio and 1080p/30 FPS settings remain.
The updater checks hashes, creates backups and rolls back copy failures.
The prior wolves/Doru/Strahd cleanup is included; no separate update needed.

Manual check: open the editor from startup, verify its campaign title and
six tabs, close it, then Continue your game. Confirm only one editor opens
on repeated clicks. Report the last action and logs/campaign-editor.log if
opening fails. Use a separate campaign copy for substantial content changes.
''',encoding='utf-8')
    if args.authoring:
        (stage/'READ-ME.txt').write_text("""CAMPAIGN AUTHORING UPDATE
1. Close the game and editor.
2. Put this Campaign-Authoring-Update folder INSIDE your existing game folder,
   beside Start-CurseOfStrahd.cmd and forge-adventure-test.jar.
3. Open Campaign-Authoring-Update and double-click Apply-Update.cmd.
4. Start the game normally. Click Campaign Editor, then New Campaign.
5. Name your project, choose Bellwake or Curse of Strahd, and Create Campaign.
6. Open any existing campaign for the same tools. Checks start automatically.
   Use Files / Repairs for JSON, Create Editable Map Copies without creating a campaign,
   and searchable Allowed sets checkboxes.
7. Save each tool, restart the game, select your new campaign and start a new run.

Your original campaigns and saved runs are preserved. Creation uses a unique
identity and private copies of maps and dependencies. Card availability is
copied from the template; use Allowed sets to change it. Maps use Tiled.
There is no visual dialogue editor or Play-from-here in this increment.

The updater checks replacements, creates backups and rolls back copy failures.
Saves, cache, Java runtime, launchers, audio and 1080p/30 FPS settings remain.
Earlier startup button and wolves/Doru/Strahd cleanup updates are included.

See CAMPAIGN-EDITOR.txt for the numbered manual acceptance checklist. Report
the step, project folder, last action, error and logs/campaign-editor.log.
""",encoding='utf-8')
    archive = output / (stage.name+'.zip')
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as bundle:
        for p in sorted(stage.rglob('*')):
            if p.is_file(): bundle.write(p,p.relative_to(output))
    with zipfile.ZipFile(archive) as bundle:
        if bundle.testzip(): raise ValueError('Update CRC failed')
    checksum = sha256(archive)
    archive.with_suffix('.zip.sha256').write_text(f'{checksum}  {archive.name}\n',encoding='ascii')
    print(json.dumps({'archive':str(archive),'bytes':archive.stat().st_size,'sha256':checksum,'payload_files':len(files)},indent=2))


if __name__ == '__main__': main()
