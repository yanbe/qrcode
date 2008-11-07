//package jp.sourceforge.qrcode.example;
package example;

import java.io.*;

import java.net.URL;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.*;
import jp.sourceforge.qrcode.*;
import jp.sourceforge.qrcode.data.QRCodeImage;
import jp.sourceforge.qrcode.exception.DecodingFailedException;
import jp.sourceforge.qrcode.util.*;

import jp.sourceforge.qrcode.geom.Line;
import jp.sourceforge.qrcode.geom.Point;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

// Example of GUI QRCode reader application

public class QRCodeDecoderGUIExample extends JFrame implements ActionListener {
	JMenuBar menuBar;
	JMenu fileMenu;
	JMenuItem openMenu;
	JTextField url;
	JButton button;
	JFileChooser chooser;
	BufferedImage sourceImage;
	JLabel sourceImageLabel;
	J2SEDebugCanvas canvas;
	JTextArea decodedText;
	static final long serialVersionUID = 1;
	
	class J2SEImage implements QRCodeImage {
		BufferedImage image;

		public J2SEImage(BufferedImage image) {
			this.image = image;
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
	
	QRCodeDecoderGUIExample(String[] args) {
		System.out.println("Starting QRCode Decoder GUI Example ...");
		setSize(400,400);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		menuBar = new JMenuBar();
		openMenu = new JMenuItem("Open Image");
		openMenu.addActionListener(this);
		menuBar.add(openMenu);
		setJMenuBar(menuBar);
		url = new JTextField(20);
		url.setText("(Or input image url here.)");
		button = new JButton("Open from URL");
		button.addActionListener(this);
		JPanel urlPanel = new JPanel();
		urlPanel.add(url);
		urlPanel.add(button);
		button = new JButton("URL");
		getContentPane().add(urlPanel, BorderLayout.NORTH);
		chooser =  new JFileChooser("Open QR Code Image");
		chooser.setFileFilter(new ImageFileFilter());
		setLocation(300, 200);
		url.select(0, url.getText().length());
		setVisible(true);
    if (args.length == 1)
      decode(args[0]);
	}

  public void decode(String filename) {
    try {
	    sourceImage = ImageIO.read(new File(filename));
    } catch (IOException e) {
      ;
    }
		if (sourceImageLabel != null)
			getContentPane().remove(sourceImageLabel);
		
		sourceImageLabel = new JLabel(new ImageIcon(sourceImage));
		getContentPane().add(sourceImageLabel, BorderLayout.WEST);
		

		QRCodeDecoder decoder = new QRCodeDecoder();
		if (canvas != null) {
			getContentPane().remove(canvas);
			//canvas.setImage(null);
		}
		canvas = new J2SEDebugCanvas();
		QRCodeDecoder.setCanvas(canvas);
		getContentPane().add(canvas, BorderLayout.EAST);
		String decodedString = null;
		try {
      byte[] decodedBytes = decoder.decode(new J2SEImage(sourceImage));
      decodedString = new String(decodedBytes);
    } catch (Exception e) {
     e.printStackTrace();
    } 
		decodedString = ContentConverter.convert(decodedString);
		canvas.println("\nDecode result:");
		canvas.println(decodedString);
		canvas.println("--------");
		if (decodedText != null)
			getContentPane().remove(decodedText);
		decodedText = new JTextArea(decodedString);
		decodedText.setLineWrap(true);
		decodedText.setRows(decodedString.length() / 20 + 1);
		if (decodedString.length() < 20)
			decodedText.setColumns(decodedString.length());
		else
			decodedText.setColumns(20);
		//decodedText.setSize(sourceImageLabel.getSize().width,100);
		getContentPane().add(decodedText, BorderLayout.SOUTH);
		pack();
  }

	public void actionPerformed(ActionEvent event) {
		if (event.getSource().equals(openMenu)) {
			chooser.showOpenDialog(this);
			if (chooser.getSelectedFile() == null)
				return ;
			try {
				sourceImage = ImageIO.read(chooser.getSelectedFile());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (event.getActionCommand().equals("Open from URL")) {
			try {
			sourceImage = ImageIO.read(new URL(url.getText()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else
			return;
		

		if (sourceImageLabel != null)
			getContentPane().remove(sourceImageLabel);
		
		sourceImageLabel = new JLabel(new ImageIcon(sourceImage));
		getContentPane().add(sourceImageLabel, BorderLayout.WEST);
		

		QRCodeDecoder decoder = new QRCodeDecoder();
		if (canvas != null) {
			getContentPane().remove(canvas);
			//canvas.setImage(null);
		}
		canvas = new J2SEDebugCanvas();
		QRCodeDecoder.setCanvas(canvas);
		getContentPane().add(canvas, BorderLayout.EAST);
		String decodedString = null;
		try {
			decodedString = new String(decoder.decode(new J2SEImage(sourceImage)));
		} catch (DecodingFailedException e) {
			canvas.println(e.getMessage());
			canvas.println("--------");
			return;
		}
		decodedString = ContentConverter.convert(decodedString);
		canvas.println("\nDecode result:");
		canvas.println(decodedString);
		canvas.println("--------");
		if (decodedText != null)
			getContentPane().remove(decodedText);
		decodedText = new JTextArea(decodedString);
		decodedText.setLineWrap(true);
		decodedText.setRows(decodedString.length() / 20 + 1);
		if (decodedString.length() < 20)
			decodedText.setColumns(decodedString.length());
		else
			decodedText.setColumns(20);
		//decodedText.setSize(sourceImageLabel.getSize().width,100);
		getContentPane().add(decodedText, BorderLayout.SOUTH);
		pack();
	}
	public static void main(String[] args) {
		new QRCodeDecoderGUIExample(args);
	}

}

class ImageFileFilter extends javax.swing.filechooser.FileFilter {
	String[] acceptExtendsions = {"jpg","jpeg","gif","png"};

	public String getDescription() {
		return "QR Code Image files (*.jpg,*.png,*.gif,*.png)";
	}
	public boolean accept(File f) {
		if (f.isDirectory()) 
			return true;
		
		String extension = getExtension(f);
		if (extension == null)
			return true;
		for (int i = 0; i < acceptExtendsions.length; i++) {
			if (extension.equals(acceptExtendsions[i]))
				return true;
		}
		return false;
			
	}
		
  String getExtension(File f) {
    String ext = null;
    String s = f.getName();
    int i = s.lastIndexOf('.');

    if (i > 0 &&  i < s.length() - 1) {
        ext = s.substring(i+1).toLowerCase();
    }
    return ext;
  }
}


class J2SEDebugCanvas extends Canvas implements DebugCanvas {
	BufferedImage image;

	public void paint(Graphics g){
		if (image != null)
			g.drawImage(image, 0, 0, java.awt.Color.WHITE, null);
	}
	
	public  void println(String string){
		System.out.println(string);
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
