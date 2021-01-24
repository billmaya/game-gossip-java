package Gossip;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.UIManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.Polygon;
import java.awt.geom.Line2D;


/*
 * This program uses a modified version of Hungarian notation.
 * Variables often have single-character prefixes denoting their function.
 * "f" prefix indicates a boolean FLAG
 * "c" prefix indicates an integer COUNTER
 * "i" prefix indicates an INDEX
 * "t" indicates a passed parameter and precedes the previous prefixes
 * 
 *  A major controlling factor in this design is the concept of "phase",
 *  which is indexed with a variable called, amazingly enough, "iPhase".
 *  A phase is a single step in a turn; at present there are 16 distinct phases
 *  in a single turn, but they do not necessarily proceed in a clear and simple
 *  sequence. I have prepared a flow chart showing how phase transitions are
 *  made. It's complicated. I have at least given each phase a descriptive
 *  name that should make it a little easier to understand.
 *  
 *  The main thread of the program is in method "main" at the very end of this
 *  file. It spends all of its time sitting in a wait loop, waiting for the
 *  game to quit. Meanwhile there are two other major types of threads:
 *  Graphics display threads, of which there is just one, and input threads,
 *  of which there are two types: timer threads and mouse input threads.
 *  
 *  Timer threads are used to permit certain animations/sounds to play out.
 *  None of those animations/sounds have been implemented yet, but they 
 *  are readily plugged in so as to be triggered when the timer is started.
 *  For the moment, I have no intention of implementing those animations/sounds,
 *  because I want to make sure that they really can be implemented adequately
 *  on all platforms, information that you will have to provide. I have not
 *  yet attempted their implementation in Java.
 *  
 *  I do NOT implement conventional Java buttons, because I could not get
 *  them to behave in the manner I desired while co-existing with other
 *  Java features. Perhaps I don't understand Java clearly enough, but when
 *  I tried to combine Java graphics redrawing with Java Swing buttons, I ran
 *  into apparent incompatibilities. In the end, I decided that it would be
 *  preferable to implement these buttons at a lower level, so as to obviate
 *  any problems with differences in handling buttons by different platforms.
 *  
 *  There are two types of mouse response: button handling and face-click handling.
 *  The button handling is pretty much what you expect; it is the release of the
 *  mouse button that triggers the event to which my code responds. Most of the
 *  buttons have fairly straightforward functions, but one button, the "enter"
 *  button, is used in many situations and so the response to that button 
 *  depends critically on what phase the program is in. This yields a monstrous
 *  switch statement that handles the enter button for many different phases.
 *  
 *  The other mouse input is a mouse-down on one of the six faces. This selects
 *  that character for some action. Again, the behavior of the code is mightily
 *  dependent on the current phase, so the code is a bit messy there also.
 *  
 *  Thus, the overall architecture is as follows:
 *     a main thread just ticking over, waiting for quitting time
 *     input response code that triggers changes of state, including phase changes.
 *     graphical code that draws the screen based on phase and other state variables.
 *     
 * Here's another major concept in the game: direct statements versus indirect statements
 * Direct statements are "{iPerson1} says that {iPerson2} is {adjective}."
 * They are simply statements of affinity[iPerson1][iPerson2]
 * 
 *  Indirect statements involve a fourth person; they represent iPerson1's 
 *  assessment of how much iPerson3 likes iPerson4, which is simply 
 *  perceivedAffinity[iPerson1][iPerson3][iPerson4]
 *  There's a lot of this stuff scattered through the program and
 *  at times it can get a bit confusing.
 *  
 *  The AI for the NPCs has not yet been implemented. They're pretty
 *  much brainless actors right now. I'll be adding that over the course
 *  of January. All the AI is confined to the last section of code (I think).
 */
public class Gossip extends JFrame implements ImageObserver {
	private static final long serialVersionUID=1L;
	// graphics constants specifying sizes and distances of imagery
	static final int frameWidth=940;	// width of window
	static final int frameHeight=640;	// height of window
	static final int centerX=frameWidth/2;	// center of window
	static final int centerY=frameHeight/2;	// center of window
	static final int messageX=650;  // location of message text
	static final int messageY=416;  // location of message text
	static final int turnIndicatorX=710; // location of turn indicator
	static final int turnIndicatorY=220; // location of turn indicator
	static final int offset=140;		// how far away from the face its text must be
	static final int faceSize=128;	// width and height of a face image
	static final int haloSize=140;
	static final double radius=250; // radius of the circle circumscribing the hexagon


	static final int maxCharacters=6;	// number of characters
	static final int cAffinityLevels=9; // number of permitted expressions
	static final int nobody=-1;
	static final int anybody=-1;
	
	// button identifications
	static final int upArrow=0;
	static final int downArrow=1;
	static final int enter=2;
	static final int rules=3;
	static final int exit=4;	
	static final int playAgain=5;
	static final int rightArrow=6;
	static final int leftArrow=7;
	static final int _4Button=8;
	static final int _5Button=9;
	static final int _6Button=10;
	static final int easyButton=11;
	static final int mediumButton=12;
	static final int hardButton=13;
	static final int leaveOptions=14;
	static final int tips=15;
	static final int cButtons=16;	// number of button
	
	// These are the different displays that can be presented
	static final int mainDisplay=0;
	static final int rulesDisplay=1;
	static final int titleDisplay=2;
	static final int endGameDisplay=3;
	static final int backgroundDisplay=4;
	static final int optionsDisplay=5;
	static final int tipsDisplay=6;
	
	// Phase definitions
	static final int playerSelectsCallee=0;
	static final int ring=1;
	static final int playerSelectsPredicate=2;
	static final int playerDeclaresDirectAffinity=3;
	static final int reactionAnimation1=4;
	static final int npcRespondsDirectAffinity=5;
	static final int playerDeclaresIndirectAffinity=6;
	static final int reactionAnimation2=7;
	static final int npcRespondsIndirectAffinity=8;
	static final int playerHangsUp=9;
	static final int npcTurn=10;
	static final int npcCallsPlayer=11;
	static final int npcDeclaresDirectAffinity=12;
	static final int playerRespondsDirectAffinity=13;
	static final int reactionAnimation3=14;
	static final int npcDeclaresIndirectAffinity=15;
	static final int playerRespondsIndirectAffinity=16;	
	static final int reactionAnimation4=17;
	static final int npcHangsUp=18;
	
	boolean fQuit;		// is it time to quit the game?
	int maxTurns;		// how long the game will be
	int iTurn;			// how many turns have we played so far?
	int iPhase;			// the all-important phase index
	int iDisplay;		// specifies which display mode we'in in; see above constants
	int player;			// a variable specifying which character is the player. For now, it's always zero, but I might want to change it in the future.
	int buttonValue;	// the variable controlled by the upArrow and downArrow buttons.
	int iBackgroundCharacter; // the character whose background is being displayed.
	int cCharacters;	// the number of characters playing in this particular game
	int difficultyLevel;	// you'll never guess what this is!
	int subPhase;		// used for display phase npcTurn to animate faces
	
	// these two values are used to communicate feedback to the player
	int iSuspect; // how much the speaker's statement disagrees with what I've previously heard
	int iLikeWhatIHear;	// how agreeable I find the speaker's statement
	
	// These next three variables are of enormous importance and show up everywhere.
	// They specify who's who in any given conversation.
	// Caller is always the person who initiated the call
	// Callee is the person who was called.
	// Predicate is the one they're talking about
	//
	// Since they alternate during a conversation, we also track two additional indeces:
	// Speaker is the person actually saying something
	// Listener is the person hearing it.
	//
	// There are two forms of statement, direct and indirect
	// Direct: "Speaker tells Listener that Speaker thinks that Predicate is {adjective}."
	// Indirect: "Speaker tells Listener that Predicate told Speaker that Listener is {adjective}"
	// The adjectives in the above statements are statements of affinity; they are specified
	//    with String[][] guyDescription and galDescription.
	int caller, callee, predicate;
	
	String[] names=new String[maxCharacters]; // the names of the six characters
	
	// These are just buffered images that have a location attached to them.
	// They come in nine versions: one for each affinity level.
	ImagePlusLocation[][] faces=new ImagePlusLocation[maxCharacters][cAffinityLevels+1];
	RoundButton[] buttons; // the various buttons, some of which appear and disappear.
	
	BufferedImage quotationMarks; // the image of quotation marks used in indirect displays
	Graphics g;  // the Java graphics environment used for the display
	Graphics2D g2; // the second-level Java graphics environment used for the display
	
	// how much the first character likes the second character. Bounded Number
	double[][] affinity=new double[maxCharacters][maxCharacters];
	
	// perceived affinity of second for third as perceived by first. Bounded Number
	double[][][] perceivedAffinity=new double[maxCharacters][maxCharacters][maxCharacters];
	
	// average of the affinities towards the character, for each turn
	double[][] popularity=new double[maxCharacters][10];

	// personality traits
	double[] dishonest=new double[maxCharacters];
	double[] gullible=new double[maxCharacters];
	double[] vain=new double[maxCharacters];

	// location on the screen of the characters' faces
	int[] hexagonX=new int[maxCharacters];
	int[] hexagonY=new int[maxCharacters];
	
	// strings used in player feedback.
	String[][][] directFeedback=new String[maxCharacters][3][3];
	String[][] indirectFeedback=new String[maxCharacters][3];
	int[][] feedbackFace=new int[3][3];
	
	// random number generator in Java
	Random rand;
	
	// color assignment for each of 9 discrete affinity levels, plus transparency
	Color[] affinityLevelColor=new Color[cAffinityLevels+1];
	
	// adjective describing affinity 
	String[] affinityLevelText=new String[cAffinityLevels+1];
	String[][] guyDescription=new String[maxCharacters][cAffinityLevels+1];
	String[][] galDescription=new String[maxCharacters][cAffinityLevels+1];
	
	ArrayList<History> historyBook=new ArrayList<History>();
	
	Color transparent;	// transparency color
	Font messageFont;		// font used for messages
	Font nameFont;			// font used for names
	Font turnFont;			// font used for turns
	Font optionsFont;		// font used for option labels
	
	// These are all Java timers; when started, they run for their specified time,
	// then trigger an event for which code must be assigned.
	Timer splashScreenTimer;			// how long the splash screen remains up
	Timer helloTimer;						// time delay for ring sound and animation
	//Timer goodbyeTimer;					// time delay for goodbye sound and animation
	Timer playerHangsUpTimer;			// time delay for any "hang up phone" sound and animation
	Timer npcCallsPlayerTimer;			// time delay for ring sound and animation
	Timer npcHangsUpTimer;				// time delay for any "hang up phone" sound and animation
	Timer reactionAnimationTimer;		// time delay for the NPC reaction animation
	Timer secretNPCHangsUpTimer;		// time delay for the short NPC talking animation
	
	// for playing sounds
	Clip soundClip;	
	
	// These values control Timers and are used for normal play, 
	// and are shortened for debugging
	int splashScreenTime;
	int helloTime;
	int npcCallsPlayerTime;
	//int goodbyeTime;
	int playerHangsUpTime;
	int npcHangsUpTime;
	int reactionAnimationTime;
	
	// This boolean is for use during testing.
	// It speeds up actions and prints debugging values.
	// Set it to false for real players.
	boolean fDebug;
	// these values are used only when debug=true;
	double flatteryDamage=0;
	double suspectDamage=0;;
// ************************************************************
	public Gossip() {
		fDebug=false;

		// Java for "draw stuff with antialiasing"
		System.setProperty("apple.awt.antialiasing", "on");
		System.setProperty("apple.awt.textantialiasing", "on");	
		
		setSize(frameWidth,frameHeight);
		setTitle("Gossip");		
		setBackground(Color.black);
		setLayout(null);
		setVisible(false);
		g=getGraphics();
		g2=(Graphics2D)g;
		
		rand=new Random(27); // do you think that 27 is the best number to use here?
		player=0;
		caller=player;
		callee=nobody;
		messageFont=new Font("Arial", Font.PLAIN, 24);
		nameFont=new Font("Arial", Font.PLAIN, 36);
		turnFont=new Font("Arial", Font.PLAIN, 64);
		optionsFont=new Font("Times", Font.BOLD, 64);
		transparent=new Color(0,0,0,0);
		if (fDebug) {
			splashScreenTime=100;
			helloTime=100;
			npcCallsPlayerTime=100;
			//goodbyeTime=100;
			playerHangsUpTime=100;
			npcHangsUpTime=100;
			reactionAnimationTime=100;
		}
		else {
			splashScreenTime=2000;
			helloTime=3300;
			npcCallsPlayerTime=4000;
			//goodbyeTime=4000;
			playerHangsUpTime=1000;
			npcHangsUpTime=2000;
			reactionAnimationTime=2000;
		}
			
		
		// Yes, I could have put this all in an XML file, but this stuff was
		//   initially small and steadily grew. I was a frog in warming water.
		//   Besides, I'm a little worried about making it too easy for users
		//   to alter.
		names[0]="Bara";
		names[1]="Owen";
		names[2]="Max";
		names[3]="Ella";
		names[4]="Mort";
		names[5]="Zoe";	
		
		// personality traits of the characters
		// these are NOT BNumbers, they are UNumbers
		dishonest[0]=0.0;
		dishonest[1]=0.8;
		dishonest[2]=0.0;
		dishonest[3]=-0.7;
		dishonest[4]=-0.8;
		dishonest[5]=0.0;
		
		gullible[0]=0.5;
		gullible[1]=0.2;
		gullible[2]=0.8;
		gullible[3]=0.5;
		gullible[4]=-0.25;
		gullible[5]=0.7;
		
		vain[0]=0.0;
		vain[1]=0.4;
		vain[2]=0.7;
		vain[3]=0.3;
		vain[4]=0.5;
		vain[5]=0.8;
				
		// Red through grey to Blue
		affinityLevelColor[9]=transparent;
		affinityLevelColor[8]=new Color(255,0,0); // red
		affinityLevelColor[7]=new Color(224,32,32); // 
		affinityLevelColor[6]=new Color(192,64,64); // 
		affinityLevelColor[5]=new Color(160,96,96); // 
		affinityLevelColor[4]=new Color(128,128,128); // gray
		affinityLevelColor[3]=new Color(96,96,160); //
		affinityLevelColor[2]=new Color(64,64,192); // 
		affinityLevelColor[1]=new Color(32,32,224); // 
		affinityLevelColor[0]=new Color(0,0,255); // blue
		  
		affinityLevelText[9]="hello";
		affinityLevelText[8]="adorable";
		affinityLevelText[7]="great";
		affinityLevelText[6]="nice";
		affinityLevelText[5]="pleasant";
		affinityLevelText[4]="so-so";
		affinityLevelText[3]="unpleasant";
		affinityLevelText[2]="not nice";
		affinityLevelText[1]="nasty";
		affinityLevelText[0]="hateful";
		
		guyDescription[0][0]="hateful";
		guyDescription[1][0]="an asshole";
		guyDescription[2][0]="despicable";
		guyDescription[3][0]="horrible";
		guyDescription[4][0]="loathsome";
		guyDescription[5][0]="totally scuzzy";
		
		guyDescription[0][1]="nasty";
		guyDescription[1][1]="gross";
		guyDescription[2][1]="awful";
		guyDescription[3][1]="obnoxious";
		guyDescription[4][1]="yuckers";
		guyDescription[5][1]="an ickyosaurus";
		
		guyDescription[0][2]="not nice";
		guyDescription[1][2]="a downer";
		guyDescription[2][2]="annoying";
		guyDescription[3][2]="unkind";
		guyDescription[4][2]="disagreeable";
		guyDescription[5][2]="ugh-ville";
		
		guyDescription[0][3]="unpleasant";
		guyDescription[1][3]="bleh";
		guyDescription[2][3]="a little negatory";
		guyDescription[3][3]="zzzzzz...";
		guyDescription[4][3]="uncool";
		guyDescription[5][3]="a nothingburger";
		
		guyDescription[0][4]="so-so";
		guyDescription[1][4]="meh";
		guyDescription[2][4]="OK, I guess";
		guyDescription[3][4]="yawn-worthy";
		guyDescription[4][4]="a who-cares kinda guy";
		guyDescription[5][4]="shrug-erific";
		
		guyDescription[0][5]="pleasant";
		guyDescription[1][5]="friendly";
		guyDescription[2][5]="a 6 out of 10";
		guyDescription[3][5]="not so bad";
		guyDescription[4][5]="better than average";
		guyDescription[5][5]="agreeable";
		
		guyDescription[0][6]="nice";
		guyDescription[1][6]="straight-up";
		guyDescription[2][6]="good";
		guyDescription[3][6]="swell";
		guyDescription[4][6]="fine";
		guyDescription[5][6]="two steps short of excellent";
		
		guyDescription[0][7]="great";
		guyDescription[1][7]="cool";
		guyDescription[2][7]="really nice";
		guyDescription[3][7]="sweet";
		guyDescription[4][7]="a really good guy";
		guyDescription[5][7]="SO badass!";
		
		guyDescription[0][8]="adorable";
		guyDescription[1][8]="way cool";
		guyDescription[2][8]="awesome";
		guyDescription[3][8]="super cute";
		guyDescription[4][8]="amazing";
		guyDescription[5][8]="just TOO much";
		
		galDescription[0][0]="hateful";
		galDescription[1][0]="shitty";
		galDescription[2][0]="Darth Vader in a dress";
		galDescription[3][0]="bitchy";
		galDescription[4][0]="just evil";
		galDescription[5][0]="like, super-nasty";
		
		galDescription[0][1]="nasty";
		galDescription[1][1]="gross";
		galDescription[2][1]="minus ungood";
		galDescription[3][1]="a yuck-face";
		galDescription[4][1]="crude";
		galDescription[5][1]="majorly not nice";
		
		galDescription[0][2]="not nice";
		galDescription[1][2]="Loser City";
		galDescription[2][2]="annoying";
		galDescription[3][2]="unkind";
		galDescription[4][2]="a downer";
		galDescription[5][2]="a loser";
		
		galDescription[0][3]="unpleasant";
		galDescription[1][3]="is like runny catchup";
		galDescription[2][3]="a little negatory";
		galDescription[3][3]="'zzzzzz...'";
		galDescription[4][3]="disagreeable";
		galDescription[5][3]="not classy";
		
		galDescription[0][4]="so-so";
		galDescription[1][4]="a big yawn";
		galDescription[2][4]="in the middle of the curve";
		galDescription[3][4]="adequate";
		galDescription[4][4]="meh";
		galDescription[5][4]="'not on my radar'";
		
		galDescription[0][5]="pleasant";
		galDescription[1][5]="fair";
		galDescription[2][5]="not without some charm";
		galDescription[3][5]="well-meaning";
		galDescription[4][5]="OK in my book";
		galDescription[5][5]="nice but not on my iPhone";
		
		galDescription[0][6]="nice";
		galDescription[1][6]="one good chickie";
		galDescription[2][6]="'somebody I like'";
		galDescription[3][6]="worthy";
		galDescription[4][6]="one of the nicer people";
		galDescription[5][6]="kinda excellent";
		
		galDescription[0][7]="great";
		galDescription[1][7]="wicked good";
		galDescription[2][7]="really nice";
		galDescription[3][7]="a lovely person";
		galDescription[4][7]="like a sister";
		galDescription[5][7]="cool-orific";
		
		galDescription[0][8]="bff";
		galDescription[1][8]="gorgeous";
		galDescription[2][8]="definitely a 10";
		galDescription[3][8]="the best";
		galDescription[4][8]="'number 1 in my book'";
		galDescription[5][8]="so very fab";
				
		directFeedback[0][0][0]=".";
		directFeedback[1][0][0]="is angry at you.";
		directFeedback[2][0][0]="doesn't like at all what he's hearing.";
		directFeedback[3][0][0]="is upset with you.";
		directFeedback[4][0][0]="expresses great displeasure.";
		directFeedback[5][0][0]="is, like, totally mad at you.";
		
		directFeedback[0][1][0]=".";
		directFeedback[1][1][0]="doesn't have anything to say.";
		directFeedback[2][1][0]="is silent.";
		directFeedback[3][1][0]="talks about her dog.";
		directFeedback[4][1][0]="seems OK with you.";
		directFeedback[5][1][0]="thanks you for being honest";
		
		directFeedback[0][2][0]=".";
		directFeedback[1][2][0]="is quite pleased with you.";
		directFeedback[2][2][0]="is really happy you said that.";
		directFeedback[3][2][0]="expresses delight at your statement.";
		directFeedback[4][2][0]="says 'Right on!'";
		directFeedback[5][2][0]="agrees like, totally.";
		
		directFeedback[0][0][1]=".";
		directFeedback[1][0][1]="is angry at you and expresses some doubts.";
		directFeedback[2][0][1]="disses you and doesn't take you seriously.";
		directFeedback[3][0][1]="gets mad and says she isn't convinced.";
		directFeedback[4][0][1]="says 'Rats!' and seems uncertain about your claim.";
		directFeedback[5][0][1]="is most seriously displeased and somewhat skeptical.";
		
		directFeedback[0][1][1]=".";
		directFeedback[1][1][1]="sounds irritated and says, 'Oh, really?'.";
		directFeedback[2][1][1]="is not happy with that and asks if you're sure.";
		directFeedback[3][1][1]="seems displeased and wonders where you heard that.";
		directFeedback[4][1][1]="grumbles and says 'That's, um, interesting.'";
		directFeedback[5][1][1]="says, 'My, my! That's not good. How very special!'";
		
		directFeedback[0][2][1]=".";
		directFeedback[1][2][1]="is pleased but seems skeptical.";
		directFeedback[2][2][1]="appreciates your statement, sorta.";
		directFeedback[3][2][1]="approves, but seems uncertain about you.";
		directFeedback[4][2][1]="responds pleasantly but tactfully.";
		directFeedback[5][2][1]="praises what you said -- a little too much.";
		
		directFeedback[0][0][2]=".";
		directFeedback[1][0][2]="calls you a nasty liar.";
		directFeedback[2][0][2]="is angry about your lying.";
		directFeedback[3][0][2]="accuses you of lying.";
		directFeedback[4][0][2]="says 'That is a lie!'";
		directFeedback[5][0][2]="says she's never heard such lies.";
	
		directFeedback[0][1][2]=".";
		directFeedback[1][1][2]="doesn't believe you.";
		directFeedback[2][1][2]="doesn't buy a word of it.";
		directFeedback[3][1][2]="doubts you.";
		directFeedback[4][1][2]="is very skeptical.";
		directFeedback[5][1][2]="is totally, totally unconvinced.";
		
		directFeedback[0][2][2]=".";
		directFeedback[1][2][2]="says, 'Yeah, sure.'";
		directFeedback[2][2][2]="is nice but clearly doesn't believe you.";
		directFeedback[3][2][2]="doesn't believe you, but is polite about it.";
		directFeedback[4][2][2]="says, 'Uh, gee, that's nice of you to say.'";
		directFeedback[5][2][2]="says 'I'm sure, honey.'";
		
		indirectFeedback[0][0]=".";
		indirectFeedback[1][0]="says that you're completely wrong.";
		indirectFeedback[2][0]="disagrees 100%.";
		indirectFeedback[3][0]="thinks that you're just plain wrong.";
		indirectFeedback[4][0]="says he has the opposite feeling.";
		indirectFeedback[5][0]="says 'You're totally mistaken, dearie.'";
		
		indirectFeedback[0][1]=".";
		indirectFeedback[1][1]="doesn't completely agree with you.";
		indirectFeedback[2][1]="has his doubts about what you say.";
		indirectFeedback[3][1]="suggests that you've been fooled.";
		indirectFeedback[4][1]="diplomatically waffles.";
		indirectFeedback[5][1]="says 'You're not quite right.'";
		
		indirectFeedback[0][2]=".";
		indirectFeedback[1][2]="thinks the same thing.";
		indirectFeedback[2][2]="is 100% sure you're right.";
		indirectFeedback[3][2]="confirms your assessment.";
		indirectFeedback[4][2]="says 'You nailed it!'";
		indirectFeedback[5][2]="agrees totally with everything you say.";
		
		
		feedbackFace[0][0]=0;
		feedbackFace[0][1]=1;
		feedbackFace[0][2]=2;
		feedbackFace[1][0]=4;
		feedbackFace[1][1]=3;
		feedbackFace[1][2]=3;
		feedbackFace[2][0]=7;
		feedbackFace[2][1]=5;
		feedbackFace[2][2]=2;	
		
		for (int i=0; (i<maxCharacters); ++i) {
			double angle=Math.toRadians(60*i+90);  // convert 60¼ steps to radians
			hexagonX[i]=(int)(centerX+(radius-70)*Math.sin(angle));
			hexagonY[i]=(int)(centerY+(radius-70)*Math.cos(angle));
			double dx=centerX+radius*Math.sin(angle);
			int x=(int)dx-faceSize/2;
			double dy=centerY+radius*Math.cos(angle);
			int y=(int)dy-faceSize/2;
			for (int j=0; (j<cAffinityLevels+1); ++j) {
				faces[i][j]=new ImagePlusLocation(readImage("Faces/"+affinityLevelText[j]+"/"+names[i]));
				faces[i][j].setLocation(x,y);
			}
		}
		quotationMarks=readImage("QuotationMarks");
		buttons=new RoundButton[cButtons];
		buttons[upArrow]=new RoundButton("upArrow",800,30);
		buttons[downArrow]=new RoundButton("downArrow",800,170);
		buttons[enter]=new RoundButton("enter",800,500);
		buttons[rules]=new RoundButton("Rules",10,30);
		buttons[exit]=new RoundButton("Exit",10,500);
		buttons[playAgain]=new RoundButton("PlayAgain",800,30);
		buttons[rightArrow]=new RoundButton("rightArrow",800,500);
		buttons[leftArrow]=new RoundButton("leftArrow",10,500);
		buttons[_4Button]=new RoundButton("4Button",127,120);
		buttons[_5Button]=new RoundButton("5Button",397,120);
		buttons[_6Button]=new RoundButton("6Button",667,120);
		buttons[easyButton]=new RoundButton("easy",127,480);
		buttons[mediumButton]=new RoundButton("medium",397,480);
		buttons[hardButton]=new RoundButton("hard",667,480);
		buttons[leaveOptions]=new RoundButton("leaveOptions",800,290);
		buttons[tips]=new RoundButton("tips",10,500);

		// Herewith the code for the various timers that specify delays for sounds and animations.
		// The first parameter is the time delay in milliseconds
		splashScreenTimer=new Timer(splashScreenTime, new ActionListener() {
			public void actionPerformed(ActionEvent e) { 
					splashScreenTimer.stop();
					iDisplay=optionsDisplay;
					buttons[_4Button].setfVisible(true);
					buttons[_5Button].setfVisible(true);
					buttons[_6Button].setfVisible(true);
					buttons[easyButton].setfVisible(true);
					buttons[mediumButton].setfVisible(true);
					buttons[hardButton].setfVisible(true);
					buttons[leaveOptions].setfVisible(true);
					buttons[_4Button].setfEnabled(true);
					buttons[_5Button].setfEnabled(true);
					buttons[_6Button].setfEnabled(true);
					buttons[easyButton].setfEnabled(true);
					buttons[mediumButton].setfEnabled(true);
					buttons[hardButton].setfEnabled(true);
					buttons[leaveOptions].setfEnabled(true);
					
					buttons[_4Button].setfRadioed(true);
					buttons[easyButton].setfRadioed(true);
					repaint();
			};			
		});
		helloTimer=new Timer(helloTime, new ActionListener() {
			public void actionPerformed(ActionEvent e) { 
				playSound("Hello/"+names[callee], false);
				iPhase=playerSelectsPredicate;
				repaint();
			};			
		});
		helloTimer.setRepeats(false);
		
		npcCallsPlayerTimer=new Timer(npcCallsPlayerTime, new ActionListener() {
			public void actionPerformed(ActionEvent e) { 
				buttons[enter].setfEnabled(true);
				playSound("Hello/"+names[callee], false);
				iPhase=npcDeclaresDirectAffinity;
				repaint();
			};			
		});
		npcCallsPlayerTimer.setRepeats(false);

		/*goodbyeTimer=new Timer(goodbyeTime, new ActionListener() {
			public void actionPerformed(ActionEvent e) { 
				playSound("Goodbye/"+names[callee], false);
				iPhase=playerSelectsPredicate;
				repaint();
			};			
		});
		goodbyeTimer.setRepeats(false);*/

		playerHangsUpTimer=new Timer(playerHangsUpTime, new ActionListener() {
			public void actionPerformed(ActionEvent e) { 
				if (iDisplay==mainDisplay) {
					iPhase=npcTurn;
					caller=1; // first NPC
					callee=nobody;
					runNPCTurn();
				}
				repaint();
			};			
		});
		playerHangsUpTimer.setRepeats(false);
		
		npcHangsUpTimer=new Timer(npcHangsUpTime, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nextPerson1();
				repaint();
			};
		});
		npcHangsUpTimer.setRepeats(false);

		reactionAnimationTimer=new Timer(reactionAnimationTime, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				++iPhase;
				buttons[enter].setfEnabled(true);
				if (iPhase==npcHangsUp) {
					playSound("Goodbye/"+names[callee],false);
					npcHangsUpTimer.start();
					buttons[enter].setfEnabled(false);
				}
				repaint();
			};
		});
		reactionAnimationTimer.setRepeats(false);

		secretNPCHangsUpTimer=new Timer(200, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				++subPhase;
				playSound("psst", false);
				if (subPhase==4) {
					secretNPCHangsUpTimer.stop();
					nextPerson1();
				}
				repaint();
			};
		});

// Here we go with the Monstrous Mouse Listener!
		addMouseListener(new MouseListener() {
//		  public void mouseDragged(MouseEvent e) { }
			//------------------------------------------------------------
			/*
			 * this code handles all mouse activity. 
			 * In some places it relies on outside code;
			 * usually these are matters that require some kind of game intelligence.
			 */
			//------------------------------------------------------------
		  public void mouseReleased(MouseEvent e) {
			  // this routine handles buttons
			  boolean isEventAlreadyHandled=false;
			  int iButton=0;
			  while ((!isEventAlreadyHandled)&(iButton<cButtons)) {
				  if (buttons[iButton].isfPressed()) {
					  buttons[iButton].setfPressed(false); // reset the button
					  Rectangle test=new Rectangle();
					  BufferedImage image=buttons[iButton].getMainImage().getImage();
					  test.setRect(buttons[iButton].getX(),buttons[iButton].getY(),image.getWidth(),image.getHeight());
					  if (buttons[iButton].isfVisible() & test.contains(e.getX(), e.getY())) {
						  // take action!
						  isEventAlreadyHandled=true;
						  switch (iButton) {
							  case upArrow: {
								  if (buttonValue<(cAffinityLevels-1))
								  	++buttonValue;
								  if (buttonValue==(cAffinityLevels-1))
									  buttons[upArrow].setfEnabled(false);
								  buttons[downArrow].setfEnabled(true);
								  break;
							  }
							  case downArrow: {
								  if (buttonValue>0) 
								  	--buttonValue;
								  if (buttonValue==0)
									  buttons[downArrow].setfEnabled(false);
								  buttons[upArrow].setfEnabled(true);
								  break;
							  }
							  case rules: {
								  iDisplay=rulesDisplay;
								  buttons[enter].setfVisible(false);
								  buttons[upArrow].setfVisible(false);
								  buttons[downArrow].setfVisible(false);
								  buttons[rules].setfVisible(false);
								  buttons[tips].setfVisible(false);
								  buttons[rightArrow].setfVisible(true);
								  buttons[leftArrow].setfVisible(true);
								  buttons[leftArrow].setfEnabled(true);
								  buttons[rightArrow].setfEnabled(true);
								  iBackgroundCharacter=-1;
								  break;
							  }
							  case _4Button: {
								  cCharacters=4;
								  buttons[_4Button].setfRadioed(true);
								  buttons[_5Button].setfRadioed(false);
								  buttons[_6Button].setfRadioed(false);
								  break;
							  }
							  case _5Button: {
								  cCharacters=5;
								  buttons[_4Button].setfRadioed(false);
								  buttons[_5Button].setfRadioed(true);
								  buttons[_6Button].setfRadioed(false);
								  break;
							  }
							  case _6Button: {
								  cCharacters=6;
								  buttons[_4Button].setfRadioed(false);
								  buttons[_5Button].setfRadioed(false);
								  buttons[_6Button].setfRadioed(true);
								  break;
							  }
							  case easyButton: {
								  difficultyLevel=0;
								  buttons[easyButton].setfRadioed(true);
								  buttons[mediumButton].setfRadioed(false);
								  buttons[hardButton].setfRadioed(false);
								  break;
							  }
							  case mediumButton: {
								  difficultyLevel=1;								  
								  buttons[easyButton].setfRadioed(false);
								  buttons[mediumButton].setfRadioed(true);
								  buttons[hardButton].setfRadioed(false);
								  break;
							  }
							  case hardButton: {
								  difficultyLevel=2;
								  buttons[easyButton].setfRadioed(false);
								  buttons[mediumButton].setfRadioed(false);
								  buttons[hardButton].setfRadioed(true);
								  break;
							  }
							  case leaveOptions: {
								  iDisplay=mainDisplay;
								  maxTurns=3*(cCharacters-3);
								  iPhase=playerSelectsCallee;
								  buttons[_4Button].setfVisible(false);
								  buttons[_5Button].setfVisible(false);
								  buttons[_6Button].setfVisible(false);
								  buttons[easyButton].setfVisible(false);
								  buttons[mediumButton].setfVisible(false);
								  buttons[hardButton].setfVisible(false);
								  buttons[leaveOptions].setfVisible(false);
									
								  buttons[upArrow].setfVisible(true);
								  buttons[downArrow].setfVisible(true);
								  buttons[enter].setfVisible(true);
								  buttons[rules].setfVisible(true);
								  buttons[rules].setfEnabled(true);
								  buttons[tips].setfVisible(true);
								  buttons[tips].setfEnabled(true);

								  // initialize perceptions based on difficulty level
								  for (int i=0; (i<cCharacters); ++i) {
									  for (int j=0; (j<cCharacters); ++j) {
										  for (int k=0; (k<cCharacters); ++k) {
											  if (i==j) {
												  perceivedAffinity[i][j][k]=affinity[i][k];											  
											  }
											  else {
												  double deviance=rand.nextDouble()-0.5;
												  double wrongValue=bSum(affinity[j][k],deviance);
												  perceivedAffinity[i][j][k]=blend(affinity[j][k],wrongValue,difficultyLevel-1);
											  }
										  }
									  }
								  }
							  calculatePopularity();
							  // New code for March 11th, 2013
							  // This slows down the reaction for easier levels
							  if (difficultyLevel==0)
							  	  reactionAnimationTimer.setInitialDelay(4000);
							  if (difficultyLevel==1)
							  	  reactionAnimationTimer.setInitialDelay(3000)
							  break;
							  }
							  case playAgain: {
								  initialize();
								  break;
							  }
							  case exit: {
								  fQuit=true;
								  break;
							  }
							  case tips: {
								  iDisplay=tipsDisplay;
								  buttons[upArrow].setfVisible(false);
								  buttons[downArrow].setfVisible(false);
								  buttons[enter].setfVisible(false);
								  buttons[rules].setfVisible(false);
								  buttons[tips].setfVisible(false);
								  buttons[tips].setfEnabled(false);
								  buttons[leftArrow].setfVisible(true);
								  buttons[leftArrow].setfEnabled(true);
								  break;
							  }
							  case rightArrow: {
								  iDisplay=backgroundDisplay;
								  buttons[leftArrow].setfEnabled(true);
								  ++iBackgroundCharacter;
								  if (iBackgroundCharacter==cCharacters-1)
									  buttons[rightArrow].setfEnabled(false);
								  break;
							  }
							  case leftArrow: {
								  if ((iDisplay==rulesDisplay)|(iDisplay==backgroundDisplay)) {
									  --iBackgroundCharacter;
									  if (iBackgroundCharacter==-1)
										  iDisplay=rulesDisplay;
									  else if (iBackgroundCharacter==-2) {
										  buttons[leftArrow].setfVisible(false);
										  buttons[rightArrow].setfVisible(false);
										  iDisplay=mainDisplay;
										  buttons[upArrow].setfVisible(true);
										  buttons[downArrow].setfVisible(true);
										  buttons[enter].setfVisible(true);
										  buttons[rules].setfVisible(true);
										  buttons[tips].setfVisible(true);
										  buttons[tips].setfPressed(false);
									  }	
									  else
										  buttons[rightArrow].setfEnabled(true);									
								  }
								  if (iDisplay==tipsDisplay) {
									  iDisplay=mainDisplay;
									  buttons[upArrow].setfVisible(true);
									  buttons[downArrow].setfVisible(true);
									  buttons[enter].setfVisible(true);
									  buttons[rules].setfVisible(true);
									  buttons[tips].setfVisible(true);
									  buttons[tips].setfEnabled(true);
									  buttons[leftArrow].setfVisible(false);
								  }
								  break;
							  }
							  case enter: {
								  switch (iPhase) {
									  case playerDeclaresDirectAffinity: {
										  declareAffinity(buttonValue,player,callee, predicate );
										  iPhase=reactionAnimation1;
										  buttons[enter].setfEnabled(false);
										  buttons[upArrow].setfEnabled(false);
										  buttons[downArrow].setfEnabled(false);
										  reactionAnimationTimer.start();
										  break;
									  }
									  case npcRespondsDirectAffinity: {
										  iPhase=playerDeclaresIndirectAffinity;
										  int iAffinity=getAffinityIndex(callee,predicate);
										  declareAffinity(iAffinity, callee, player, predicate);
										  buttonValue=getPAffinityIndex(player,predicate,callee);
										  buttons[enter].setfEnabled(true);
										  buttons[upArrow].setfEnabled(true);
										  buttons[downArrow].setfEnabled(true);
										  break;
									  }
									  case playerDeclaresIndirectAffinity: {
										  iPhase=reactionAnimation2;
										  declareIndirectAffinity(buttonValue, predicate, player, callee, callee);
										  buttons[enter].setfEnabled(false);
											buttons[upArrow].setfEnabled(false);
											buttons[downArrow].setfEnabled(false);
										  reactionAnimationTimer.start();
										  break;
									  }
									  case npcRespondsIndirectAffinity: {
										  declareIndirectAffinity(getPAffinityIndex(callee,predicate,player),predicate, callee, player, player);
										  iPhase=playerHangsUp;
										  buttons[enter].setfEnabled(false);
										  playSound("Goodbye/"+names[callee],false);
										  playerHangsUpTimer.start();
										  break;
									  }
									  case npcDeclaresDirectAffinity: {
										  iPhase=playerRespondsDirectAffinity;
										  declareAffinity(getAffinityIndex(caller,predicate), caller, player, predicate);
										  buttonValue=getPAffinityIndex(player,player,predicate);
										  buttons[upArrow].setfEnabled(true);
										  buttons[downArrow].setfEnabled(true);
										  buttons[enter].setfEnabled(true);
										  break;
									  }
									  case playerRespondsDirectAffinity: {
										  declareAffinity(buttonValue, player, caller, predicate);
										  iPhase=reactionAnimation3;
										  buttons[enter].setfEnabled(false);
											buttons[upArrow].setfEnabled(false);
											buttons[downArrow].setfEnabled(false);
										  reactionAnimationTimer.start();
										  break;
									  }
									  case npcDeclaresIndirectAffinity: {
										  iPhase=playerRespondsIndirectAffinity;
										  declareIndirectAffinity(getPAffinityIndex(caller,predicate,player), predicate, caller, player, player);
										  buttonValue=getPAffinityIndex(player,predicate,caller);
										  buttons[upArrow].setfEnabled(true);
										  buttons[downArrow].setfEnabled(true);
										  buttons[enter].setfEnabled(true);
										  break;
									  }
									  case playerRespondsIndirectAffinity: {
										  declareIndirectAffinity(buttonValue, predicate, player, caller, caller);
										  iPhase=reactionAnimation4;
										  buttons[enter].setfEnabled(false);
										  buttons[upArrow].setfEnabled(false);
										  buttons[downArrow].setfEnabled(false);
										  reactionAnimationTimer.start();
										  break;
									  }
								  }
								  break;
							  }
						  }
					  }
					  repaint();
				  }
				  ++iButton; // go to the next button
			  }
		  }
			//------------------------------------------------------------
//		  public void mouseMoved(MouseEvent e) { }
			//------------------------------------------------------------
		  public void mouseClicked(MouseEvent e) { }
			//------------------------------------------------------------
		  public void mousePressed(MouseEvent e) {
			  // First we check for mousedowns on buttons and, if so, mark them as pressed.
			  int i=0; 
			  boolean gotcha=false;
			  while ((i<cButtons)&(!gotcha)) {
				  if ((buttons[i].isfEnabled())&(buttons[i].isfVisible())) {
					  Rectangle test=new Rectangle(buttons[i].x,buttons[i].y,faceSize,faceSize);
					  if (test.contains(e.getX(), e.getY())) {
						  gotcha=true;
						  buttons[i].setfPressed(true);
						  repaint(buttons[i].getX(),buttons[i].getY(),faceSize,faceSize);
					  }	
				  }
				  ++i;
			  }
			  // Here we check for mousedowns on a character's face
			  for (i=0; (i<cCharacters); ++i) {
				  Rectangle test=new Rectangle();
				  BufferedImage image=faces[i][0].getImage();
				  test.setRect(faces[i][0].getX(),faces[i][0].getY(),image.getWidth(),image.getHeight());
				  if (test.contains(e.getX(), e.getY())) {
					  switch (iPhase) {
						  case playerSelectsCallee: {
							  if (i!=caller) {
								  callee=i;
								  iPhase=ring;
								  playSound("Ringtones/"+names[callee], false);
								  helloTimer.start(); // initiate ring sound
							  }
							  break;
						  }
						  case playerSelectsPredicate: {
							  if ((i!=caller)&(i!=callee)) {
								  predicate=i;
								  int iAffinity=getAffinityIndex(caller,predicate);
								  buttons[upArrow].setfEnabled(iAffinity<8);
								  buttons[downArrow].setfEnabled(iAffinity>0);
								  buttons[enter].setfEnabled(true);
								  buttons[enter].setfVisible(true);
								  buttonValue=iAffinity;
								  iPhase=playerDeclaresDirectAffinity;
							  }
							  break;
						  }
						 } // end of switch statement
					  	repaint();
					  } // end of if statement
				}
		  }
			//------------------------------------------------------------
		  // In a better world, I would make a button unpressed if this happened to it,
		  //  but I'm too lazy.
		  public void mouseExited(MouseEvent e) { }
			//------------------------------------------------------------
		  public void mouseEntered(MouseEvent e) { }
		});		
	
	}
// ************************************************************
	private class History {
		private int iSpeaker;
		private int iSource;
		private int iListener;
		private int iPredicate;
		private int iValue;
		// ------------------------------------------------------------
		public int getiSpeaker() {
			return iSpeaker;
		}
		public int getiSource() {
			return iSource;
		}
		public int getiListener() {
			return iListener;
		}
		public int getiPredicate() {
			return iPredicate;
		}
		public int getiValue() {
			return iValue;
		}
		// ------------------------------------------------------------
		History(int tiSpeaker, int tiListener, int tiSource, int tiPredicate,int tiValue) {
			iSpeaker=tiSpeaker;
			iListener=tiListener;
			iSource=tiSource;
			iPredicate=tiPredicate;
			iValue=tiValue;
		}		
		// ------------------------------------------------------------			
	}
// ************************************************************
	// A simple class that ties a screen location to an image
	private class ImagePlusLocation {
		private BufferedImage image;
		private int x, y;
	// ------------------------------------------------------------
		ImagePlusLocation(BufferedImage tImage) {
			image=tImage;
		}
	// ------------------------------------------------------------
		public void setLocation(int tx, int ty) {
			x=tx;
			y=ty;
		}
	// ------------------------------------------------------------
		public int getX() { return x; }
		// ------------------------------------------------------------
		public int getY() { return y; }
		// ------------------------------------------------------------
		public BufferedImage getImage() { return image; }
		// ------------------------------------------------------------
		
	}
// ************************************************************
	/*
	 * This is my hand-made version of a button. As mentioned above, this is
	 * obviously a primitive approach, but I could not get the Java JButtons
	 * to do exactly as I desired. I probably just don't know enough about 
	 * Java, but this more primitive approach works. Since there are only
	 * a few buttons, the manual search the mouse handler code executes
	 * is not a significant loss.
	 */
	private class RoundButton {
		public boolean isfPressed() {
			return fPressed;
		}
		public void setfPressed(boolean fPressed) {
			this.fPressed = fPressed;
		}
		public boolean isfEnabled() {
			return fEnabled;
		}
		public void setfEnabled(boolean fEnabled) {
			this.fEnabled = fEnabled;
		}
		public boolean isfVisible() {
			return fVisible;
		}
		public void setfVisible(boolean fVisible) {
			this.fVisible = fVisible;
		}
		public boolean isfRadioed() {
			return fRadioed;
		}
		public void setfRadioed(boolean fRadioed) {
			this.fRadioed = fRadioed;
		}
		public ImagePlusLocation getMainImage() {
			return mainImage;
		}
		public ImagePlusLocation getPressedImage() {
			return pressedImage;
		}
		public ImagePlusLocation getDisabledImage() {
			return disabledImage;
		}
		private boolean fPressed;	// the mouse was pressed on this button; show pressedImage
		private boolean fEnabled;	// the button is active and ready to be pressed
		private boolean fVisible;  // the button is displayed (several buttons sit in the same locations)
		private boolean fRadioed;	// special button only for the case of the six radio buttons in the options screen
		private int x, y;			// for faster access to button coordinates
		private ImagePlusLocation mainImage, pressedImage, disabledImage;
		// ------------------------------------------------------------
		RoundButton(String name, int tx, int ty) {
	   	mainImage=new ImagePlusLocation(readImage("Buttons/"+name));
	   	pressedImage=new ImagePlusLocation(readImage("Buttons/"+name+"Pressed"));
	   	disabledImage=new ImagePlusLocation(readImage("Buttons/"+name+"Disabled"));
	   	mainImage.setLocation(tx,ty);
	   	fPressed=false;
	   	fEnabled=false;
	   	fVisible=false;
	   	fRadioed=false;
	   	x=tx;
	   	y=ty;
		}
		// ------------------------------------------------------------
		public int getX() { return mainImage.getX(); }
		// ------------------------------------------------------------
		public int getY() { return mainImage.getY(); }
		// ------------------------------------------------------------
	}
// ************************************************************
	// just a file-reading routine
	private BufferedImage readImage(String fileName) {
      BufferedImage bi=null;
		try {
			bi=ImageIO.read(new File(System.getProperty("user.dir")+"/res/"+fileName+".png"));		
      } catch (Exception e) { }
		return bi;
	}
// ************************************************************
	// this is executed every time the game is restarted.
	private void  initialize() {
		fQuit=false;
		iTurn=0;
		maxTurns=3;
		cCharacters=4;
		caller=player; // the first turn goes to the player
		callee=nobody;
		buttonValue=0;
		iDisplay=titleDisplay;
		iBackgroundCharacter=0;
		
		// these values must be reset at the beginning of a game
		for (int i=0; (i<maxCharacters); ++i) {
			// initialize all affinities BEFORE initializing perceived affinities
			int j=i+1;
			while (j<maxCharacters) {
				affinity[i][j]=2*rand.nextDouble()-1;
				// make affinities semi-symmetric
				affinity[j][i]=bSum(affinity[i][j],(2*rand.nextDouble()-1)/4);
				++j;
			}
		}
		
		for (int i=0; (i<cButtons); ++i) {
			buttons[i].setfVisible(false);
			buttons[i].setfEnabled(false);
			buttons[i].setfRadioed(false);
		}
		splashScreenTimer.start();	// we start this timer at the beginning of the game
		
		// I think it's safe to turn on the display now.
		setVisible(true);
	}
// ************************************************************
	/*
	 * This is the end of the input section.
	 * The next section concerns cosmetics: sound and the display of images on the screen
	 */
// ************************************************************
	private void playSound(String fileName, boolean fPlayContinuously) {
		if (!fDebug) {
	      try {
	        soundClip = AudioSystem.getClip();
	        AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File(System.getProperty("user.dir")+"/res/sounds/"+fileName+".wav"));
	         soundClip.open(inputStream);
	         if (fPlayContinuously) {
	         	soundClip.loop(Clip.LOOP_CONTINUOUSLY);
	         }
	         soundClip.start(); 
	       } catch (Exception e) {
	         System.err.println(e.getMessage());
	       }
		}
	}
// ************************************************************
	public void paint(Graphics g) {
		g2=(Graphics2D)g;
		g2.setFont(nameFont);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Color.black);
		g2.setStroke(new BasicStroke(3));
		g2.fillRect(0,0,frameWidth,frameHeight);
		switch (iDisplay) {
			case titleDisplay: {
				BufferedImage splashScreen=readImage("SplashScreen");
				g2.drawImage(splashScreen,0,0,transparent,this);
				break;
			}
			case optionsDisplay: {
				g2.fillRect(0,0,frameWidth,frameHeight);	
				g2.setColor(Color.white);
				g2.setFont(optionsFont);
				g2.drawString("How many players?",185,90);
				if (buttons[easyButton].isfRadioed())
					g2.setColor(Color.magenta);
				else
					g2.setColor(Color.white);					
				g2.drawString("Easy",128,462);

				if (buttons[mediumButton].isfRadioed())
					g2.setColor(Color.magenta);
				else
					g2.setColor(Color.white);					
				g2.drawString("Medium",340,462);
				
				if (buttons[hardButton].isfRadioed())
					g2.setColor(Color.magenta);
				else
					g2.setColor(Color.white);					
				g2.drawString("Hard",655,462);
				
				drawButtons();
			break;
			}
			case rulesDisplay: {
				BufferedImage rulesScreen=readImage("Rules");
				g2.drawImage(rulesScreen,0,0,transparent,this);
				drawButtons();
				break;
			}
			case backgroundDisplay: {
				BufferedImage rulesScreen=readImage("Backgrounds/"+names[iBackgroundCharacter]);
				g2.drawImage(rulesScreen,0,0,transparent,this);
				drawButtons();
				break;
			}
			case tipsDisplay: {
				BufferedImage rulesScreen=readImage("Tips"+difficultyLevel);
				g2.drawImage(rulesScreen,0,0,transparent,this);
				drawButtons();
				break;
			}
			case endGameDisplay: {
				drawHexagon();
				g2.setFont(nameFont);
				g2.drawString("Game Over",turnIndicatorX-50, turnIndicatorY);
				drawButtons();
				int[] iWinnerList=new int[cCharacters];
				double[] winnerScore=new double[cCharacters];
				for (int i=0; (i<cCharacters); ++i) {
					iWinnerList[i]=i;
					winnerScore[i]=popularity[i][iTurn]-popularity[i][0];				  
				}
				for (int i=1; (i<cCharacters); ++i) {
					int j=i;
					boolean atTheTop=false;
					while (!atTheTop) {
						if (winnerScore[j]>winnerScore[j-1]) {
							int iTemp=iWinnerList[j-1];
							double dTemp=winnerScore[j-1];
							iWinnerList[j-1]=iWinnerList[j];
							winnerScore[j-1]=winnerScore[j];
							iWinnerList[j]=iTemp;
							winnerScore[j]=dTemp;
						}
						--j;
						atTheTop=(j==0);
					}
				}
				for (int i=0; (i<cCharacters); ++i) {
					int iAffinity=boundedToInteger(bSum(popularity[iWinnerList[i]][iTurn],-popularity[iWinnerList[i]][0]));
					drawHalo(iWinnerList[i],affinityLevelColor[iAffinity],true);
					String place="";
					switch (i) {
						case 0: { place="First!";  break; }
						case 1: { place="Second"; break;  }
						case 2: { place="Third"; break; }
						case 3: { place="Fourth"; break; }
						case 4: { place="Fifth"; break;  }
						case 5: { place="Last"; break; }
						}
					g2.setColor(Color.yellow);
					int nameX=0;
					int k=iWinnerList[i];
					if ((k==0)|(k==1)|(k==5))
						nameX=faces[k][0].getX()+140;
					else
						nameX=faces[k][0].getX()-130;
					g2.drawString(place,nameX,faces[k][0].getY()+110);
				}
				break;
			}
			case mainDisplay: {
				drawHexagon();
				g2.setFont(turnFont);
				g2.drawString(String.valueOf(maxTurns-iTurn),turnIndicatorX, turnIndicatorY);
				g2.setFont(nameFont);
				drawButtons();
				//  handling for different phases		
				switch (iPhase) {
					case playerSelectsCallee: { // prompt player for selection, haloize candidates
						drawHalo(caller, Color.white, true);
						postMessage("select somebody to call");
						for (int j=0; (j<cCharacters); ++j) {
							if (j!=player) {
								drawHalo(j,Color.blue,false);
							}
						}
						break;
					}
					case ring: { // draw halo for person2
						drawSolidHalos(2);
						break;
					}
					case playerSelectsPredicate: { // prompt player, haloize candidates
						drawSolidHalos(2);
						drawFace(callee,9);
						postMessage("Select somebody to gossip about");
						for (int j=0; (j<cCharacters); ++j) {
							if ((j!=player)&(j!=callee))
								drawHalo(j,Color.magenta,false);
						}
						break;
					}
					case playerDeclaresDirectAffinity: { // player edits affinity
						drawSolidHalos(3);
						hilightSpoke(caller,predicate, false);
						break;
					}
					 // quick reaction of listener to player
					case reactionAnimation1: case reactionAnimation2: case reactionAnimation3: case reactionAnimation4:{
						// add a message explaining this animation
						int iSpeaker=0;
						if (player==caller)
							iSpeaker=callee;
						else
							iSpeaker=caller;
						String message=names[iSpeaker]+" ";
						if ((iPhase==reactionAnimation1)|(iPhase==reactionAnimation3)) {
							// direct reaction							
							drawFace(callee,feedbackFace[iLikeWhatIHear][iSuspect]);
							drawSolidHalos(3);
							hilightSpoke(iSpeaker,player, true);
							message+=directFeedback[iSpeaker][iLikeWhatIHear][iSuspect];
						}
						else { // indirect reaction
							drawFace(callee,feedbackFace[2][iSuspect]);
							drawSolidHalos(3);
							hilightSpoke(iSpeaker,player, true);
							message+=indirectFeedback[iSpeaker][2-iSuspect];
						}
						postMessage(message);
						break;
					}
					case npcRespondsDirectAffinity: { // display NPC affinity
						drawSolidHalos(3);
						hilightSpoke(callee,predicate, false);
						break;
					}
					case playerHangsUp: {
						postMessage("Goodbye!");
						break;
					}
					case npcTurn: {
						switch (subPhase) {
							case 0: case 2:{
								int iFace=(int)(cAffinityLevels*rand.nextDouble());
								drawFace(caller,iFace);
								break;
							}
							case 1: case 3:{
								int iFace=(int)(cAffinityLevels*rand.nextDouble());
								drawFace(callee,iFace);
								break;
							}
						}
						break;
					}
					case npcCallsPlayer: { // calling animation
						drawSolidHalos(2);
						postMessage(names[caller]+" is calling");
						break;
					}
					case npcDeclaresDirectAffinity: { // display NPC affinity
						drawSolidHalos(3);
						hilightSpoke(caller,predicate, false);
						drawFace(callee,9);
						break;
					}
					case playerRespondsDirectAffinity: { // player edits affinity
						drawSolidHalos(3);
						hilightSpoke(callee,predicate, false);
						break;
					}
					case npcHangsUp: { // goodbye animation
						postMessage("<Goodbye!>");
						break;
					}
					case playerDeclaresIndirectAffinity: { 
						drawSolidHalos(3);
						hilightSpoke(predicate, callee, player);
						break;
					}
					case npcRespondsIndirectAffinity: { 
						drawSolidHalos(3);
						hilightSpoke(predicate, player, callee);
						break;
					}
					case npcDeclaresIndirectAffinity: { 
						drawSolidHalos(3);
						hilightSpoke(predicate, player, caller);
						break;
					}
					case playerRespondsIndirectAffinity: { 
						drawSolidHalos(3);
						hilightSpoke(predicate, caller, player);
						break;
					}
				} // end of switch statement
				break;
			}
		}		
	}
//************************************************************
	private void drawButtons() {
		for (int i=0; (i<cButtons); ++i) {
			if (buttons[i].isfVisible()) {
				if ((buttons[i].isfPressed())|(buttons[i].isfRadioed())) {
					g2.drawImage(buttons[i].getPressedImage().getImage(),buttons[i].getX(),buttons[i].getY(),Color.black,this);				
				}
				else if (buttons[i].isfEnabled()) {
					g2.drawImage(buttons[i].getMainImage().getImage(),buttons[i].getX(),buttons[i].getY(),Color.black,this);				
				}
				else {
					g2.drawImage(buttons[i].getDisabledImage().getImage(),buttons[i].getX(),buttons[i].getY(),Color.black,this);				
				}
			}
		}		
	}
//************************************************************
	private void drawHexagon() {
		int arrowHeadSize;
		if (iTurn<maxTurns) {
			g2.setStroke(new BasicStroke(3));
			arrowHeadSize=5;
		}
		else {
			g2.setStroke(new BasicStroke(5));
			arrowHeadSize=7;
		}
		for (int i=0; (i<cCharacters); ++i) {
			for (int j=0; (j<cCharacters); ++j) {
				if (i!=j) {
					int iAffinity=cAffinityLevels; // this makes the arrow transparent
					if (iDisplay==endGameDisplay) {
						iAffinity=(int)(cAffinityLevels*((affinity[i][j]+1)/2));
					}
					else {
						if (difficultyLevel==0)
							iAffinity=(int)(cAffinityLevels*((affinity[i][j]+1)/2));							
						else
							iAffinity=(int)(cAffinityLevels*((perceivedAffinity[player][i][j]+1)/2));
					}
					double deltaX=hexagonX[j]-hexagonX[i];
					double deltaY=hexagonY[j]-hexagonY[i];
					double lengthX=deltaX*0.48;
					double lengthY=deltaY*0.48;
					int tipX=hexagonX[i]+(int)lengthX;
					int tipY=hexagonY[i]+(int)lengthY;
					g2.setColor(affinityLevelColor[iAffinity]);
					g2.drawLine(hexagonX[i],hexagonY[i],tipX,tipY);
					Line2D.Double line = new Line2D.Double(hexagonX[i],hexagonY[i],tipX,tipY);
					drawArrowHead(g2,line,arrowHeadSize);
					

				}
			}
			// draw the faces and names
			drawFace(i,4);
		}
	}
//************************************************************
	private void postMessage(String messageText) {
		g2.setFont(messageFont);
		int maxChars=25;
		g2.setColor(Color.black);
		g2.fillRect(messageX,messageY-25,frameWidth-messageX,60);		
		g2.setColor(Color.white);
		String line=messageText;
		int y=0;
		while (line.length()>maxChars) {
			String shortLine=line.substring(0,maxChars);
			int cChars=shortLine.length()-1;
			while (!shortLine.endsWith(" ")) {
				shortLine=shortLine.substring(0,--cChars);
			}
			g2.drawString(shortLine,messageX,messageY+y);
			y+=28;
			line=line.substring(cChars);
		}
		g2.drawString(line,messageX,messageY+y);
	}
// ************************************************************
	private void drawFace(int iWho, int iAffinity) {
		if (faces[iWho][0]!=null) {
			g2.drawImage(faces[iWho][iAffinity].getImage(),faces[iWho][0].getX(),faces[iWho][0].getY(),transparent,this);
			g2.setColor(Color.white);
			g2.setFont(nameFont);
			int nameX=0;
			if ((iWho==0)|(iWho==1)|(iWho==5))
				nameX=faces[iWho][0].getX()+140;
			else
				nameX=faces[iWho][0].getX()-80;
			if (player==iWho)
				g2.drawString("You",nameX,faces[iWho][0].getY()+75);
			else	
				g2.drawString(names[iWho],nameX,faces[iWho][0].getY()+75);					
		}
	}
// ************************************************************
	private void drawSolidHalos(int cHalos) {
		drawHalo(caller, Color.white, true);
		if (cHalos>1)
			drawHalo(callee, Color.blue, true);
		if (cHalos>2)
			drawHalo(predicate, Color.magenta, true);
	}
// ************************************************************
	private void hilightSpoke(int iFromCharacter, int iToCharacter, boolean fIsDashed) {
		// This version handles direct statements of affinity
	    float dash1[] = {10, 10};
	    int iAffinity=0;
	    if (fIsDashed) {
	   	 BasicStroke dashed = new BasicStroke(10.0f, BasicStroke.CAP_BUTT, 
	                               BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
			g2.setStroke(dashed);
			if ((iPhase==reactionAnimation1)|(iPhase==reactionAnimation3)) 
				iAffinity=feedbackFace[iLikeWhatIHear][iSuspect];
			else
				iAffinity=feedbackFace[2][iSuspect];
		}
		else {
			g2.setStroke(new BasicStroke(8));
			if (iFromCharacter==player)
				iAffinity=buttonValue;
			else
				iAffinity=getAffinityIndex(iFromCharacter,iToCharacter);
		}
	    try {
	   	 g2.setColor(affinityLevelColor[iAffinity]);
	    }
	    catch (Exception e) {
	   	 System.out.println("Error in highlightSpoke: iAffinity out of range: "+iAffinity);
	    }
		double deltaX=hexagonX[iToCharacter]-hexagonX[iFromCharacter];
		double deltaY=hexagonY[iToCharacter]-hexagonY[iFromCharacter];
		double lengthX=deltaX*0.48;
		double lengthY=deltaY*0.48;
		int tipX=hexagonX[iFromCharacter]+(int)lengthX;
		int tipY=hexagonY[iFromCharacter]+(int)lengthY;
		g2.drawLine(hexagonX[iFromCharacter],hexagonY[iFromCharacter],tipX,tipY);
		Line2D.Double line = new Line2D.Double(hexagonX[iFromCharacter],hexagonY[iFromCharacter],tipX,tipY);
		drawArrowHead(g2,line,11);
		drawFace(iFromCharacter,iAffinity);
		if (!fIsDashed) {
			if (iDisplay==mainDisplay) {
				String quote;
				if (iFromCharacter==player) {
					quote="You say that ";
				}
				else
					quote=names[iFromCharacter]+" says that ";
				quote+=names[iToCharacter]+" is ";
				// add description based on gender of iToCharacter
				if ((iToCharacter==1)|(iToCharacter==2)|(iToCharacter==4))
					quote+=guyDescription[iFromCharacter][iAffinity];
				else
					quote+=galDescription[iFromCharacter][iAffinity];
				quote+=".";
				postMessage(quote);
			}
		}
	}
// ************************************************************
	private void hilightSpoke(int iFromCharacter, int iToCharacter, int iSpeaker) {
		// This version handles indirect statements of affinity
		g2.setStroke(new BasicStroke(7));
		int iPerceivedAffinity=0;
		if (iSpeaker==player)
			iPerceivedAffinity=buttonValue;
		else
			iPerceivedAffinity=getPAffinityIndex(iSpeaker,iFromCharacter,iToCharacter);
//		g2.setColor(uncertainifyColor(iPerceivedAffinity, certainty[speaker][fromCharacter][toCharacter]));
		g2.setColor(affinityLevelColor[iPerceivedAffinity]);
		double deltaX=hexagonX[iToCharacter]-hexagonX[iFromCharacter];
		double deltaY=hexagonY[iToCharacter]-hexagonY[iFromCharacter];
		double lengthX=deltaX*0.48;
		double lengthY=deltaY*0.48;
		int tipX=hexagonX[iFromCharacter]+(int)lengthX;
		int tipY=hexagonY[iFromCharacter]+(int)lengthY;
		g2.drawLine(hexagonX[iFromCharacter],hexagonY[iFromCharacter],tipX,tipY);
		Line2D.Double line = new Line2D.Double(hexagonX[iFromCharacter],hexagonY[iFromCharacter],tipX,tipY);
		drawArrowHead(g2, line,11);
		drawFace(iFromCharacter,iPerceivedAffinity);

		String quote;
		if (iSpeaker==player)
			quote="You say \"";
		else 
			quote=names[iSpeaker]+" says \"";
		quote+=names[iFromCharacter]+" told me that you are ";
		// add description based on gender of iToCharacter
		if ((iToCharacter==1)|(iToCharacter==2)|(iToCharacter==4))
			quote+=guyDescription[iFromCharacter][iPerceivedAffinity];
		else
			quote+=galDescription[iFromCharacter][iPerceivedAffinity];
		quote+=".\"";
		postMessage(quote);
		
		// now add the big quotation marks around the face
		g2.drawImage(quotationMarks,faces[iFromCharacter][0].getX()-28,faces[iFromCharacter][0].getY()-10,transparent,this);
		
	}
// ************************************************************
	private void drawHalo(int iCharacter, Color haloColor, boolean fullHalo) {
		double angle=(double)(60*iCharacter)*Math.PI/180+Math.PI/2;  // convert 60¼ steps to radians
		double dx=centerX+radius*Math.sin(angle);
		double dy=centerY+radius*Math.cos(angle);
		int x=(int)dx-haloSize/2;
		int y=(int)dy-haloSize/2;
		g2.setColor(haloColor);
		g2.setStroke(new BasicStroke(3));
		if (fullHalo)
			g2.fillOval(x,y,haloSize,haloSize);
		else
			g2.drawOval(x+2,y+2,haloSize-5,haloSize-5);
		drawFace(iCharacter,4);
	}
// ************************************************************
	private void drawArrowHead(Graphics2D g2d, Line2D.Double line, int size) {  
		AffineTransform tx = new AffineTransform();
		Polygon arrowHead = new Polygon();  
		arrowHead.addPoint( 0,size);
		arrowHead.addPoint( -size, -size);
		arrowHead.addPoint(size,-size);
	    tx.setToIdentity();
	    double angle = Math.atan2(line.y2-line.y1, line.x2-line.x1);
	    tx.translate(line.x2, line.y2);
	    tx.rotate((angle-Math.PI/2d));  

	    Graphics2D g = (Graphics2D) g2d.create();
	    g.setTransform(tx);   
	    g.fill(arrowHead);
	    g.dispose();
	}		
// ************************************************************
	/*
	 * This ends the section of code dedicated to graphic display.
	 * The next section contains the algorithms that drive the smarts of the game.
	 * This is all number-crunching with no references to anything peculiar
	 * to input or graphics.
	 * All the code from here to the end of the program is confidential
	 *   and shall not be communicated to any party.
	 */
// ************************************************************
	private double blend(double from, double to, double weight) {
		if (weight<=-1) weight=-1;
		if (weight>=1) weight=1;
		// this is a conversion from BNumber to UNumber
		double uWeightingFactor = 1-((1-weight)/2);
		double x = to*uWeightingFactor + from*(1.0f-uWeightingFactor);	
		return x;
	}
//**********************************************************************	 
	public double bSum(double a, double b) {
		double x2 = boundedInverseTransform(a);
		double x1 = boundedInverseTransform(b);
		return boundedTransform(x1+x2);
	}
//**********************************************************************	 
	double boundedInverseTransform(double boundedNumber) {
		if (boundedNumber > 0.0f)
			return (1.0f / (1.0f - boundedNumber)) -1.0f;
		else
			return 1.0f - (1.0f / (1.0f + boundedNumber));
	}
//**********************************************************************	
	private double boundedTransform(double unboundedNumber) {
		if (unboundedNumber > 0.0f)
			return 1.0 - (1.0 / (1.0 + unboundedNumber));
		else
			return (1.0 / (1.0 - unboundedNumber)) -1.0;
		}
// ************************************************************
	private int getAffinityIndex(int iFromCharacter, int iToCharacter) {
		int AffinityIndex=boundedToInteger(affinity[iFromCharacter][iToCharacter]);
		return AffinityIndex;
	}
// ************************************************************
	private int getPAffinityIndex(int iPerceiver, int iFromCharacter, int iToCharacter) {
		int iAffinity=boundedToInteger(perceivedAffinity[iPerceiver][iFromCharacter][iToCharacter]);
		return iAffinity;
	}
// ************************************************************
	private void declareIndirectAffinity(int iValue, 
														int tiSource, 
														int tiSpeaker, 
														int tiListener, 
														int tiPredicate) {
		// Record the declaration in the history book
		historyBook.add(new History(tiSpeaker, tiListener, tiSource, tiPredicate, iValue));
		
		setSuspiciousness(tiSpeaker, tiSource, tiPredicate, tiListener);
		iLikeWhatIHear=iValue/3; // flattery works
		// change affinity based on agreement
		double before=affinity[tiListener][tiSpeaker];
		// scale down magnitude of flattery effect
		double flattery=vain[tiListener]*(integerToBounded(iValue)-perceivedAffinity[tiListener][tiSource][tiListener])/4;
		affinity[tiListener][tiSpeaker]=bSum(affinity[tiListener][tiSpeaker],0.08-flattery);
		if (fDebug) 
			flatteryDamage+=affinity[tiListener][tiSpeaker]-before;
		iLikeWhatIHear=(cAffinityLevels-boundedToInteger(flattery))/3;
	}
// ************************************************************
	private void declareAffinity(int iValue, int tiSpeaker, int tiListener, int tiPredicate) {
		// Record the declaration in the history book
		historyBook.add(new History(tiSpeaker, tiListener, tiSpeaker, tiPredicate, iValue));

		double bValue=integerToBounded(iValue);
		if (tiSpeaker==player) {
			affinity[tiSpeaker][tiPredicate]=bValue;
			perceivedAffinity[tiSpeaker][tiSpeaker][tiPredicate]=bValue;
		}
		
		setSuspiciousness(tiSpeaker, tiSpeaker, tiPredicate, tiListener);
		double disagreement=(Math.abs(bValue-affinity[tiListener][tiPredicate]))/4;
		// change affinity based on agreement
		double before=affinity[tiListener][tiSpeaker];
		affinity[tiListener][tiSpeaker]=bSum(affinity[tiListener][tiSpeaker],0.08-disagreement);
		if (fDebug) {
			if ((tiSpeaker==0))
				System.out.println(disagreement+"  "+before+"  "+affinity[tiListener][tiSpeaker]);
			flatteryDamage+=affinity[tiListener][tiSpeaker]-before;
		}
		iLikeWhatIHear=(cAffinityLevels-boundedToInteger(disagreement))/3;
	}
// ************************************************************
	private void setSuspiciousness(int tiSpeaker, int tiSource, int tiPredicate, int tiListener) {
		ArrayList<History> pastTestimony=new ArrayList<History>();
		for (int i=0; (i<historyBook.size()); ++i) {
			History h=historyBook.get(i);
			if ((tiListener==h.getiListener())&(tiSource==h.getiSource())&(tiPredicate==h.getiPredicate()))
				pastTestimony.add(h);
		}
		// there will always be at least one entry in pastTestimony
		double meanAffinity=perceivedAffinity[tiListener][tiSource][tiPredicate];
		double sumWeight=1;
		for (int i=0; (i<pastTestimony.size()); ++i) {
			History h=pastTestimony.get(i);
			double trust=(affinity[tiListener][h.getiSpeaker()]+1)/2; // make it unitary
			if (h.getiSpeaker()==h.getiSource())
				trust=bSum(trust,trust);
			sumWeight+=trust;
			double x=integerToBounded(pastTestimony.get(i).getiValue());
			meanAffinity+=trust*x;
		}
		if (sumWeight>0) // in difficulty level 2, it is possible for sumWeight to be zero
			meanAffinity/=sumWeight;
		
		// Calculate the deviations from all previous statements on the 
		double dSuspect=0;
		for (int i=0; (i<pastTestimony.size()); ++i) {
			History h=pastTestimony.get(i);	
			double deviation=Math.abs(meanAffinity-integerToBounded(h.getiValue()));
			// apply gullibility
			deviation*=1-gullible[tiListener];
			dSuspect+=deviation;
			double before=affinity[tiListener][h.getiSpeaker()];
			affinity[tiListener][h.getiSpeaker()]=bSum(affinity[tiListener][h.getiSpeaker()],0.1-deviation);
			if (fDebug) {
				if (h.getiSpeaker()==0) {
					System.out.println(tiListener+"  "+deviation+" "
							+perceivedAffinity[tiListener][h.getiSource()][h.getiPredicate()]
						+"  "+integerToBounded(h.getiValue())+" "+before+"  "
							+affinity[tiListener][h.getiSpeaker()]);
				}
				suspectDamage+=affinity[tiListener][h.getiSpeaker()]-before;
			}
		}
		iSuspect=(int)(10*dSuspect);
		if (iSuspect>2) iSuspect=2;
		perceivedAffinity[tiListener][tiSource][tiPredicate]=meanAffinity;
	}
// ************************************************************
	private void calculatePopularity() {
		for (int i=0; (i<cCharacters); ++i) {
			popularity[i][iTurn]=0;;
			for (int j=0; (j<cCharacters); ++j) {
				if (j!=i)
					popularity[i][iTurn]+=affinity[j][i]/(cCharacters-1);
			}
		}	
		if (fDebug) {
			double grandSum=0;
			for (int i=0; (i<cCharacters); ++i) {
				for (int j=0; (j<cCharacters); ++j) {
					grandSum+=affinity[i][j]*affinity[i][j];
				}
			}
			System.out.println("Grand Sum = "+grandSum+" flater: "+flatteryDamage+"  susp: "+suspectDamage);
		}
	}
// ************************************************************
	private void runNPCTurn() {
		subPhase=4;
		selectCallee();
		selectPredicate();
		if (callee==player) {
			iPhase=npcCallsPlayer;
			playSound("Ringtones/"+names[caller], false);
			npcCallsPlayerTimer.start();
		}
		else { // communicate affinity
			subPhase=0;
			playSound("psst", false);
			secretNPCHangsUpTimer.start();
			declareAffinity(boundedToInteger(planDirectReport(caller, callee)), caller, callee, predicate);
			declareAffinity(boundedToInteger(planDirectReport(callee, caller)), callee, caller, predicate);
			declareIndirectAffinity(boundedToInteger(planIndirectReport(caller,callee)),predicate, caller, callee, callee);
			declareIndirectAffinity(boundedToInteger(planIndirectReport(callee,caller)), predicate, callee, caller, caller);
		}
	}			
// ************************************************************
	private double planDirectReport(int iSpeaker, int iListener) {
		// Decides what iSpeaker will say to listener about predicate
		
		// These intermediate variables exist only for readability
		double trueValue=affinity[iSpeaker][predicate];
		double lieValue=perceivedAffinity[iSpeaker][iListener][predicate];
		
		// I'm more honest to my friends
		double bias=bSum(dishonest[iSpeaker],-affinity[iSpeaker][iListener]);
		
		// now adjust for difficulty level
		bias=bSum(bias,difficultyLevel/4);
		
		// Tell 'em what they want to hear
		return blend(trueValue,lieValue,bias);
	}
// ************************************************************
	private double planIndirectReport(int iSpeaker, int iListener) {
		// Decides what iSpeaker will say to listener about predicate's feelings for listener
		// Warning: this method is identical in form to the above,
		//          but the array indeces are different!
		
		// These intermediate variables exist only for readability
		double lieValue=affinity[iSpeaker][iListener];
		double trueValue=perceivedAffinity[iSpeaker][predicate][iListener];
		
		// My dishonesty is to make it sound as if the predicate shares my feelings
		// I'm more honest to my friends
		double bias=bSum(dishonest[iSpeaker],-affinity[iSpeaker][iListener]);
		
		// now adjust for difficulty level
		bias=bSum(bias,difficultyLevel/4);
		
		// Tell 'em what they want to hear
		return blend(trueValue,lieValue,bias);
	}
// ************************************************************
	private void nextPerson1() {
		callee=nobody;
		++caller;
		if (caller==cCharacters) {
			caller=player;
			++iTurn;
			calculatePopularity();
			iPhase=playerSelectsCallee;
			if (iTurn==maxTurns) { // end of game!
				iDisplay=endGameDisplay;
				buttons[upArrow].setfVisible(false);
				buttons[downArrow].setfVisible(false);
				buttons[tips].setfVisible(false);
				buttons[enter].setfVisible(false);
				buttons[exit].setfVisible(true);
				buttons[playAgain].setfVisible(true);
				buttons[exit].setfEnabled(true);
				buttons[playAgain].setfEnabled(true);
			}
		}
		else
			runNPCTurn();
	}
// ************************************************************
	private void selectCallee() {
		double bestFoM=0;
		int bestCallee=nobody;
		for (int i=0; (i<cCharacters); ++i) {
			if (i!=caller) {
				double age=Math.min(howRecent(caller,i,anybody),howRecent(i,caller,anybody))+rand.nextDouble();
				double foM=age;//+2*affinity[caller][i];
				if ((howRecent(caller,callee,i)<(historyBook.size()+1))&(foM>bestFoM)) {
					bestFoM=foM;
					bestCallee=i;
				}
			}
		}
		callee=bestCallee;
		return;		
	}
// ************************************************************
	private void selectPredicate() {
		double oldestCall=0;
		int oldestPredicate=nobody;
		for (int i=0; (i<cCharacters); ++i) {
			if ((i!=caller)&(i!=callee)) {
				// the random term allows random selection of otherwise equal candidates
				double age=Math.min(howRecent(caller,callee,i),howRecent(callee,caller,i))+rand.nextDouble();
				if (age>oldestCall) {
					oldestCall=age;
					oldestPredicate=i;
				}
			}
		}
		predicate=oldestPredicate;
		return;
	}
// ************************************************************
	private double howRecent(int iSpeaker, int iListener, int iPredicate) {
		boolean fSpeakerHit;
		boolean fListenerHit;
		boolean fPredicateHit;
		boolean fTrifecta;
		int i=historyBook.size();
		do { 
			--i;
			History theEvent=historyBook.get(i);
			fSpeakerHit=(iSpeaker==theEvent.iSpeaker);
			fListenerHit=(iListener==theEvent.iListener);
			fPredicateHit=(iPredicate==theEvent.iPredicate)|(iPredicate==anybody);
			fTrifecta=fSpeakerHit&fListenerHit&fPredicateHit;
		}
		while ((i>0)&!fTrifecta);
		return historyBook.size()-i;
	}
// ************************************************************
	private double integerToBounded(int tInteger) {
		double affinity=(2*(double)tInteger/cAffinityLevels)-1;
		// snip off extreme values
		if (affinity==-1) affinity=-0.98;
		if (affinity==1) affinity=0.98;
		return affinity;
	}
// ************************************************************
	private int boundedToInteger(double tBounded) {
	int statement= (int)(cAffinityLevels*(1+tBounded)/2);
	return statement;
}
// ************************************************************	
	private void printTurnStats() {
		// for debugging purposes only
		double aveDeviance=0;
		double aveWeight=0;
		double aveAffinity=0;
		for (int i=0; (i<cCharacters); ++i) {
			for (int j=0; (j<cCharacters); ++j) {
				if (i!=j) {
					aveAffinity+=affinity[i][j]/30;
					for (int k=0; (k<cCharacters); ++k) {
						if (k!=j) {
							aveDeviance+=Math.pow((perceivedAffinity[i][j][k]-affinity[j][k]),2)/150;
						}
					}
				}
			}
		}
		System.out.println("AveDeviance= "+aveDeviance+" aveWeight+ "+aveWeight+" aveAffinity= "+aveAffinity+" aff: "+affinity[3][0]);
	}
	
// ************************************************************
	public static void main(String args[]) {
		Gossip theGame=new Gossip();
		theGame.initialize();

		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		}
		catch(Exception evt) {}
		do {
			while (!theGame.fQuit) { 
				try { Thread.sleep(10); } catch (Exception e) {} 
			}
		}
		while (!theGame.fQuit);
		System.exit(0);
	}
// ************************************************************
}
