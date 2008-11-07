package example;

import java.io.IOException;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;
import javax.microedition.midlet.MIDlet;

import jp.sourceforge.qrcode.QRCodeDecoder;
import jp.sourceforge.qrcode.data.QRCodeImage;
import jp.sourceforge.qrcode.exception.DecodingFailedException;
import jp.sourceforge.qrcode.geom.Line;
import jp.sourceforge.qrcode.geom.Point;
import jp.sourceforge.qrcode.util.DebugCanvas;

// Example of Midlet QR Code reader application

public class QRCodeDecoderMIDletExample extends MIDlet{
	private CameraCanvas cameraCanvas = null;
	private DisplayCanvas displayCanvas = null;
	private DecodedTextBox decodedTextBox = null;
	public QRCodeDecoderMIDletExample() {}
	
	public void startApp() {
		Displayable current = Display.getDisplay(this).getCurrent();
		if (current == null) {
			cameraCanvas = new CameraCanvas(this);
			displayCanvas = new DisplayCanvas(this);
			decodedTextBox = new DecodedTextBox(this);
			Display.getDisplay(this).setCurrent(cameraCanvas);
			cameraCanvas.start();
		} else {
			if (current == cameraCanvas) {
				cameraCanvas.start();
			}
			Display.getDisplay(this).setCurrent(current);
		}
		
	}
	public void pauseApp() {
		if (Display.getDisplay(this).getCurrent() == cameraCanvas)  {
			cameraCanvas.stop();
		}
	}
	public void destroyApp(boolean b) {
		if (Display.getDisplay(this).getCurrent() == cameraCanvas) {
			cameraCanvas.stop();
		}
	}
	private void exitRequested() {
		destroyApp(false);
		notifyDestroyed();
	}
	void cameraCanvasExit() {
		exitRequested();
	}
	void cameraCanvasCaptured(byte[] pngData) {
		cameraCanvas.stop();
		displayCanvas.setImage(pngData);
		Display.getDisplay(this).setCurrent(displayCanvas);
		Image image = Image.createImage(pngData, 0, pngData.length);
		// TODO Uncomment below for demo on emulator
		try { image = Image.createImage("/qrcode.jpg"); } catch (IOException ioe) {}
		QRCodeDecoder decoder = new QRCodeDecoder();
		QRCodeDecoder.setCanvas(displayCanvas);
		try {
			decodedTextBox.setDecodedString(new String(decoder.decode(new J2MEImage(image))));
		} catch (DecodingFailedException dfe) {
			displayCanvas.println("Decoding failed");
			displayCanvas.println("("+dfe.getMessage()+")");
			displayCanvas.println("--------");
			return;
		}
		displayCanvas.println("--------");
		displayCanvas.addViewDecodedStringCommand();
	}
	void displayCanvasBack() {
		Display.getDisplay(this).setCurrent(cameraCanvas);
		cameraCanvas.start();
	}
	void decodedTextBoxBack() {
		Display.getDisplay(this).setCurrent(displayCanvas);
	}
	void toDecodedTextBox() {
		Display.getDisplay(this).setCurrent(decodedTextBox);
	}
}

class CameraCanvas extends Canvas implements CommandListener {
	private final QRCodeDecoderMIDletExample midlet;
	private final Command exitCommand;
	private Command captureCommand = null;
	private Player player = null;
	private VideoControl videoControl = null;
	
	private boolean active = false;
	
	private String message1 = null;
	private String message2 = null;
	
	
	public CameraCanvas(QRCodeDecoderMIDletExample midlet) {
		this.midlet = midlet;
		exitCommand = new Command("Exit", Command.EXIT, 1);
		addCommand(exitCommand);
		setCommandListener(this);
		try {
			player = Manager.createPlayer("capture://video");
			player.realize();
			videoControl = (VideoControl)player.getControl("VideoControl");
			if (videoControl == null) {
				discardPlayer();
				message1 = "Unsupported:";
				message2 = "Can't get video control";
			} else {
				videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, this);
				captureCommand = new Command("Capture", Command.SCREEN, 1);
				addCommand(captureCommand);
				
			}
		} catch (IOException ioe) {
			discardPlayer();
			message1 = "IOException:";
			message2 = ioe.getMessage();
		} catch (MediaException me) {
			discardPlayer();
			message1 = "MediaException:";
			message2 = me.getMessage();
		} catch (SecurityException se) {
			discardPlayer();
			message1 = "SecurityException";
			message2 = se.getMessage();
		}
		
	}
	private void discardPlayer() {
		if (player != null) {
			player.close();
			player = null;
		}
		videoControl = null;
	}
	public void paint(Graphics g) {
		g.setColor(0xFFFFFF);
		g.fillRect(0, 0, getWidth(), getHeight());
		if (message1 != null) {
			g.setColor(0x000000);
			g.drawString(message1, 1, 1, Graphics.TOP | Graphics.LEFT);
			g.drawString(message2, 1, 1 + g.getFont().getHeight(), Graphics.TOP | Graphics.LEFT);
		}
	}
	synchronized void start() {
		if ((player != null) && !active) {
			try {
				player.start();
				videoControl.setVisible(true);
			} catch (MediaException me) {
				message1 = "Media exception:";
				message2 = me.getMessage();
			} catch (SecurityException se) {
				message1 = "SecurityException";
				message2 = se.getMessage();
			}
			active = true;
			
		}
	}
	synchronized void stop() {
		if ((player != null) && active) {
			try {
				videoControl.setVisible(false);
				player.stop();
			} catch (MediaException me) {
				message1 = "MediaException:";
				message2 = me.getMessage();
			}
			active = false;
		}
	}
	public void commandAction(Command c, Displayable d) {
		if (c == exitCommand) {
			midlet.cameraCanvasExit();
		} else if (c == captureCommand) {
			takeSnapshot();
		}
	}
	public void keyPressed(int keyCode) {
		if (getGameAction(keyCode) == FIRE) {
			takeSnapshot();
		}
	}
	private void takeSnapshot() {
		if (player != null) {
			try {
				byte[] pngImage = videoControl.getSnapshot(null);
				midlet.cameraCanvasCaptured(pngImage);
			} catch (MediaException me) {
				message1 = "MediaException;";
				message2 = me.getMessage();
			}
				
		}
	}
}

class DisplayCanvas extends Canvas implements CommandListener, DebugCanvas {
	private final QRCodeDecoderMIDletExample midlet;
	private Image image = null;
	private Command viewDecodedStringCommand = null;
	private String[] log = null;
	private int numCols;
	private int numRows;
	Font logFont = null;
	DisplayCanvas(QRCodeDecoderMIDletExample midlet) {
		this.midlet = midlet;
		addCommand(new Command("Camera", Command.BACK, 1));
		setCommandListener(this);
		logFont = Font.getDefaultFont();
		numRows = this.getHeight() / logFont.getHeight();
		numCols = this.getWidth() / logFont.charWidth('_');
		log = new String[numRows];
	}
	public void paint(Graphics g) {
		g.setColor(0xFFFFFF);
		g.fillRect(0, 0, getWidth(), getHeight());
		if (image != null) {
			g.drawImage(image, getWidth()/2, getHeight()/2, Graphics.VCENTER | Graphics.HCENTER);
		}
		g.setColor(0x000000);
		for (int i = 0; i < log.length; i++) {
			if (log[i] != null) {
				g.drawString(log[i], 0, i*logFont.getHeight(), Graphics.TOP|Graphics.LEFT);
				
			}
		}
	}
	void setImage(byte[] pngImage) {
		image = Image.createImage(pngImage, 0, pngImage.length);
	}
	void addViewDecodedStringCommand() {
		if (viewDecodedStringCommand == null) {
			viewDecodedStringCommand = new Command("Result", Command.SCREEN, 1);
			addCommand(viewDecodedStringCommand);
		}
	}
	public void commandAction(Command c, Displayable d) {
		if (c == viewDecodedStringCommand) {
			midlet.toDecodedTextBox();
		} else {
			midlet.displayCanvasBack();
		}
	}
	public void drawCross(Point point, int color) {
		Image bufImage = Image.createImage(image.getWidth(), image.getHeight());
		Graphics g = bufImage.getGraphics();
		g.drawImage(image, 0, 0, 0);
		g.setColor(color);
		g.drawLine(point.getX()-5, point.getY(), point.getX()+5, point.getY());
		g.drawLine(point.getX(), point.getY()-5, point.getX(), point.getY()+5);
		image = bufImage;
		repaint();	
	}
	public void drawLine(Line line, int color) {
		Image bufImage = Image.createImage(image.getWidth(), image.getHeight());
		Graphics g = bufImage.getGraphics();
		g.drawImage(image, 0, 0, 0);
		g.setColor(color);
		g.drawLine(line.getP1().getX(), line.getP1().getY(), 
				line.getP2().getX(), line.getP2().getY());
		image = bufImage;
		repaint();
	}
	public void drawLines(Line[] lines, int color) {
		Image bufImage = Image.createImage(image.getWidth(), image.getHeight());
		Graphics g = bufImage.getGraphics();
		g.drawImage(image, 0, 0, 0);
		g.setColor(color);
		
		for (int i = 0; i < lines.length - 1; i++) {
			g.drawLine(lines[i].getP1().getX(), lines[i].getP1().getY(), 
					lines[i].getP2().getX(), lines[i].getP2().getY());
		}
		image = bufImage;
		repaint();
	}
	public void drawMatrix(boolean[][] matrix) {
		Image bufImage = Image.createImage(image.getWidth(), image.getHeight());
		Graphics g = bufImage.getGraphics();
		g.setColor(0xCCCCCC);
		for (int y = 0; y < matrix[0].length; y++) {
			for (int x = 0; x < matrix.length; x++) {
				if (matrix[x][y] == true)
					g.drawLine(x, y, x+1, y);
			}
		}
		image = bufImage;
		repaint();
	}
	public void drawPoint(Point point, int color) {
/*		Image bufImage = Image.createImage(image.getWidth(), image.getHeight());
		Graphics g = bufImage.getGraphics();
		g.drawImage(image, 0, 0, 0);
		g.setColor(color);
		g.drawLine(point.getX(), point.getY(),
				point.getX()+1, point.getY());
		image = bufImage;
		repaint();*/
	}
	public void drawPoints(Point[] points, int color) {		
		Image bufImage = Image.createImage(image.getWidth(), image.getHeight());
		Graphics g = bufImage.getGraphics();
		g.drawImage(image, 0, 0, 0);
		g.setColor(color);
		for (int i = 0; i < points.length - 1; i++) {
			g.drawLine(points[i].getX(), points[i].getY(),
					points[i].getX()+1, points[i].getY());
		}
		image = bufImage;
		repaint();
	}
	public void drawPolygon(Point[] points, int color) {
		Image bufImage = Image.createImage(image.getWidth(), image.getHeight());
		Graphics g = bufImage.getGraphics();
		g.drawImage(image, 0, 0, 0);
		g.setColor(color);
		int i = 0;
		for (; i < points.length - 1; i++) {
			g.drawLine(points[i].getX(), points[i].getY(), points[i+1].getX(), points[i+1].getY());
		}
		g.drawLine(points[i].getX(), points[i].getY(), points[0].getX(), points[0].getY());
		image = bufImage;
		repaint();		
	}
	public void println(String message) {
		System.out.println(message);
		int numParts = message.length() / numCols;
		if (message.length() % numCols > 0) {
			numParts += 1;
		}
		String[] lineStrings = new String[numParts];
		int offset = 0;
		for (int i = 0; i < numParts - 1; i++) {
			lineStrings[i] = message.substring(offset, offset+numCols);
			offset += numCols;
		}
		lineStrings[numParts - 1] = message.substring(offset);
		int numLoggedLine = 0;
		if (log[log.length - numParts] == null) {
			for (int i = 0; i < log.length; i++) {
				if (log[i] == null) {
					log[i] = lineStrings[numLoggedLine];
					numLoggedLine += 1;
					if (numLoggedLine == lineStrings.length)
						break;
				}
			}
		} else {
			int i;
			for (i = 0; i < log.length - numParts; i++) {
				log[i] = log[i+numParts];
			}
			
			for (int start = i; i < log.length; i++) {
				log[i] = lineStrings[i - start];
			}
				
		}
		repaint();
	}
}

class DecodedTextBox extends TextBox implements CommandListener {
	QRCodeDecoderMIDletExample midlet;
	public DecodedTextBox(QRCodeDecoderMIDletExample midlet) {
		super("Decoded String", "", 2048, TextField.ANY);
		this.midlet = midlet;
		addCommand(new Command("Back", Command.BACK, 1));
		setCommandListener(this);
	}
	public void setDecodedString(String decodedString) {
		this.setString(decodedString);
	}
	public void commandAction(Command c, Displayable d) {
		midlet.decodedTextBoxBack();
	}
}

class J2MEImage implements QRCodeImage {
	Image image;
	int[] intImage;
	public J2MEImage(Image image) {
		this.image = image;
		intImage = new int[image.getWidth()*image.getHeight()];
		image.getRGB(this.intImage, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
	}
	public int getHeight() {
		return image.getHeight();
	}
	public int getWidth() {
		return image.getWidth();
	}
	public int getPixel(int x, int y) {
		return intImage[x + y*image.getWidth()];
	}
}
