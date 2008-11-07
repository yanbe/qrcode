package jp.sourceforge.qrcode.example.jmf;

// NOTE: main class is in jmfexample.java

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.Buffer;
import javax.media.Processor;

import javax.media.control.FrameGrabbingControl;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;

import jp.sourceforge.qrcode.QRCodeDecoder;
import jp.sourceforge.qrcode.data.QRCodeImage;
import jp.sourceforge.qrcode.exception.DecodingFailedException;
import jp.sourceforge.qrcode.geom.Line;
import jp.sourceforge.qrcode.geom.Point;
import jp.sourceforge.qrcode.util.DebugCanvas;



public class QRCodeDecoderJMFExample implements Runnable {
	Processor processor;
	QRCodeDecoder decoder;
	JMFCanvas canvas;
	public QRCodeDecoderJMFExample(Processor processor) {
		this.processor = processor;
	}
	
	public BufferedImage getDebugImage() {
		return canvas.getImage();
	}
	
	public  void run() {
		for (;;) {
	        FrameGrabbingControl fgc = (FrameGrabbingControl) processor.getControl("javax.media.control.FrameGrabbingControl");
	        Buffer buf = fgc.grabFrame();
	        BufferToImage btoi = new BufferToImage((VideoFormat)buf.getFormat());
	        Image img = btoi.createImage(buf);
	        QRCodeDecoder decoder = new QRCodeDecoder();
	        int width = processor.getVisualComponent().getWidth();
	        int height = processor.getVisualComponent().getHeight();
	        canvas = new JMFCanvas();
	        QRCodeDecoder.setCanvas(canvas);
	        try {
	        	J2SEImage decoderImage = new J2SEImage(img, width, height);
	        	String decodedString = new String(decoder.decode(decoderImage));
	        	System.out.println("Result: "+ decodedString);
	        	File resultImage = new File("C:\\tmp\\result.jpg");
	        	try {
	        		ImageIO.write(canvas.getImage(), "png", resultImage);
	        	} catch (IOException e) {
	        		System.out.println(e.getMessage());
	        	}
	        } catch (DecodingFailedException e) {
	        	System.out.println("Error: "+e.getMessage());
	        } catch (IllegalStateException e) {
	        	System.out.println("Error: "+e.getMessage());	        	
	        }
	        try {
	        	Thread.sleep(500);
	        } catch (InterruptedException e) {
	        	System.out.println(e.getMessage());
	        }
		}
	}
	
}
class J2SEImage implements QRCodeImage {
	BufferedImage image;
	int[] pixels;
	PixelGrabber pg;
	public J2SEImage(Image img, int width, int height) {
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = image.createGraphics();
		g2.drawImage(img, null, null);
	}

	public int getWidth() {
		return image.getWidth();
	}
	
	public int getHeight() {
		return image.getHeight();
	}

	public int getPixel(int x, int y) {
		return image.getRGB(x, y);
	}
}

class JMFCanvas extends Canvas implements DebugCanvas {
	BufferedImage image;
	public void paint(Graphics g){
		if (image != null)
			g.drawImage(image, 0, 0, java.awt.Color.WHITE, null);
	}
	
	
	public  void println(String string){
		//System.out.println(string);
	}
	
	public  void drawMatrix(boolean[][] matrix) {
		if (image == null) {
			image = new BufferedImage(matrix.length, matrix[0].length, BufferedImage.TYPE_INT_ARGB);
			setSize(matrix.length, matrix[0].length);
		}
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(java.awt.Color.WHITE);
		int width = getWidth();
		for (int x = 0; x < matrix.length; x++) {
			g2d.drawLine(x, 0, x, width);
		}
		g2d.setColor(java.awt.Color.BLACK);
		for (int x = 0; x < matrix.length; x++) {
			for (int y = 0; y < matrix[0].length; y++) {
				if (matrix[x][y] == true)
					g2d.drawLine(x, y, x, y);
			}
		}
		repaint();
	}

	public  void drawLine(Line line, int color){
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(new Color(color));
		g2d.drawLine(line.getP1().getX(), line.getP1().getY(),
					line.getP2().getX(), line.getP2().getY());
		repaint();
	}

	public  void drawLines(Line[] lines, int color){
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(new Color(color));
		for (int i = 0; i < lines.length; i++) {
			g2d.drawLine(lines[i].getP1().getX(), lines[i].getP1().getY(),
					lines[i].getP2().getX(), lines[i].getP2().getY());
		}
		repaint();
	}
	
	public  void drawPolygon(Point[] points, int color){
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(new Color(color));
		int numPoints = points.length;
		int[] polygonX = new int[numPoints];
		int[] polygonY = new int[numPoints];
		for (int i = 0; i < numPoints; i++) {
			polygonX[i] = points[i].getX();
			polygonY[i] = points[i].getY();
		}
		g2d.drawPolygon(polygonX, polygonY, numPoints);
		repaint();
	}

	public  void drawPoints(Point[] points, int color){
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(new Color(color));
		for (int i = 0; i < points.length; i++)
			g2d.drawLine(points[i].getX(), points[i].getY(),points[i].getX(), points[i].getY());
		repaint();

	}


	public  void drawPoint(Point point, int color){
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(new Color(color));
		g2d.drawLine(point.getX(), point.getY(),point.getX(), point.getY());
		repaint();

	}

	public  void drawCross(Point point, int color){
		int x = point.getX();
		int y = point.getY();

		Line[] lines = {
			new Line(x - 5, y-1, x + 5, y-1),new Line(x-1, y - 5, x-1 ,y + 5),
			new Line(x - 5, y+1, x + 5, y+1),new Line(x+1, y - 5, x+1 ,y + 5),
			new Line(x - 5, y, x + 5, y),new Line(x, y - 5, x ,y + 5)
		};
		drawLines(lines, color);
	}
	public BufferedImage getImage() {
		return image;
	}
	public void setImage(BufferedImage image) {
		this.image = image;
	}
}
