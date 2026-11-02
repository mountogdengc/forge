package forge.adventure.editor;
import forge.adventure.util.Config;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/** Direct file access also works for drafts that a structured tool cannot read. */
final class CampaignFilesEditor extends JPanel {
 private final Config config;
 private final Runnable saved;
 private final JComboBox<String> file = new JComboBox<>();
 final JTextArea text = new JTextArea();
 private final JLabel status = new JLabel(" ");
 private String current, original, expected;
 CampaignFilesEditor(Config config,Runnable saved) {
  super(new BorderLayout(6,6)); this.config=config; this.saved=saved;
  JPanel bar=new JPanel(new FlowLayout(FlowLayout.LEFT));
  file.setEditable(true);
  for(String name:new String[]{"campaign.json","config.json","world/world.json","world/points_of_interest.json","world/quests.json","world/enemies.json","world/items.json","world/shops.json","story/dialogues.json"}) file.addItem(name);
  bar.add(new JLabel("Project-relative JSON file:")); bar.add(file);
  JButton load=new JButton("Open / Reload"), save=new JButton("Save JSON");
  load.addActionListener(e->load()); save.addActionListener(e->save()); bar.add(load);bar.add(save);
  add(bar,BorderLayout.NORTH); text.setFont(new Font(Font.MONOSPACED,Font.PLAIN,14)); add(new JScrollPane(text),BorderLayout.CENTER);
  JPanel footer=new JPanel(new GridLayout(0,1)); footer.add(status);
  footer.add(new JLabel("Existing forms reload using their Load button. Save each tool before switching to edit the same file here.")); add(footer,BorderLayout.SOUTH);
  load();
 }
 private void load() {
  if (original!=null && !original.equals(text.getText()) && JOptionPane.showConfirmDialog(this,"Discard unsaved file changes?","Reload file",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;
  try {
   String name=String.valueOf(file.getEditor().getItem()).trim();
   Path root=Path.of(config.getPrefix()).toAbsolutePath().normalize(), target=root.resolve(name).normalize();
   if (Path.of(name).isAbsolute() || !target.startsWith(root) || !name.endsWith(".json")) throw new IllegalArgumentException("Choose a project-relative JSON file.");
   expected=Files.isRegularFile(target)?Files.readString(target,StandardCharsets.UTF_8):null;
   var inherited=EditorFiles.read(config,name);
   String value=expected!=null?expected:inherited!=null && inherited.exists()?inherited.readString("UTF-8"):"{}";
   current=name; original=value; text.setText(value);text.setCaretPosition(0);
   status.setText(expected==null?"Inherited or new file. Saving creates a campaign-local copy.":"Editing "+target);
  } catch(Exception error) { status.setText("Could not open file: "+error.getMessage()); }
 }
 private void save() {
  if(current==null) return;
  try {
   CampaignJsonStore.save(Path.of(config.getPrefix()),current,expected,text.getText());
   expected=text.getText();original=expected;status.setText("Saved "+current+". Campaign checks are running; reload any form that was editing this file.");saved.run();
  } catch(Exception error) { status.setText("Not saved: "+error.getMessage()); }
 }
}
