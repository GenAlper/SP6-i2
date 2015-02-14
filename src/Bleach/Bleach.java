package Bleach;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import Bleach.InputManager.Receptionist;
import Bleach.InputManager.Receptionist.KeyBinding;
import Bleach.Loader.Discette;
import Bleach.PhysicsEngine.Physique;
import Bleach.Renderer.Picasso;

public class Bleach extends JPanel {
    // Pointer to the active level.
    private static Level activeLevel;

    // FPS limiter, limits how often the game is rendered.
    private static double FPS = 60;
    // A handle to the window.
    private static JFrame jWindow;
    // All the levels.
    private static Map<String, Level> levels = new HashMap<>();
    // A (set of) bool to see if the game is paused by any subsystem.
    private static Map<PauseType, Boolean> pause = new HashMap<>();

    private static Receptionist receptionist = null;

    private static Picasso renderer;

    private static long timeDebug;

    // Used for delta-time in the game loop (e.g. FPS limiting)
    private static double timePreviousLoop;

    // Used for delta-time in the rendering (e.g. calculating actual rendering
    // FPS)
    private static double timePreviousRender;

    private static int winHeight;
    private static String winTitle;
    private static int winWidth;

    public Bleach() {

	// Let's try to HW-accelerate stuff.
	System.setProperty("sun.java2d.opengl", "True");

	timePreviousLoop = timePreviousRender = System.currentTimeMillis();
	winWidth = 800; // Default width
	winHeight = 600; // Default height
	winTitle = "Game window"; // Default title;
    }

    public static Sprite getSprite(String key) {
	return Discette.getImage(key);
    }

    public static BufferedImage getTexture(String key) {
	Sprite sprite = Discette.getImage(key);

	return sprite == null ? null : sprite.getFrame();
    }

    public static void loadImages(String assetJsonPath) {
	Discette.loadImages(assetJsonPath);
    }

    public static Discette.JsonObjectLevel loadLevel(String assetJsonPath) {
	return Discette.loadLevel(assetJsonPath);
    }

    public static void loadSounds(String assetJsonPath) throws IOException, UnsupportedAudioFileException {
	Discette.loadSound(assetJsonPath);
    }

    private void gameLoop() {

	boolean quit = false;
	boolean paused = false;
	double deltaTime;

	while (!quit) {
	    deltaTime = System.currentTimeMillis() - timePreviousLoop;

	    // Simulate work
	    while (System.currentTimeMillis() - timePreviousLoop < 34) {
		Thread.yield();
	    }

	    if (!isPaused()) {
		/* Physics engine */
		Physique.step(activeLevel);

		/* Let's iterate entities and tick() and/or delete them */
		Iterator<EntityTranslatable> iter;

		/* Projectiles heartbeat */
		iter = activeLevel.getProjectiles().iterator();
		EntityTranslatable projectile;
		while (iter.hasNext()) {
		    projectile = iter.next();
		    if (projectile.isDead()) {
			iter.remove();
		    } else {
			((Entity) projectile).tick(activeLevel);
		    }
		}

		/* Mobiles heartbeat */
		iter = activeLevel.getMobiles().iterator();
		EntityTranslatable mobile;
		while (iter.hasNext()) {
		    mobile = iter.next();
		    if (mobile.isDead()) {
			iter.remove();
		    } else {
			((Entity) mobile).tick(activeLevel);
		    }
		}

		/* Players heartbeat */
		iter = activeLevel.getPlayers().iterator();
		EntityTranslatable player;
		while (iter.hasNext()) {
		    player = iter.next();
		    if (player.isDead()) {
			iter.remove();
		    } else {
			((Entity) player).tick(activeLevel);
			activeLevel.focusEntity(((Entity) player), false);
		    }
		}
	    }
	    paintComponent(this.getGraphics());
	    timePreviousLoop = System.currentTimeMillis();
	}
    }

    public static boolean isPaused() {
	/* Check if any subsystem is pausing the game */
	for (Entry<PauseType, Boolean> entry : pause.entrySet()) {
	    if (entry.getValue()) {
		return true;
	    }
	}

	return false;
    }

    private static boolean setActiveLevel(String key) {
	Level newLevel = null;
	newLevel = levels.get(key);
	if (newLevel != null)
	    activeLevel = newLevel;

	return newLevel != null;
    }

    public static void addLevel(Level level) {
	if (level != null) {
	    level.setScreenSize(winWidth, winHeight);
	    levels.put(level.getKey(), level);

	    // No active level has been set, let's set it to this one.
	    if (activeLevel == null)
		activeLevel = level;
	}
    }

    public void addReceptionist(Receptionist receptionist) {
	Bleach.receptionist = receptionist;

	for (KeyBinding keyBinding : receptionist.getKeyBindings()) {
	    this.getInputMap().put(keyBinding.getKey(), keyBinding.getActionMapKey());
	    this.getActionMap().put(keyBinding.getActionMapKey(), keyBinding.getAction());
	}

	this.addMouseMotionListener(new MouseMotionListener() {

	    @Override
	    public void mouseDragged(MouseEvent e) {
		// Ignore
	    }

	    @Override
	    public void mouseMoved(MouseEvent event) {
		Bleach.receptionist.handleEvent(event);
	    }
	});

    }

    /**
     * This sets up the window and starts the game. *
     **/
    public void init() {

	// Set the size of this JPanel before inserting it into the window.
	setSize(winWidth, winHeight);

	// Sometimes setSize() just fails. Go figure.
	setPreferredSize(new Dimension(winWidth, winHeight));

	// This is a pointer to this JPanel used in the Event Dispatch Thread
	// (EDT).
	final Bleach EDTpointerToPanel = this;

	// This is the window title variable used in the Event Dispatch Thread
	// (EDT).
	final String EDTwindowTitle = winTitle;

	try {
	    SwingUtilities.invokeAndWait(new Runnable() {
		/*
		 * Event Dispatch Thread - prevents potential race conditions
		 * that could lead to deadlock.
		 */
		@Override
		public void run() {
		    jWindow = new JFrame(EDTwindowTitle);
		    jWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		    jWindow.setResizable(false);
		    jWindow.add(EDTpointerToPanel);

		    // Fixes a bug that sometimes adds 10 pixels to width and
		    // height. Weird stuff.
		    jWindow.pack();
		    jWindow.pack();

		    // Center the window on the primary monitor.
		    jWindow.setLocationRelativeTo(null);

		    jWindow.setVisible(true);
		}
	    });
	} catch (InvocationTargetException | InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	setDoubleBuffered(true);
	setFocusable(true);
	setBackground(Color.cyan);

	renderer = new Picasso(winWidth, winHeight);
    }

    public void init(int windowWidth, int windowHeight, String windowTitle) {
	winWidth = windowWidth;
	winHeight = windowHeight;
	winTitle = windowTitle;
	init();
    }

    @Override
    public void paintComponent(Graphics g) {
	double deltaTime = System.currentTimeMillis() - timePreviousRender;

	if (FPS > 0 && deltaTime < 1000.0 / FPS)
	    return;

	double actualFPS = (1000.0 / Math.max(1, (deltaTime)));

	timeDebug += deltaTime;
	if (timeDebug >= 1000) {
	    timeDebug = 0;
	    renderer.clearDebugLines();
	    renderer.addDebugLine("FPS: " + (int) actualFPS);
	}

	renderer.render(g, activeLevel);

	timePreviousRender = System.currentTimeMillis();
    }

    public void run() {
	gameLoop();
    }

    public static double setFPS(double newFPS) {
	/* Sets the FPS, returns the old FPS. */
	double retval = FPS;
	FPS = newFPS;
	return retval;
    }

    public static void setTitle(String title) {
	winTitle = title;
    }

    /**
     * The game can be paused by many reasons, this is an enumeration of those.
     **/
    private enum PauseType {
	// In-game information is displayed (e.g. a splash-screen is displayed,
	// a book, notepad, messageboard etc is displayed, inventory is
	// displayed)
	GAMEMESSAGE,

	// The loader is working (e.g. save game)
	LOADER,

	// The user used the pause functionality.
	USER
    }
}
