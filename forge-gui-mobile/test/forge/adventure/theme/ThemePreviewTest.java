package forge.adventure.theme;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Disposable;
import org.testng.annotations.*;
import static org.testng.Assert.*;

public class ThemePreviewTest {
    @Test public void anEighthLevelThemeCanBePreviewedWithoutAddingAnInheritanceLevel() throws Exception {
        var root=ThemeResolverTest.roots();String parent="folio";
        for(int i=1;i<=7;i++){String id="user.depth"+i;ThemeResolverTest.manifest(root.resolve("users"),id,ThemeResolverTest.child(id,parent,""));parent=id;}
        var catalog=ThemeResolverTest.catalog(root);ThemePreview preview=new ThemePreview(ThemeSkinAdapterTest.legacy());
        preview.rebuild(ThemeResolver.resolve(catalog,parent),ThemePreview.PreviewPage.COMPONENTS);
        assertNotNull(preview.findActor("preview-title"));
        assertFalse(ThemeDraft.from(catalog,parent).validate().isEmpty(),"A real ninth-level variant must remain invalid");preview.dispose();
    }
    @Test public void frameCaptionConsumesTheEditableFrameForeground() throws Exception {
        ThemePreview preview=new ThemePreview(ThemeSkinAdapterTest.legacy());
        preview.rebuild(ThemeSkinAdapterTest.theme(),ThemePreview.PreviewPage.COMPONENTS);
        Label inscription=preview.findActor("preview-frame-caption");assertNotNull(inscription);
        assertEquals(inscription.getStyle().fontColor,Color.valueOf("101810"));preview.dispose();
    }
    @Test public void undecodableOptionalFontKeepsThePreviousPreviewAndReportsValidation() throws Exception {
        var root=ThemeResolverTest.roots();
        var file=ThemeResolverTest.manifest(root.resolve("users"),"badfont",ThemeResolverTest.child("user.badfont","folio",",\"fonts\":{\"body\":\"bad.fnt\"}"));
        java.nio.file.Files.writeString(file.getParent().resolve("bad.fnt"),"invalid bitmap font");
        ThemePreview preview=new ThemePreview(ThemeSkinAdapterTest.legacy());
        preview.rebuild(ThemeDraft.from(ThemeResolverTest.catalog(root),"folio"),ThemePreview.PreviewPage.COMPONENTS);
        var title=preview.findActor("preview-title");
        assertThrows(IllegalArgumentException.class,()->preview.rebuild(ThemeDraft.from(ThemeResolverTest.catalog(root),"user.badfont"),ThemePreview.PreviewPage.COMPONENTS));
        assertSame(preview.findActor("preview-title"),title);preview.dispose();
    }
    private Files prior;
    @BeforeClass public void filesForScene2D(){
        prior=Gdx.files;
        // Cell defaults key their cache by the Files provider. Supply a real filesystem
        // provider without starting an Application, graphics, game, or card database.
        Gdx.files=new Files(){
            public FileHandle getFileHandle(String p,FileType t){return new FileHandle(p);}
            public FileHandle classpath(String p){return new FileHandle(p);}public FileHandle internal(String p){return new FileHandle(p);}
            public FileHandle external(String p){return new FileHandle(p);}public FileHandle absolute(String p){return new FileHandle(p);}
            public FileHandle local(String p){return new FileHandle(p);}public String getExternalStoragePath(){return System.getProperty("java.io.tmpdir");}
            public boolean isExternalStorageAvailable(){return true;}public String getLocalStoragePath(){return System.getProperty("java.io.tmpdir");}
            public boolean isLocalStorageAvailable(){return true;}
        };
    }
    @AfterClass public void restoreFiles(){Gdx.files=prior;}
    @Test public void previewDoesNotTouchActiveRuntimeOrCardDatabase() throws Exception {
        var root=ThemeResolverTest.roots();AdventureThemeRuntime runtime=new AdventureThemeRuntime(root.resolve("builtins"),root.resolve("users"),"folio",id->{throw new AssertionError("Preview cannot persist settings");});
        ThemeDraft draft=ThemeDraft.from(runtime.discover(),"folio");draft.setOrnament("quiet");
        ThemePreview preview=new ThemePreview(ThemeSkinAdapterTest.legacy());
        preview.rebuild(draft,ThemePreview.PreviewPage.ADVENTURE);
        assertNotNull(preview.findActor("preview-life"));assertEquals(((Label)preview.findActor("preview-life")).getText().toString(),"Life 20 / 20");
        assertEquals(runtime.resolveActive(false).ornament(),"full");assertNull(Gdx.app,"No game application was needed");preview.dispose();
    }
    @Test public void repeatedPreviewRebuildReleasesOnlyOwnedResources() throws Exception {
        Skin borrowed=ThemeSkinAdapterTest.legacy();boolean[] disposed={false};borrowed.add("asset",(Disposable)()->disposed[0]=true,Disposable.class);
        ThemeDraft draft=ThemeDraft.from(ThemeResolverTest.catalog(ThemeResolverTest.roots()),"folio");ThemePreview preview=new ThemePreview(borrowed);
        for(int i=0;i<12;i++){draft.setTexture(i%2==0?0:.16f);preview.rebuild(draft,ThemePreview.PreviewPage.values()[i%3]);}
        preview.dispose();preview.dispose();assertFalse(disposed[0]);assertEquals(borrowed.getFont("default").getData().scaleX,1f);
        assertEquals(preview.getChildren().size,0);
    }
}
