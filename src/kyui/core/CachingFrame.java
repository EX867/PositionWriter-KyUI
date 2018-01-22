package kyui.core;
import kyui.util.Rect;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
public final class CachingFrame extends Element {
  public PGraphics display;
  boolean invalidated=false;
  public CachingFrame(String name, Rect pos_) {
    super(name);
    pos=pos_;
    display=KyUI.Ref.createGraphics((int)(pos.right - pos.left), (int)(pos.bottom - pos.top));
  }
  @Override
  synchronized void render_(PGraphics g) {
    boolean a=renderFlag || invalidated;
    if (a) render(null);//???
    renderChildren(display);
    if (a) {
      display.popMatrix();
      display.endDraw();
    }
    renderFlag=false;
    invalidated=false;
  }
  @Override
  public void render(PGraphics g) {
    display.beginDraw();
    display.pushMatrix();
    display.scale(KyUI.scaleGlobal);
    if (renderFlag) {
      display.clear();
    }
    display.rectMode(PApplet.CORNERS);
    display.textAlign(PApplet.CENTER, PApplet.CENTER);
    display.noStroke();
    display.textFont(KyUI.fontMain);
  }
  @Override
  boolean checkInvalid(Rect rect) {
    invalidated=true;
    return super.checkInvalid(rect);
  }
  public void resize(int width, int height) {
    if (width == 0 || height == 0) {
      return;
    }
    display.dispose();
    display=KyUI.Ref.createGraphics(width, height);
    onLayout();//FIX>> resize layout problem!!!
    invalidate();
  }
  public synchronized void renderReal(PGraphics g) {
    g.image(display, display.width / 2, display.height / 2);
  }
  public void clear() {
    display.beginDraw();
    display.clear();
    display.endDraw();
  }
}
