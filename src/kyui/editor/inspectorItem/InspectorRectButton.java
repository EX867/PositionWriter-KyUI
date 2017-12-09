package kyui.editor.inspectorItem;
import kyui.element.TextBox;
import kyui.util.Rect;
public class InspectorRectButton extends InspectorButton<Rect> {
  Rect cacheRect=new Rect();
  public TextBox[] ltrb=new TextBox[4];
  public InspectorRectButton(String name) {
    super(name);
    init();
  }
  private void init() {
    ratio=1.3F;
    ltrb[0]=new TextBox(getName() + ":x1");
    ltrb[1]=new TextBox(getName() + ":y1");
    ltrb[2]=new TextBox(getName() + ":x2");
    ltrb[3]=new TextBox(getName() + ":y2");
    for (TextBox t : ltrb) {
      t.padding=t.padding / 2;
      t.setNumberOnly(TextBox.NumberType.FLOAT);
    }
    for (TextBox t : ltrb) {
      addChild(t);
    }
  }
  @Override
  public void set(Rect value) {
    ltrb[0].setText("" + value.left);
    ltrb[1].setText("" + value.top);
    ltrb[2].setText("" + value.right);
    ltrb[3].setText("" + value.bottom);
  }
  @Override
  public Rect get() {
    return cacheRect.set(ltrb[0].valueF, ltrb[1].valueF, ltrb[2].valueF, ltrb[3].valueF);
  }
}