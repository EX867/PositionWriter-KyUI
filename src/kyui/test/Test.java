package kyui.test;
import kyui.core.Attributes;
import kyui.core.KyUI;
import kyui.element.*;
import kyui.util.Rect;
import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
public class Test extends PApplet {
  public static void main(String[] args) {
    PApplet.main("kyui.test.Test");
  }
  @Override
  public void settings() {
    size(300, 300);
  }
  int count=0;
  int rcount=0;
  int mcount=0;
  TabLayout f;
  @Override
  public void setup() {
    //frameRate(10);
    frameRate(1000);
    //KyUI.setRoot(new Background("root", color(255, 255, 255)));
    KyUI.start(this);
    f=new TabLayout("tabs", new Rect(0, 0, width, height));
    f.setRotation(Attributes.ROTATE_LEFT);
    f.setButtonRotation(Attributes.ROTATE_NONE);
    f.setButtonEdgeRotation(Attributes.ROTATE_LEFT);
    f.setTabSize(70);
    KyUI.add(f);
    // write your other code
  }
  @Override
  public void draw() {
    //long aa=System.currentTimeMillis();
    KyUI.render(g);
    // write your other code
  }
  @Override
  public void keyTyped() {
    if (key == ' ') {
      f.addTab("Tab" + count, new ToggleButton("Asdf" + count));
      count++;
    }
  }
  @Override
  protected void handleKeyEvent(KeyEvent e) {
    super.handleKeyEvent(e);
    KyUI.handleEvent(e);
  }
  @Override
  protected void handleMouseEvent(MouseEvent e) {
    super.handleMouseEvent(e);
    KyUI.handleEvent(e);
  }
}