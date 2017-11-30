package kyui.element;
import kyui.core.Attributes;
import kyui.core.Element;
import kyui.core.KyUI;
import kyui.event.ItemSelectListener;
import kyui.event.MouseEventListener;
import kyui.event.EventListener;
import kyui.task.Task;
import kyui.util.ColorExt;
import kyui.util.Rect;
import kyui.util.Vector2;
import processing.core.PGraphics;
import processing.event.MouseEvent;
public class LinearList extends Element {
  int strokeWeight=4;
  protected DivisionLayout linkLayout;
  protected LinearLayout listLayout;
  protected RangeSlider slider;
  protected ItemSelectListener selectListener;
  protected SelectableButton selection=null;
  protected SelectableButton pressItem;
  int count=0;
  //modifiable values
  public int sliderSize;
  public int fgColor;
  public Attributes.Direction direction=Attributes.Direction.VERTICAL;
  //temp values
  Rect cacheRect=new Rect();
  public LinearList(String name) {
    super(name);
    init();
  }
  public LinearList(String name, Rect pos_) {
    super(name);
    pos=pos_;
    init();
  }
  private void init() {
    margin=strokeWeight / 2;
    sliderSize=14;
    linkLayout=new DivisionLayout(getName() + ":linkLayout", pos);
    linkLayout.rotation=Attributes.Rotation.RIGHT;
    listLayout=new LinearLayout(getName() + ":listLayout");
    slider=new RangeSlider(getName() + ":slider");
    linkLayout.addChild(listLayout);
    linkLayout.addChild(slider);
    listLayout.setDirection(Attributes.Direction.VERTICAL);
    listLayout.setMode(LinearLayout.Behavior.FIXED);
    listLayout.padding=strokeWeight;
    slider.margin=0;
    listLayout.setAdjustListener(new EventListener() {
      @Override
      public void onEvent(Element e) {
        setSlider();
      }
    });
    slider.setAdjustListener(new EventListener() {
      @Override
      public void onEvent(Element e) {
        setList();
        listLayout.invalidate();
      }
    });
    bgColor=KyUI.Ref.color(127);
    fgColor=50;
    addChild(linkLayout);
  }
  public void addItem(String text) {
    SelectableButton btn=new SelectableButton(getName() + ":" + count, this);
    btn.text=text;
    listLayout.addChild(btn);
    count++;
    afterModify();
  }
  public void addItem(int index, String text) {
    SelectableButton btn=new SelectableButton(getName() + ":" + count, this);
    btn.text=text;
    listLayout.addChild(index, btn);
    count++;
    afterModify();
  }
  public void addItem(SelectableButton e) {
    listLayout.addChild(e);
    e.Ref=this;
    afterModify();
  }
  public void addItem(int index, SelectableButton e) {
    listLayout.addChild(index, e);
    e.Ref=this;
    afterModify();
  }
  public void removeItem(int index) {
    if (index < 0 || index >= size()) return;
    listLayout.removeChild(index);
    afterModify();
  }
  public void removeItem(String name) {
    listLayout.removeChild(listLayout.children.indexOf(name));
    afterModify();
  }
  public void setSelectListener(ItemSelectListener l) {
    selectListener=l;
  }
  public void setFixedSize(int size) {//only works on fixed mode.
    listLayout.setFixedSize(size);
    localLayout();
  }
  @Override
  public synchronized void onLayout() {
    if (direction == Attributes.Direction.VERTICAL) {
      linkLayout.value=sliderSize;
      linkLayout.rotation=Attributes.Rotation.RIGHT;
    } else {
      linkLayout.value=sliderSize;
      linkLayout.rotation=Attributes.Rotation.DOWN;
    }
    listLayout.setDirection(direction);
    slider.direction=direction;
    linkLayout.setPosition(pos);
    setList();
  }
  @Override
  public void render(PGraphics g) {
    g.strokeWeight(strokeWeight);
    g.stroke(fgColor);
    g.noFill();
    pos.render(g);
    g.fill(bgColor);
    listLayout.pos.render(g);
    //slider.pos.render(g);
    g.noStroke();
  }
  @Override
  public boolean mouseEventIntercept(MouseEvent e) {
    if (e.getAction() == MouseEvent.PRESS) {
      pressItem=null;
    }
    return true;
  }
  @Override
  public void startDrop(MouseEvent e, int index) {
    if (pressItem != null) {
      KyUI.dropStart(this, e, index, pressItem.getName(), pressItem.text);
    }
  }
  void afterModify() {
    KyUI.taskManager.addTask(new Task() {
      @Override
      public void execute(Object data) {
        setList();
        slider.setOffset(listLayout.getTotalSize(), listLayout.offset);
      }
    }, null);
  }
  void setSlider() {//when move slider
    setSliderLength();
    slider.setOffset(listLayout.getTotalSize(), listLayout.offset);
  }
  void setList() {//when move list
    //list.totalSize is from onLayout.
    setSliderLength();
    listLayout.setOffset(slider.getOffset(listLayout.getTotalSize()));
    listLayout.localLayout();
  }
  void setSliderLength() {
    if (direction == Attributes.Direction.VERTICAL) {
      slider.setLength(listLayout.getTotalSize(), pos.bottom - pos.top);
    } else {
      slider.setLength(listLayout.getTotalSize(), pos.right - pos.left);
    }
  }
  public static class SelectableButton extends Button {//parent_max=1;
    //modifiable values
    boolean selected=false;
    LinearList Ref;
    public SelectableButton(String name, LinearList Ref_) {
      super(name);
      Ref=Ref_;
      init();
    }
    public SelectableButton(String name, Rect pos_, LinearList Ref_) {
      super(name, pos_);
      Ref=Ref_;
      init();
    }
    private void init() {
      setPressListener(new ListItemPressListener(this));
    }
    @Override
    public void render(PGraphics g) {
      float height=(pos.bottom - pos.top);
      float overlap=height;
      if (Ref.pos.top > pos.top) {//up overlap
        overlap=(height - Ref.pos.top + pos.top);
      } else if (Ref.pos.bottom < pos.bottom) {//down overlap
        overlap=(height + Ref.pos.bottom - pos.bottom);
      }
      if (bgColor != 0) {
        int c=getDrawBgColor(g);
        if (selected) {//for identification...
          c=(ColorExt.brighter(bgColor, 40));
        } else {
          c=ColorExt.scale(c, overlap / height);
        }
        g.fill(c);
        pos.render(g);
      }
      if (Ref.pos.top > pos.top) {//up overlap
        textOffsetY=(int)(Ref.pos.top - pos.top) / 2;
      } else if (Ref.pos.bottom < pos.bottom) {//down overlap
        textOffsetY=(int)(Ref.pos.bottom - pos.bottom) / 2;
      } else {
        textOffsetY=0;
      }
      if (overlap > textSize && overlap > 0) {//this can not be correct.
        g.fill(ColorExt.scale(textColor, overlap / height));
        g.textSize(textSize);
        g.pushMatrix();
        g.translate((pos.left + pos.right) / 2 + textOffsetX, (pos.top + pos.bottom) / 2 + textOffsetY);
        for (int a=1; a <= rotation.ordinal(); a++) {
          g.rotate(KyUI.Ref.radians(90));
        }
        if (overlap < height && overlap > 0) {
          g.scale(1, (overlap / height));
        }
        g.text(text, 0, 0);
        g.popMatrix();
      }
    }
    @Override
    public boolean mouseEvent(MouseEvent e, int index) {
      if (e.getAction() == MouseEvent.PRESS) {
        Ref.pressItem=this;
      }
      return super.mouseEvent(e, index);
    }
    class ListItemPressListener implements MouseEventListener {//why!!! why you don't make MouseEvent "Press Action" Listener for button...
      SelectableButton Ref2;
      public ListItemPressListener(SelectableButton Ref2_) {
        Ref2=Ref2_;
      }
      @Override
      public boolean onEvent(MouseEvent e, int index) {
        if (Ref.selection != null) {
          Ref.selection.selected=false;
          Ref.selection.invalidate();
        }
        selected=true;
        Ref.selection=Ref2;
        if (Ref.selectListener != null) {
          Ref.selectListener.onEvent(index);
        }
        invalidate();
        return false;
      }
    }
  }
  //these inspector classes exists here because I will use it!
  static class InspectorButton extends SelectableButton {
    //modifiable values
    public float ratio=3.0F;//this is inspection button width by height.
    public InspectorButton(String name, LinearList Ref_) {
      super(name, Ref_);
    }
    @Override
    public void render(PGraphics g) {
      g.textAlign(KyUI.Ref.LEFT, KyUI.Ref.CENTER);
      textOffsetX=(int)(-(pos.right - pos.left) / 2 + padding);
      super.render(g);
      g.textAlign(KyUI.Ref.CENTER, KyUI.Ref.CENTER);
    }
    @Override
    public void onLayout() {
      float padding2=(pos.bottom - pos.top) / 6;
      float left=padding2;
      float width=(pos.bottom - pos.top) * ratio;
      for (Element child : children) {//just works like horizontal LinearList...
        child.setPosition(child.pos.set(pos.right - left - width + padding2, pos.top + padding2, pos.right - left, pos.bottom - padding2));
        left+=width + padding2;
      }
      if (Ref.direction == Attributes.Direction.HORIZONTAL) {//FIX>> not horizontal
        for (Element child : children) {
          child.setActive(false);
          child.setVisible(false);
        }
      }
    }
  }
  public static class InspectorColorButton extends InspectorButton {
    public ColorButton colorButton;
    public InspectorColorButton(String name, LinearList Ref_) {
      super(name, Ref_);
      init();
    }
    private void init() {
      colorButton=new ColorButton(getName() + ":colorButton");
      colorButton.bgColor=KyUI.Ref.color(127);
      colorButton.setPressListener(new ColorButton.OpenColorPickerEvent(colorButton));//auto for picking and storing color
      colorButton.c=KyUI.Ref.color((int)(Math.random() * 0xFF), (int)(Math.random() * 0xFF), (int)(Math.random() * 0xFF), 255);//FIX>>temporary
      addChild(colorButton);
    }
  }
  public static class InspectorTextButton extends InspectorButton {
    public TextBox textBox;//get this TextBox directly and you can modify this.
    public InspectorTextButton(String name, LinearList Ref_) {
      super(name, Ref_);
      init();
    }
    private void init() {
      textBox=new TextBox(getName() + ":texBox");
      textBox.setNumberOnly(false);
      addChild(textBox);
    }
  }
  public static class InspectorImageButton extends InspectorButton {
    public ImageDrop imageDrop;//get this TextBox directly and you can modify this.
    public InspectorImageButton(String name, LinearList Ref_) {
      super(name, Ref_);
      init();
    }
    private void init() {
      imageDrop=new ImageDrop(getName() + ":imageDrop");
      addChild(imageDrop);
    }
  }
  @Override
  public Vector2 getPreferredSize() {
    return new Vector2(pos.right - pos.left, listLayout.fixedSize * listLayout.children.size());
  }
  @Override
  public int size() {
    return listLayout.size();
  }
}
