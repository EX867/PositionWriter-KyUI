package kyui.core;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import kyui.event.DropEventListener;
import kyui.event.EventListener;
import kyui.event.FileDropEventListener;
import kyui.event.ResizeListener;
import kyui.util.Task;
import kyui.util.TaskManager;
import kyui.util.Rect;
import kyui.util.Vector2;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.event.Event;
import processing.event.MouseEvent;
import processing.event.KeyEvent;
import sojamo.drop.*;
//===To ADD list===//
//ADD>>optimize mouseEvent and rendering chain!! especially clipping...
//ADD>>search elements in editor (later)
//ADD>>drag and drop overlay !!!**
public class KyUI {
  //
  public static PApplet Ref;
  private static boolean ready=false;
  private static boolean end=false;
  // object control
  protected static final HashMap<String, Element> Elements=new HashMap<String, Element>();
  protected static LinkedList<CachingFrame> roots=new LinkedList<CachingFrame>();//no support multi window.
  public static Element focus=null;
  // events
  public static LinkedList<Event> EventQueue=new LinkedList<Event>();//items popped from update thread.
  public static TaskManager taskManager=new TaskManager();//do it in here... add task directly to here. (in Element)
  static class ModifyLayerTask implements Task {
    @Override
    public void execute(Object data_raw) {
      if (data_raw == null) {//remove
        roots.pollLast();
        if (roots.size() == 0) {
          System.err.println("there is some error!");
          return;
        }
        roots.peekLast().renderFlag=true;
      } else {
        roots.addLast((CachingFrame)data_raw);
        addElement((Element)data_raw);
      }
    }
  }
  static ModifyLayerTask modifyLayerTask=new ModifyLayerTask();//task for this object.
  static LinkedList<ResizeListener> resizeListeners=new LinkedList<>();
  //
  public static final int STATE_PRESS=1;
  public static final int STATE_PRESSED=2;
  public static final int STATE_RELEASE=3;
  public static final int STATE_RELEASED=4;
  public static int mouseState=STATE_RELEASED;//no multi touch
  public static int mouseTime=0;// this parameter indicates the time after mouse clicked.
  public static Vector2 mouseClick=new Vector2();// this parameter stores mouse click position.
  public static Vector2 mouseGlobal=new Vector2();// this parameter stores global mouse position.(scaled)
  public static final int GESTURE_THRESHOLD=13;
  public static final int WHEEL_COUNT=25;
  public static HashMap<String, DropEventListener> dropEvents=new HashMap<String, DropEventListener>();//dnd - internal
  public static DropMessenger dropMessenger;//
  public static CachingFrame dropLayer;
  public static HashMap<String, FileDropEventListener> dropEventsExternal=new HashMap<String, FileDropEventListener>();//dnd - external
  static CachingFrame dropLayerExternal;
  static SDrop drop;//drop from outside is handled not like drop between elements...this is my limit of abstraction...
  public static boolean draggingExternal=false;//used when checking this moueEvent is external drag and drop.
  static class CheckOverlayTask implements Task {
    @Override
    public void execute(Object data_) {
      if (data_ instanceof DropEvent) {
        DropEvent de=(DropEvent)data_;
        mouseGlobal.set(de.x() / scaleGlobal, de.y() / scaleGlobal);
        Element target=roots.getLast().checkOverlay(mouseGlobal.x, mouseGlobal.y);//if overlay, setup overlay.
        if (target != null) {
          dropEventsExternal.get(target.getName()).onEvent(de);
        }
      }
    }
  }
  static CheckOverlayTask checkOverlayTask=new CheckOverlayTask();
  //
  public static int KEY_INIT_DELAY=1000;// you can change this valueI.
  public static int KEY_INTERVAL=300;
  public static boolean ctrlPressed=false;
  public static boolean shiftPressed=false;
  public static boolean altPressed=false;
  public static boolean keyState=false;
  public static long keyTime=0;
  public static boolean keyInit=false;// this used on textEdit and etc...
  protected static List<Long> reflectedPressedKeys;
  // shortcuts
  public static HashMap<String, Shortcut> shortcutsByName=new HashMap<String, Shortcut>(199);
  public static LinkedList<Shortcut> shortcuts=new LinkedList<Shortcut>();
  // graphics
  public static PGraphics cacheGraphics;
  public static LinkedList<Vector2> transform=new LinkedList<>();
  static {
    transform.add(new Vector2());
  }
  public static LinkedList<Rect> clipArea=new LinkedList<Rect>();
  public static long drawStart=0;// these 3 parameters used to measure elapsed time.
  public static long drawEnd=0;
  public static long drawInterval=0;
  public static float scaleGlobal=1.0F;// this parameter stores global window scale.
  public static PFont fontMain;//set manually! (so public)
  public static PFont fontText;//set manually! (2)
  public static int INF=987654321;
  private static Rect clipRect=new Rect();
  //thread
  public static Thread updater;
  public static int updater_interval;
  public static long frameCount;
  //public Thread animation;
  //temp
  private static int pwidth;
  private static int pheight;
  private static int count=0;
  private KyUI() {//WARNING! names must not contains ':' and "->".
  }
  public static void start(PApplet ref) {
    start(ref, 30);
  }
  @SuppressWarnings("unchecked")
  public static void start(PApplet ref, int rate) {
    if (ready) return;// this makes setup() only called once.
    Ref=ref;
    //SDrop set
    try {
      Field f=Ref.getSurface().getClass().getDeclaredField("canvas");
      f.setAccessible(true);//Very important, this allows the setting to work.
      java.awt.Component cp=(java.awt.Canvas)f.get(Ref.getSurface());
      drop=new SDrop(cp);
      drop.addDropListener(new DropListener() {
        @Override
        public void dragExit() {
          draggingExternal=false;
          handleEvent(new MouseEvent(null, 0, MouseEvent.MOVE, 0, -1, -1, PApplet.LEFT, 1));
        }
        @Override
        public void update(float x, float y) {
          draggingExternal=true;
          handleEvent(new MouseEvent(null, 0, MouseEvent.MOVE, 0, (int)(x / scaleGlobal), (int)(y / scaleGlobal), PApplet.LEFT, 1));
        }
        @Override
        public void dropEvent(DropEvent de) {
          draggingExternal=false;//not synchronized with updater so release don't works, but it is real time!
          handleEvent(new MouseEvent(null, 0, MouseEvent.RELEASE, 0, (int)(de.x() / scaleGlobal), (int)(de.y() / scaleGlobal), PApplet.LEFT, 1));
          taskManager.addTask(checkOverlayTask, de);
        }
      });
      cp.addComponentListener(new ComponentListener() {
        @Override
        public void componentResized(ComponentEvent e) {
          try {
            synchronized (updater) {
              //System.out.println(e.getComponent().getSize().width+" "+e.getComponent().getSize().height);
              resize(e.getComponent().getSize().width, e.getComponent().getSize().height);
              KyUI.taskManager.executeAll();
            }
          } catch (NullPointerException ex) {
            ex.printStackTrace();
          }
        }
        @Override
        public void componentMoved(ComponentEvent e) {
        }
        @Override
        public void componentShown(ComponentEvent e) {
        }
        @Override
        public void componentHidden(ComponentEvent e) {
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
    pwidth=Ref.width;
    pheight=Ref.height;
    //other things
    fontMain=KyUI.Ref.createFont(new java.io.File("data/SourceCodePro-Bold.ttf").getAbsolutePath(), 20);
    fontText=KyUI.Ref.createFont(new java.io.File("data/The160.ttf").getAbsolutePath(), 20);
    cacheGraphics=KyUI.Ref.createGraphics(10, 10);//small graphics...used for some functions
    dropLayer=new CachingFrame("KyUI:dropLayer", new Rect(0, 0, Ref.width, Ref.height));
    if (roots.size() == 0)
      addLayer(getNewLayer());
    try {
      Field pressedKeys;
      pressedKeys=PApplet.class.getDeclaredField("pressedKeys");
      pressedKeys.setAccessible(true);
      reflectedPressedKeys=(List<Long>)pressedKeys.get(Ref);
    } catch (Exception e) {
      e.printStackTrace();
    }
    ready=true;
    updater=new Thread(new Updater());
    frameRate(rate);
    frameCount=0;
    updater.start();
  }
  public static void end() {
    end=true;
  }
  public static void resize(int w, int h) {
    for (CachingFrame root : roots) {
      root.pos.set(0, 0, w, h);
      root.resize(w, h);
    }
    for (ResizeListener resizeListener : resizeListeners) {
      resizeListener.onEvent(w, h);
    }
  }
  public static void addResizeListener(ResizeListener e) {
    resizeListeners.add(e);
  }
  public static boolean isRootPresent(CachingFrame e) {
    return roots.contains(e);
  }
  public static void frameRate(int rate) {//update thread frame rate.
    updater_interval=1000 / rate;
  }
  public static void addLayer(CachingFrame root) {
    root.clear();
    taskManager.addTask(modifyLayerTask, root);
  }
  public static void removeLayer() {
    taskManager.executeAll();
    taskManager.addTask(modifyLayerTask, null);
  }
  public static CachingFrame getNewLayer() {
    return new CachingFrame("KyUI:" + count++, new Rect(0, 0, Ref.width, Ref.height));
  }
  protected static void addElement(Element e) {
    if (!Elements.containsKey(e.name)) {
      Elements.put(e.name, e);
    } else {
      if (e != Elements.get(e.name) && !e.name.equals("KyUI:messenger")) {//messenger always share same name...
        throw new RuntimeException("[KyUI] try to add existing name. (" + e.name + ") type : " + e.getClass().getTypeName() + ", exists : " + Elements.get(e.name).getClass().getTypeName());
      }
    }
  }
  public static boolean rename(Element e, String name) {
    if (e.name.equals(name)) {
      return true;
    }
    if (!Elements.containsKey(name)) {
      Elements.remove(e.name);
      e.name=name;
      Elements.put(name, e);
      return true;
    } else {
      System.err.println("[KyUI] try to rename to existing name. (" + e.name + " to " + name + ")");
      return false;
    }
  }
  public static void removeElement(String name) {
    Elements.remove(name);
  }
  public static void add(Element object) {
    taskManager.executeAll();//because this need latest state.
    roots.getLast().addChild(object);
  }
  public static Element get(String name) {
    taskManager.executeAll();//because this need latest state.
    return Elements.get(name);
  }
  public static <Type> Type get2(String name) {
    taskManager.executeAll();//because this need latest state.
    return (Type)Elements.get(name);
  }
  // called by processing animation thread
  public static void render(PGraphics g) {
    drawStart=drawEnd;
    g.imageMode(PApplet.CENTER);
    synchronized (updater) {
      for (CachingFrame root : roots) {
        root.renderReal(g);//render all...
      }
    }
    drawEnd=System.currentTimeMillis();
    drawInterval=drawEnd - drawStart;
  }
  static class Updater implements Runnable {
    @Override
    public void run() {
      cacheGraphics.beginDraw();
      while (!end) {
        synchronized (updater) {
          //empty EventQueue.
          while (EventQueue.size() > 0) {
            Event e=EventQueue.pollFirst();
            if (e instanceof KeyEvent) {
              keyEvent((KeyEvent)e);
            } else if (e instanceof MouseEvent) {
              mouseEvent((MouseEvent)e);
            }
          }
          KyUI.taskManager.executeAll();
          for (int a=0; a < roots.size(); a++) {
            roots.getLast().render_(null);
          }
          roots.getLast().update_();
        }
        frameCount++;
        try {
          Thread.sleep(updater_interval);
        } catch (InterruptedException e) {
        }
        /*if (pwidth != Ref.width || pheight != Ref.height) {//resizing
          pwidth=Ref.width;
          pheight=Ref.height;
          resize();
          KyUI.taskManager.executeAll();
          //try {//resizing is heavy action.
          //  Thread.sleep(200);//wait 0.2s to not make cpu hot!
          //} catch (InterruptedException e) {
          //}
        }*/
      }
      cacheGraphics.endDraw();
    }
  }
  //
  public static void handleEvent(Event e) {
    EventQueue.addLast(e);
  }
  static void keyEvent(KeyEvent e) {// FIX >> This code is unstable. test and fix!
    if (Ref.key == PApplet.ESC) {
      Ref.key=0; // Fools! don't let them escape!
    }
    if (e.getKey() == PApplet.CODED) {
      if (e.getAction() == KeyEvent.PRESS) {//handle first.
        if (e.getKeyCode() == PApplet.CONTROL) ctrlPressed=true;//???
        else if (e.getKeyCode() == PApplet.SHIFT) shiftPressed=true;
        else if (e.getKeyCode() == PApplet.ALT) altPressed=true;
      } else if (e.getAction() == KeyEvent.RELEASE) {
        if (e.getKeyCode() == PApplet.CONTROL) ctrlPressed=false;
        else if (e.getKeyCode() == PApplet.SHIFT) shiftPressed=false;
        else if (e.getKeyCode() == PApplet.ALT) altPressed=false;
      }
    }
    if (e.getAction() == KeyEvent.PRESS) {
      for (Shortcut shortcut : shortcuts) {
        if (shortcut.isPressed(e)) {
          if (shortcut.event != null) {
            shortcut.event.onEvent(focus);//??? nothing to send...
          }
        }
      }
    }
    roots.getLast().keyEvent_((KeyEvent)e);
    if (e.getAction() == KeyEvent.PRESS) {
      roots.getLast().keyTyped_((KeyEvent)e);
      keyInit=false;
      keyState=true;
      keyTime=System.currentTimeMillis();
    } else {
      if (!keyState) {
        roots.getLast().keyTyped_((KeyEvent)e);
        keyState=true;
      }
    }
    if (keyInit) {
      if (System.currentTimeMillis() - keyTime > KEY_INTERVAL) {
        keyState=false;
        keyTime=System.currentTimeMillis();
      }
    } else {
      if (System.currentTimeMillis() - keyTime > KEY_INIT_DELAY) {
        keyInit=true;
        keyState=false;
        keyTime=System.currentTimeMillis();
      }
    }
    if (e.getAction() == KeyEvent.RELEASE) {
      if (reflectedPressedKeys.isEmpty()) {
        keyState=false;
        keyInit=false;
        keyTime=0;
      }
    }
    //updater.interrupt();
  }
  static void mouseEvent(MouseEvent e) {
    mouseGlobal.set(Ref.mouseX / scaleGlobal, Ref.mouseY / scaleGlobal);
    if (Ref.mousePressed) {
      if (mouseState == STATE_PRESS) mouseState=STATE_PRESSED;
      if (mouseState == STATE_RELEASE || mouseState == STATE_RELEASED) {
        mouseState=STATE_PRESS;
        mouseClick.set(mouseGlobal.x, mouseGlobal.y);
      }
    } else {
      if (mouseState == STATE_RELEASE) mouseState=STATE_RELEASED;
      if (mouseState == STATE_PRESS || mouseState == STATE_PRESSED) mouseState=STATE_RELEASE;
    }
    if (e.getAction() == MouseEvent.EXIT) {
      mouseGlobal.set(-1, -1);//make no element contains this.
    }
    roots.getLast().mouseEvent_(e, roots.size() - 1, true);
    //if (dropMessenger != null && e.getAction() == MouseEvent.RELEASE) {
    //}
    //int a=roots.size() - 1;
    //while (a >= 0 && roots.get(a).mouseEvent_(e, a)) {
    //  a--;
    //}
    //updater.interrupt();
  }
  //
  public static void changeLayout() {
    taskManager.executeAll();
    roots.getLast().onLayout();
    roots.getLast().invalidate();
  }
  public static void invalidate(Rect rect) {//adjust renderFlag.
    if (roots.isEmpty()) return;
    roots.getLast().checkInvalid(rect);
  }
  public static void invalidateElement(Element e) {//adjust renderFlag.
    if (roots.isEmpty()) return;
    roots.getLast().invalidated=true;
    e.renderFlag=true;
  }
  public static void clipRect(PGraphics g, Rect rect) {
    g.imageMode(PApplet.CORNERS);
    clipRect.set(rect);
    if (clipArea.size() > 0) {
      clipRect=Rect.getIntersection(rect, clipArea.getLast().translate(transform.getLast().x, transform.getLast().y), clipRect);
    }
    g.clip(clipRect.left, clipRect.top, clipRect.right, clipRect.bottom);
    clipArea.add(rect.set(clipRect));
  }
  public static void removeClip(PGraphics g) {
    if (clipArea.size() > 0) {
      clipArea.removeLast();
      if (clipArea.size() == 0) {
        g.noClip();
      } else {
        g.noClip();
        Rect last=clipArea.getLast().translate(transform.getLast().x, transform.getLast().y);
        g.imageMode(PApplet.CORNERS);
        g.clip(last.left, last.top, last.right, last.bottom);
      }
    }
  }
  public static void test(PGraphics g) {
    clipRect.render(g);
  }
  public static void transform(float x, float y) {//transforms clipArea and mouseEvent
    transform.addLast(new Vector2(x, y).addAssign(transform.getLast()));
  }
  public static void restore() {
    transform.removeLast();
  }
  public static void dropStart(Element start_, MouseEvent startEvent_, int startIndex_, String message_, String displayText_) {
    dropMessenger=new DropMessenger("KyUI:messenger", start_, startEvent_, startIndex_, message_, displayText_);
    dropMessenger.setVisual(start_.dropVisual);
    dropLayer.addChild(dropMessenger);
    addLayer(dropLayer);
  }
  public static void dropEnd(Element end, MouseEvent endEvent, int endIndex) {
    if (!end.droppableEnd) return;//this is : ignoring. so please check this thing
    if (dropMessenger != null) {
      if (end.droppableEnd) {
        if (getDropEvent(end) != null) {
          dropMessenger.onEvent(end, endEvent, endIndex);
        }
      }
      dropMessenger=null;
    }
  }
  public static void addDragAndDrop(Element start, Element end, DropEventListener listener) {
    dropEvents.put(start.getName() + "->" + end.getName(), listener);
    start.droppableStart=true;
    end.droppableEnd=true;
  }
  public static void addDragAndDrop(Element end, FileDropEventListener listener) {
    dropEventsExternal.put(end.getName(), listener);
  }
  public static DropEventListener getDropEvent(Element end) {
    if (dropMessenger == null) return null;
    return dropEvents.get(dropMessenger.start.getName() + "->" + end.getName());
  }
  public static class Shortcut {
    public boolean ctrl=false;
    public boolean alt=false;
    public boolean shift=false;
    public int key=-1;
    public int keyCode=0;
    //
    public EventListener event;
    public String name="nothing";
    public boolean custom=false;
    public Shortcut(String name_, boolean ctrl_, boolean alt_, boolean shift_, int key_, int keyCode_, EventListener event_, boolean custom_) {
      name=name_;
      set(ctrl_, alt_, shift_, key_, keyCode_, event_, custom_);
    }
    public void set(boolean ctrl_, boolean alt_, boolean shift_, int key_, int keyCode_, EventListener event_, boolean custom_) {
      ctrl=ctrl_;
      alt=alt_;
      shift=shift_;
      key=key_;
      keyCode=keyCode_;
      event=event_;
      custom=custom_;
    }
    boolean isPressed(KeyEvent e) {
      return (ctrl == ctrlPressed && alt == altPressed && shift == shiftPressed && e.getKey() == key && e.getKeyCode() == keyCode);
    }
    @Override
    public String toString() {
      String ret=name + "   [";
      if (ctrl) ret+="Ctrl+";
      if (alt) ret+="Alt+";
      if (shift) ret+="Shift+";
      //set key.
      int realKey=key;
      if (ctrl && (1 <= key && key <= 26)) {
        realKey=key - 97 + 1;
      } else if ((!ctrl) && shift && 65 <= key && key <= 90) {
        realKey=key - 97 + 65;
      }
      if (key == '\t') ret+="Tab";
      else if (key == ' ') ret+="Space";
      else if (key == PApplet.ENTER) ret+="Enter";
      else if (key == PApplet.BACKSPACE) ret+="Backspace";
      else if (key == PApplet.DELETE) ret+="Delete";
      else if (key == PApplet.ESC) ret+="ESC (None)";
      else if (key == PApplet.CODED) {
        if (keyCode == PApplet.UP) ret+="Up";
        else if (keyCode == PApplet.DOWN) ret+="Down";
        else if (keyCode == PApplet.RIGHT) ret+="Right";
        else if (keyCode == PApplet.LEFT) ret+="Left";
        else if (java.awt.event.KeyEvent.VK_F1 <= keyCode && keyCode <= java.awt.event.KeyEvent.VK_F12) {
          ret+="F" + (int)(keyCode - java.awt.event.KeyEvent.VK_F1 + 1);
        } else ret+=(char)keyCode;
      } else ret+="" + (char)realKey;
      return ret;
    }
    String toXmlString() {
      return "<shortcut ctrl=\"" + ctrl + "\" alt=\"" + alt + "\" shift=\"" + shift + "\" key=\"" + key + "\" keycode=\"" + keyCode + "\">" + name + "</shortcut>";
    }
  }
  public static void addShortcut(Shortcut shortcut) {
    shortcutsByName.put(shortcut.name, shortcut);
    shortcuts.add(shortcut);
  }
}
