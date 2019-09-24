package nl.sensorlab.videowall;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.cleverfranke.util.ConfigurationLoader;
import com.cleverfranke.util.PColor;

import nl.sensorlab.videowall.animation.BaseAnimation;
import nl.sensorlab.videowall.animation.baseanimations.BouncyPixelsAnimation;
import nl.sensorlab.videowall.animation.baseanimations.ColorAnimation;
import nl.sensorlab.videowall.animation.baseanimations.ColorGridAnimation;
import nl.sensorlab.videowall.animation.baseanimations.HorizontalScanAnimation;
import nl.sensorlab.videowall.animation.baseanimations.HorizontalWavesAnimation;
import nl.sensorlab.videowall.animation.baseanimations.LiquidColumnsAnimation;
import nl.sensorlab.videowall.animation.baseanimations.PerlinNoiseAnimation;
import nl.sensorlab.videowall.animation.baseanimations.SensorLabLogo;
import nl.sensorlab.videowall.animation.baseanimations.alphabet.Alphabet;
import nl.sensorlab.videowall.animation.baseanimations.flocking.FlockingAnimation;
import nl.sensorlab.videowall.animation.baseanimations.sorting.SortingAnimation;
import nl.sensorlab.videowall.animation.canvasanimations.BeachballAnimation;
import nl.sensorlab.videowall.animation.canvasanimations.ImageAnimation;
import nl.sensorlab.videowall.animation.canvasanimations.ShaderAnimation;
import nl.sensorlab.videowall.animation.canvasanimations.VideoAnimation;
import nl.sensorlab.videowall.animation.canvasanimations.VideoStreamAnimation;
import nl.sensorlab.videowall.property.IntProperty;
import nl.sensorlab.videowall.property.Property;
import nl.sensorlab.videowall.property.Property.PropertyValueListener;
import processing.core.PApplet;

/**
 * Class that handles the animation queue and transitions between them.
 */
public class AnimationManager implements PropertyValueListener {

	private static AnimationManager instance = null; 						// Singleton instance
	private BaseAnimation currentAnimation;									// Current animation
	private List<AnimationEntry> availableAnimations = new ArrayList<>();	// All available animations, index of an item is the same as the entry id

	private List<AnimationEventListener> eventListeners = new ArrayList<AnimationEventListener>();
	
	private IntProperty	activeAnimationId;

	/**
	 * Instantiate AnimationManager; add animations to the available
	 * animations list
	 *
	 * @param applet
	 */
	public AnimationManager(PApplet applet) {
		
		activeAnimationId = IntProperty.wallProperty("activeAnimationId");
		activeAnimationId.addValueListener(this);
		
		// Full black
		ColorAnimation black = new ColorAnimation(applet);
		black.setData(String.valueOf(PColor.color(0)));
		addAnimation("Black", "Plain black", black);

		// Full white
		ColorAnimation white = new ColorAnimation(applet);
		white.setData(String.valueOf(PColor.color(255)));
		addAnimation("White", "Plain white", white);

		// All Applet based animation
		addAnimation("Sensorlab logo", "Moving triangles logo", new SensorLabLogo(applet));
		addAnimation("Marquee text", "Scrolling text", new Alphabet(applet));
		addAnimation("Swirl", " Perlin Noise", new PerlinNoiseAnimation(applet));
		addAnimation("Horizontal Waves", "Overlapping lines", new HorizontalWavesAnimation(applet));
		addAnimation("Swarm Animation", "Flocking particles", new FlockingAnimation(applet));
		addAnimation("Bouncy Pixels Animation", "Pixel rain", new BouncyPixelsAnimation(applet));
		addAnimation("Dark Shadow", " Liquid Columns", new LiquidColumnsAnimation(applet));
		addAnimation("Sorting Algorithm", "Visualizing sorting methods", new SortingAnimation(applet));
		addAnimation("Swirl Void", " Monjori shader animation", new ShaderAnimation(applet, "monjori", 1500));
		addAnimation("Horizontal Scan", "Knight rider", new HorizontalScanAnimation(applet));
		addAnimation("Color Grid", "Fill the canvas with a gradient", new ColorGridAnimation(applet));
		addAnimation("Beach Ball", "Technical difficulties", new BeachballAnimation(applet));

		// Video stream options
		for (String host: ConfigurationLoader.get().getString("streaming.hosts", "").split(",")) {
			
			String[] streamParts = host.split(":");
			if (streamParts.length < 1 || streamParts.length > 2) {
				continue;
			}
			
			String hostIp = streamParts[0].trim();
			String streamName = (streamParts.length == 2) ? streamParts[1].trim() : "Video Stream from " + hostIp;

			if (!hostIp.isEmpty()) {
				addAnimation(streamName, "IP Stream from " + hostIp, new VideoStreamAnimation(applet));
			}
		}
		
		// Add videos to animation manager
		VideoAnimation videoAnimation = new VideoAnimation(applet);
		for (File f : VideoAnimation.getVideoFileList()) {
			String filename = f.getName();
			filename = filename.substring(0, filename.lastIndexOf('.'));
			addAnimation("Video: " + filename, f.getAbsolutePath(), videoAnimation);
		}

		// Add images to animation manager
		ImageAnimation imageAnimation = new ImageAnimation(applet);
		for (File f : ImageAnimation.getImageFileList()) {
			String filename = f.getName();
			filename = filename.substring(0, filename.lastIndexOf('.'));
			addAnimation("Image: " + filename, f.getAbsolutePath(), imageAnimation);
		}
	}

	/**
	 * Initialize Animation. This method should be called before calling
	 * AnimationManager.getInstance() and subsequent methods
	 *
	 * @return
	 */
	public static void initialize(PApplet applet) {
		if (instance != null) {
			System.err.println("Cannot initialize AnimationManager twice, this call is ignored");
		} else {
			instance = new AnimationManager(applet);
		}
	}

	/**
	 * Get singleton instance
	 * @return
	 */
	public static AnimationManager getInstance() {
		if (instance == null) {
			System.err.println("AnimationManager not initialized; first cal AnimationManager.initialize");
		}
		return instance;
	}

	public BaseAnimation getCurrentAnimation() {
		return currentAnimation;
	}

	/**
	 * Add an animation to the list of animations
	 * 
	 * @param label
	 * @param data
	 * @param animation
	 * @return the id of the animation entry
	 */
	public int addAnimation(String label, String description, String data, BaseAnimation animation) {
		AnimationEntry entry = new AnimationEntry(label, description, data, animation);
		if (availableAnimations.add(entry)) {
			entry.id = availableAnimations.size() - 1;
			return entry.id;
		} else {
			return -1;
		}
	}

	/**
	  * Add an animation to the list of animations
	 * 
	 * @param label
	 * @param animation
	 * @return the id of the animation entry
	 */
	public int addAnimation(String label, String description, BaseAnimation animation) {
		return addAnimation(label, description, null, animation);
	}

	public List<AnimationEntry> getAvailableAnimations() {
		return availableAnimations;
	}

	public int getAvailableAnimationCount() {
		return availableAnimations.size();
	}

	/**
	 * Start the animation with the given animation entry id
	 * 
	 * @param animationEntryId
	 */
	public void startAnimation(int animationEntryId) {
		if (animationEntryId > availableAnimations.size() - 1) {
			System.err.println("Cannot start animation: Invalid animation index");
			return;
		}

		// Send stop signal to current animation
		if (currentAnimation != null) {
			currentAnimation.isStopping();
		}

		// Set current animation to animation with given index
		AnimationEntry entry = availableAnimations.get(animationEntryId);
		currentAnimation = entry.getAnimation();
		if (currentAnimation == null) {
			return;
		}

		// Set data (if any data is available)
		if (entry.getData() != null && !entry.getData().isEmpty()) {
			currentAnimation.setData(entry.getData());
		}

		// Send starting signal
		currentAnimation.isStarting();
		
		activeAnimationId.setValue(animationEntryId);

		// Send change event
		for (AnimationEventListener listener : eventListeners) {
			listener.onCurrentAnimationChanged(animationEntryId);
		}

	}

	public void addListener(AnimationEventListener listener) {
		eventListeners.add(listener);
	}

	public class AnimationEntry {
		private int id;
		private String label;
		private String description;
		private String data;
		private BaseAnimation animation;

		private AnimationEntry(String label, String description, String data, BaseAnimation animation) {
			this.label = label;
			this.description = description;
			this.data = data;
			this.animation = animation;
		}
		
		protected void setId(int id) {
			this.id = id;
		}

		public AnimationEntry createAnimationEntry(String label, String description, BaseAnimation animation) {
			return new AnimationEntry( label, description, null, animation);
		}

		public AnimationEntry createAnimationEntry(String label, String description, String data, BaseAnimation animation) {
			return new AnimationEntry( label, description, data, animation);
		}
		
		public int getId() {
			return id;
		}

		public String getLabel() {
			return label;
		}
		
		public String getDescription() {
			return description;
		}

		public String getData() {
			return data;
		}

		public BaseAnimation getAnimation() {
			return animation;
		}

	}

	/**
	 * Interface to subscribe to event changes
	 */
	public interface AnimationEventListener {

		/**
		 * Method that is called when the current animation
		 * has changed
		 *
		 * @param index
		 */
		void onCurrentAnimationChanged(int index);
	}

	@Override
	public void onPropertyChange(Property property) {
		if (property == activeAnimationId) {
			int animationId = ((IntProperty) property).getValue();
			if (animationId <= availableAnimations.size() - 1) {
				startAnimation(animationId);
			}
		}
	}

}
