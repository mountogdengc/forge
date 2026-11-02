package forge.adventure.editor;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.util.Config;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises repair of an existing project without the creation wizard. Invisible only. */
public final class CampaignDirectAuthoringSmoke {
 static JButton button(Container root,String text) {
  for(Component c:root.getComponents()) { if(c instanceof JButton b && b.getText().equals(text)) return b; if(c instanceof Container p) { JButton b=button(p,text); if(b!=null)return b; } } return null;
 }
 static void waitChecks(CampaignOverviewPanel panel) throws Exception {
  long end=System.nanoTime()+45_000_000_000L; boolean[] done={false};
  while(!done[0] && System.nanoTime()<end) { SwingUtilities.invokeAndWait(()->done[0]=panel.validate.isEnabled());Thread.sleep(100); }
  if(!done[0])throw new AssertionError("Checks timed out");
 }
 public static void main(String[] args) throws Exception {
  if(!Boolean.getBoolean("editor.isolatedProfile"))throw new IllegalStateException("Isolated profile required");
  forge.adventure.Main.initializeEnvironment();
  Path installed=Path.of(Config.instance().getAdventureRoot()).toAbsolutePath().normalize();
  Path project=Path.of(System.getProperty("editor.directProject")).toAbsolutePath(); Files.createDirectories(project);
  Path source=installed.resolve("Bellwake");
  try(var stream=Files.walk(source)) { for(Path from:stream.toList()) { Path target=project.resolve(source.relativize(from)); if(Files.isDirectory(from))Files.createDirectories(target);else Files.copy(from,target); } }
  Path catalog=project.resolve("world/points_of_interest.json");String good=Files.readString(catalog);
  Files.writeString(catalog,good.replace("quay.tmx","missing-for-repair.tmx"));
  Config.activateCampaign(Config.forEditing(CampaignRegistry.readProjectMetadata(project,installed.resolve("common"),true)));
  AtomicReference<EditorMainWindow> frame=new AtomicReference<>();AtomicReference<CampaignOverviewPanel> panel=new AtomicReference<>();
  SwingUtilities.invokeAndWait(()->{EditorMainWindow w=new EditorMainWindow(Config.instance()){@Override public void setVisible(boolean visible){super.setVisible(false);}};frame.set(w);panel.set((CampaignOverviewPanel)w.tabs.getComponentAt(0));});
  SwingUtilities.invokeAndWait(()->{});waitChecks(panel.get());
  if(!panel.get().results.getText().contains("missing-for-repair"))throw new AssertionError("Automatic error not surfaced: "+panel.get().results.getText());
  SwingUtilities.invokeAndWait(()->{
   try {
    panel.get().validateCampaign(); panel.get().validateCampaign();
    var pending=CampaignOverviewPanel.class.getDeclaredField("validationQueued");pending.setAccessible(true);if(!pending.getBoolean(panel.get()))throw new AssertionError("Changes check not queued");
    frame.get().tabs.setSelectedIndex(frame.get().tabs.indexOfTab("Files"));
    CampaignFilesEditor files=(CampaignFilesEditor)((EditorMainWindow.LazyTool)frame.get().tabs.getSelectedComponent()).getComponent(0);
    var field=CampaignFilesEditor.class.getDeclaredField("file");field.setAccessible(true);((JComboBox<?>)field.get(files)).setSelectedItem("world/points_of_interest.json");
    button(files,"Open / Reload").doClick();files.text.setText(good);button(files,"Save JSON").doClick();
   }catch(Exception e){throw new RuntimeException(e);}
  });waitChecks(panel.get());
  if(!panel.get().results.getText().startsWith("Validation passed"))throw new AssertionError(panel.get().results.getText());
  SwingUtilities.invokeAndWait(()->{
   frame.get().tabs.setSelectedIndex(frame.get().tabs.indexOfTab("Locations"));frame.get().tabs.setSelectedIndex(0);
   QuestController.getInstance().getAllQuests().get(0).name="UNSAVED QUEST EDIT";
   button(panel.get(),"Create Editable Map Copies").doClick();
  });waitChecks(panel.get());
  if(!Files.readString(catalog).contains("campaign:maps/private-"))throw new AssertionError("Direct map copy failed");
  SwingUtilities.invokeAndWait(()->{
   EditorMainWindow.LazyTool tool=(EditorMainWindow.LazyTool)frame.get().tabs.getComponentAt(frame.get().tabs.indexOfTab("Locations"));
   PointOfInterestEditor locations=(PointOfInterestEditor)tool.getComponent(0);
   if(!locations.model.get(0).map.startsWith("campaign:maps/private-"))throw new AssertionError("Stale Locations model");
   locations.save();
   if(!QuestController.getInstance().getAllQuests().get(0).name.equals("UNSAVED QUEST EDIT"))throw new AssertionError("Map copy discarded quest edits");
   for(Window window:Window.getWindows())if(window.isVisible()||window.isFocused())throw new AssertionError("Visible editor");frame.get().dispose();
  });
  if(!Files.readString(catalog).contains("campaign:maps/private-"))throw new AssertionError("Locations save undid private maps");
  System.out.println("CAMPAIGN_DIRECT_AUTHORING_PASS: no wizard; automatic errors, Files repair, queued check, direct map copy and refreshed Locations save");System.exit(0);
 }
}
