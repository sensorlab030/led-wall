package nl.sensorlab.videowall;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import com.cleverfranke.util.Settings;

import de.looksgood.ani.Ani;
import nl.sensorlab.videowall.animation.BaseAnimation;
import nl.sensorlab.videowall.animation.BaseCanvasAnimation;
import nl.sensorlab.videowall.animation.Preview;
import nl.sensorlab.videowall.ui.MainWindow;
import nl.sensorlab.videowall.walldriver.WallDriver;
import nl.sensorlab.videowall.walldriver.WallDriverPort;
import nl.sensorlab.videowall.walldriver.WallGeometry;
import processing.core.PApplet;
import processing.core.PImage;
import processing.video.Movie;

public class LedWallApplication extends PApplet {

	private WallDriver driver;
	private Preview preview;
	private MainWindow mainWindow;

	private boolean ledPreviewEnabled = true;
	private boolean sourcePreviewEnabled;
	private boolean blackOutEnabled;

	@Override
	public void settings() {
		Rectangle previewRect = WallGeometry.scaleRectangleRounded(WallGeometry.getInstance().getWallGeometry(), Preview.SCALE);
		size(previewRect.width, previewRect.height, P3D);
	}

	@Override
	public void setup() {
		frameRate(WallDriverPort.FRAMERATE);
		surface.setTitle("Preview");

		// Initialize animation manager
		AnimationManager.initialize(this);

		// Setup preview
		preview = new Preview(this);

		// Create mainWindow
		mainWindow = new MainWindow(this);

		// Configure wall driver
		driver = new WallDriver(this,
				Settings.getValue("driverPort1"),
				Settings.getValue("driverPort2"));

		// Initialize ANI
		Ani.init(this);

	}

	@Override
	public void draw() {
		background(0);

		// Fetch current animation
		BaseAnimation animation =  AnimationManager.getInstance().getCurrentAnimation();
		if (animation == null) {
			return;
		}

		// Draw animation frame
		animation.draw();

		// Display previews
		if (ledPreviewEnabled) {
			image(preview.renderPreview(animation.getImage()), 0, 0);
		}
		if (sourcePreviewEnabled && BaseCanvasAnimation.class.isAssignableFrom(animation.getClass())) {
			image(((BaseCanvasAnimation) animation).getCanvasImage(), 0, 0);
		} else if (sourcePreviewEnabled) {

			// Copy animation image (else the animation image gets resized)
			// and scale it for better visibility
			PImage sourcePreview = animation.getImage().get();
			sourcePreview.resize(130, 405);

			image(sourcePreview, 0, 0);
		}

		// Send image to driver
		driver.displayImage(animation.getImage());

	}

	/**
	 * Handle movie events (used by VideoAnimation class), read
	 * a new frame from the movie
	 *
	 * @param m
	 */
	public void movieEvent(Movie m) {
		m.read();
	}

	/**
	 * Handle key presses
	 */
	@Override
	public void keyPressed() {
		switch (keyCode) {
			case KeyEvent.VK_Q:
				mainWindow.toggle();
				break;
			case KeyEvent.VK_W:
				ledPreviewEnabled = !ledPreviewEnabled;
				System.out.println("LED preview enabled: " + ledPreviewEnabled);
				break;
			case KeyEvent.VK_E:
				sourcePreviewEnabled = !sourcePreviewEnabled;
				System.out.println("Source preview enabled: " + sourcePreviewEnabled);
				break;
			case KeyEvent.VK_SPACE:
				blackOutEnabled = !blackOutEnabled;
				driver.setBlackOutEnabled(blackOutEnabled);
				System.out.println("Black out enabled: " + blackOutEnabled);
				break;
		}

	}

	/**
	 * Set wall brightness
	 *
	 * @param brightness [0, 255] (0 = off, 255 is brightest possible)
	 */
	public void setWallBrightness(int brightness) {
		driver.setBrightness(brightness);
	}

	public static void main(String[] args) {

		// Load settings (either first argument or settings.json by default)
		String settingsFile = args.length > 0 ? args[0] : "settings.json";
		if (!Settings.loadSettings(settingsFile)) {
			System.exit(1);
		}

		// Program execution starts here
		PApplet.main(LedWallApplication.class.getName());
	}

}
