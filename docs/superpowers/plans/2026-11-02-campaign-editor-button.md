# Campaign Editor startup button

User requests an editor tool button on the game's startup window.

- Reuse the existing authoring application in an independent desktop process;
  closing its Swing window must not exit the game. Do not add a new editor/wizard.
- Use the startup menu's empty Save slot; hide the editor entry during an active
  playthrough and on unsupported native mobile adapters. Preserve existing menus.
- Offer a campaign picker, label the editing target and explain that saved edits
  require a game restart. Opening tools does not save or alter a playthrough.
- Use bundled Java,512MB editor heap/two processors, explicit argument boundaries,
  log output and duplicate-launch prevention. Keep the game at1080p/30FPS/audio.
- Launcher tests: observed3RED; prove safe command, missing-tools error and an
  independently closing child. Build affected modules, hidden menu integration
  and invisible Swing construction; review and deliver a save-preserving update.
