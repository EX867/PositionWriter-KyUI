package kyui.element;
import kyui.core.Attributes;
import kyui.core.Element;
import kyui.core.KyUI;
import kyui.event.listeners.MouseEventListener;
import kyui.util.ColorExt;
import kyui.util.Rect;
import kyui.util.Vector2;
import processing.core.PGraphics;
import processing.event.MouseEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
public class TabLayout extends Element {
  protected List<Element> tabs;//only contains TabButton...
  protected List<Element> contents;//content filled in empty space.
  public HashMap<Integer, Integer> idToIndex;
  //
  LinkedList<ModificationData> tasks=new LinkedList<ModificationData>();
  //pointers
  protected DivisionLayout linkLayout;
  protected LinearLayout tabLayout;
  protected FrameLayout contentLayout;
  //modifiable values
  public int tabColor1;//selected
  public int tabColor2;
  public int edgeSize=8;
  //protected modifiable values
  protected int tabSize;
  protected int rotation=Attributes.ROTATE_NONE;
  protected int buttonRotation=Attributes.ROTATE_NONE;
  protected int buttonEdgeRotation=Attributes.ROTATE_NONE;
  protected boolean enableX=true;
  //in-class values
  public int selection=0;
  //temp vars
  private int count=0;
  private Rect cacheRect=new Rect(0, 0, 0, 0);
  public TabLayout(String name, Rect pos_) {
    super(name);
    pos=pos_;
    init();
    idToIndex=new HashMap<Integer, Integer>(100);
    linkLayout=new DivisionLayout(getName() + ":linkLayout", pos);
    tabLayout=new LinearLayout(getName() + ":tabLayout");
    contentLayout=new FrameLayout(getName() + ":contentLayout");
    linkLayout.direction=Attributes.VERTICAL;
    addChild(linkLayout);
    linkLayout.addChild(tabLayout);
    linkLayout.addChild(contentLayout);
    tabs=tabLayout.children;
    contents=contentLayout.children;
    addTab_(0, "", new Element(getName() + ":default"));
    tabs.get(0).setEnabled(false);
    selectTab(0);//0 means no tab selected.
    localLayout();
    tabLayout.bgColor=KyUI.Ref.color(127);
    contentLayout.bgColor=KyUI.Ref.color(127);
    tabColor1=KyUI.Ref.color(0, 0, 255);
    tabColor2=KyUI.Ref.color(0, 0, 127);
  }
  private void init() {
    tabSize=38;
  }
  public void addTab_(int index, String text, Element content) {
    idToIndex.put(count, index);
    TabButton btn=new TabButton(getName() + ":" + count);
    btn.setPressListener(new TabLayoutPressListener(count));
    btn.text=text;
    btn.edgeColor=tabColor2;
    btn.rotation=buttonRotation;
    btn.edgeRotation=buttonEdgeRotation;
    count++;
    tabLayout.addChild(index, btn);
    contentLayout.addChild(index, content);
    content.setEnabled(false);
    //
    if (selection >= index) selectTab(selection + 1);
    for (int a=index; a < tabs.size(); a++) {
      int id2=((TabLayoutPressListener)((TabButton)tabs.get(a)).getPressListener()).id;
      idToIndex.put(id2, a);
    }
  }
  public void addTab(int index, String text, Element content) {
    tasks.add(new ModificationData(index + 1, text, content));
  }
  public void addTab(String text, Element content) {
    addTab(KyUI.INF, text, content);//add to last!!
  }
  public void removeTab_(int index) {
    if (index < 0 || index >= tabs.size() - 1) return;
    index+=1;
    Element content=contents.get(index);
    contentLayout.removeChild(content.getName());
    Element btn=tabs.get(index);
    tabLayout.removeChild(btn.getName());
    if (((TabButton)btn).getPressListener() == null) {
      System.err.println("adsf");
    }
    int id=idToIndex.get(((TabLayoutPressListener)((TabButton)btn).getPressListener()).id);
    idToIndex.remove(id);
    KyUI.removeElement(btn.getName());
    //
    if (selection == index) selectTab(0);
    else if (selection > index) selectTab(selection - 1);
    for (int a=index; a < tabs.size(); a++) {
      int id2=((TabLayoutPressListener)((TabButton)tabs.get(a)).getPressListener()).id;
      idToIndex.put(id2, a);
    }
    localLayout();
  }
  public void removeTab(int index) {
    tasks.add(new ModificationData(index));
  }
  class ModificationData {
    static final int ADD=1;
    static final int REMOVE=2;
    int type;
    int i;
    Element e;
    String s;
    public ModificationData(int i_, String s_, Element e_) {
      type=ADD;
      i=i_;
      e=e_;
      s=s_;
    }
    public ModificationData(int i_) {
      type=REMOVE;
      i=i_;
    }
    void execute() {
      if (type == ADD) {
        if (i > tabs.size() - 1) {
          i=tabs.size() - 1;
        }
        addTab_(i, s, e);
      } else if (type == REMOVE) {
        removeTab_(i);
      }
    }
  }
  @Override
  public void update() {
    while (tasks.size() != 0) {
      tasks.pollFirst().execute();
    }
  }
  class TabLayoutPressListener implements MouseEventListener {
    int id;
    public TabLayoutPressListener(int id_) {
      id=id_;
    }
    @Override
    public boolean onEvent(MouseEvent e, int index) {
      selectTab(index);
      return true;
    }
  }
  public void selectTab(int selection_) {
    if (selection_ < 0 || selection_ >= tabs.size()) return;
    if (selection >= 0 && selection < tabs.size()) {
      ((TabButton)tabs.get(selection)).edgeColor=tabColor2;
      contents.get(selection).setEnabled(false);
    }
    selection=selection_;
    ((TabButton)tabs.get(selection)).edgeColor=tabColor1;
    contents.get(selection).setEnabled(true);
    invalidate();
  }
  public void setMode(int mode) {
    tabLayout.setMode(mode);
    localLayout();
  }
  public void setTabSize(int size) {
    tabSize=size;
    localLayout();
  }
  public void setRotation(int rotation_) {
    rotation=rotation_;
    if (rotation == Attributes.ROTATE_DOWN || rotation == Attributes.ROTATE_NONE) {
      tabLayout.setDirection(Attributes.HORIZONTAL);
      linkLayout.direction=Attributes.VERTICAL;
    } else {
      tabLayout.setDirection(Attributes.VERTICAL);
      linkLayout.direction=Attributes.HORIZONTAL;
    }
    if (rotation == Attributes.ROTATE_RIGHT || rotation == Attributes.ROTATE_DOWN) {
      linkLayout.inverse=true;
    } else {
      linkLayout.inverse=false;
    }
    setButtonRotation(rotation);
  }
  public void setButtonRotation(int rotation_) {
    buttonRotation=rotation_;
    for (Element e : tabs) {
      ((TabButton)e).rotation=buttonRotation;
    }
    setButtonEdgeRotation(buttonRotation);
  }
  public void setButtonEdgeRotation(int rotation_) {
    buttonEdgeRotation=rotation_;
    for (Element e : tabs) {
      ((TabButton)e).edgeRotation=buttonEdgeRotation;
    }
    localLayout();
  }
  public void setEnableX(boolean v) {
    enableX=v;
  }
  @Override
  public void onLayout() {
    linkLayout.setPosition(pos);
    if (linkLayout.direction == Attributes.VERTICAL) linkLayout.ratio=(float)tabSize / (pos.bottom - pos.top);
    else linkLayout.ratio=(float)tabSize / (pos.right - pos.left);
    super.onLayout();
  }
  public int size() {
    int count=0;
    for (Element e : children) {
      if (e.isEnabled()) {
        count++;
      }
    }
    return count;
  }
  public class TabButton extends Button {
    int edgeColor;
    int edgeRotation=Attributes.ROTATE_NONE;
    Button xButton;
    public TabButton(String name) {
      super(name);
      init();
    }
    public TabButton(String name, Rect pos_) {
      super(name, pos_);
      init();
    }
    void init() {
      xButton=new Button(getName() + ":xButton");
      xButton.text="x";
      xButton.bgColor=ColorExt.brighter(bgColor, -10);
      xButton.textOffsetY=-textSize / 4;
      addChild(xButton);
      xButton.setPressListener(new TabXButtonListener(this));
    }
    public void render(PGraphics g) {
      textOffsetX=0;
      textOffsetY=0;
      if (edgeRotation == Attributes.ROTATE_NONE) {
        cacheRect.set(pos.left, pos.top, pos.right, pos.top + edgeSize);
        textOffsetY=edgeSize / 2;
      } else if (edgeRotation == Attributes.ROTATE_RIGHT) {
        cacheRect.set(pos.right - edgeSize, pos.top, pos.right, pos.bottom);
        textOffsetX=-edgeSize / 2;
      } else if (edgeRotation == Attributes.ROTATE_DOWN) {
        cacheRect.set(pos.left, pos.bottom - edgeSize, pos.right, pos.bottom);
        textOffsetY=-edgeSize / 2;
      } else if (edgeRotation == Attributes.ROTATE_LEFT) {
        cacheRect.set(pos.left, pos.top, pos.left + edgeSize, pos.bottom);
        textOffsetX=edgeSize / 2;
      }
      if (enableX) {
        if (rotation % 2 == 1) {
          textOffsetX-=(pos.bottom - pos.top) / 3;
        } else {
          textOffsetX-=(pos.right - pos.left) / 3;
        }
      }
      super.render(g);
      g.fill(edgeColor);
      cacheRect.render(g);
    }
    @Override
    public void onLayout() {
      int size;
      xButton.rotation=rotation;
      if (rotation % 2 == 1) {
        size=(pos.bottom - pos.top);
      } else {
        size=(pos.right - pos.left);
      }
      if (edgeRotation - rotation % 2 != 0) {
        size-=edgeSize;
      }
      size/=2;
      if (rotation == Attributes.ROTATE_NONE) {
        xButton.setPosition(new Rect(pos.right - size * 3 / 2, pos.top + size / 2, pos.right - size / 2, pos.bottom - size / 2));
      } else if (rotation == Attributes.ROTATE_RIGHT) {
        xButton.setPosition(new Rect(pos.left + size / 2, pos.bottom - size * 3 / 2, pos.right - size / 2, pos.bottom - size / 2));
      } else if (rotation == Attributes.ROTATE_DOWN) {
        xButton.setPosition(new Rect(pos.left + size / 2, pos.top + size / 2, pos.left + size * 3 / 2, pos.bottom - size / 2));
      } else if (rotation == Attributes.ROTATE_LEFT) {
        xButton.setPosition(new Rect(pos.left + size / 2, pos.top + size / 2, pos.right - size / 2, pos.top + size * 3 / 2));
      }
      if (edgeRotation == Attributes.ROTATE_NONE) {
        xButton.setPosition(new Rect(xButton.pos.left + edgeSize / 2, xButton.pos.top + edgeSize / 2, xButton.pos.right + edgeSize / 2, xButton.pos.bottom + edgeSize / 2));
      } else if (edgeRotation == Attributes.ROTATE_RIGHT) {
        xButton.setPosition(new Rect(xButton.pos.left - edgeSize, xButton.pos.top, xButton.pos.right - edgeSize, xButton.pos.bottom));
      } else if (edgeRotation == Attributes.ROTATE_DOWN) {
        xButton.setPosition(new Rect(xButton.pos.left - edgeSize / 2, xButton.pos.top - edgeSize / 2, xButton.pos.right - edgeSize / 2, xButton.pos.bottom - edgeSize / 2));
      } else if (edgeRotation == Attributes.ROTATE_LEFT) {
        xButton.setPosition(new Rect(xButton.pos.left, xButton.pos.top, xButton.pos.right, xButton.pos.bottom));
      }
      xButton.setEnabled(enableX);
      //super.onLayout();
    }
    @Override
    public Vector2 getPreferredSize() {
      if (!enableX) return super.getPreferredSize();
      if (rotation == Attributes.ROTATE_NONE || rotation == Attributes.ROTATE_DOWN) {
        return new Vector2(KyUI.Ref.textWidth(text) + padding * 2 + textSize, textSize + padding * 2);
      } else {
        return new Vector2(textSize + padding * 2, KyUI.Ref.textWidth(text) + padding * 2 + textSize);
      }
    }
    class TabXButtonListener implements MouseEventListener {
      TabButton Ref;
      public TabXButtonListener(TabButton t) {
        Ref=t;
      }
      public boolean onEvent(MouseEvent e, int index) {
        removeTab(tabs.indexOf(Ref) - 1);
        return false;
      }
    }
  }
}
