package jp.sourceforge.qrcode.pattern;


import jp.sourceforge.qrcode.QRCodeDecoder;
import jp.sourceforge.qrcode.reader.*;
import jp.sourceforge.qrcode.exception.FinderPatternNotFoundException;
import jp.sourceforge.qrcode.exception.InvalidVersionInfoException;
import jp.sourceforge.qrcode.exception.InvalidVersionException;
import jp.sourceforge.qrcode.exception.VersionInformationException;
import jp.sourceforge.qrcode.geom.*;

import java.util.*;
import jp.sourceforge.qrcode.util.*;

public class FinderPattern {
	public static final int UL = 0;
	public static final int UR = 1;
	public static final int DL = 2;
	
	// this constant used for VersionInformation's error correction (BCC
	static final int[] VersionInfoBit = {
			0x07C94,0x085BC,0x09A99,0x0A4D3,0x0BBF6,0x0C762,0x0D847,
			0x0E60D,0x0F928,0x10B78,0x1145D,0x12A17,0x13532,0x149A6,
			0x15683,0x168C9,0x177EC,0x18EC4,0x191E1,0x1AFAB,0x1B08E,
			0x1CC1A,0x1D33F,0x1ED75,0x1F250,0x209D5,0x216F0,0x228BA,
			0x2379F,0x24B0B,0x2542E,0x26A64,0x27541,0x28C69
	};
	
	static DebugCanvas canvas = QRCodeDecoder.getCanvas();
	Point[] center;
	int version;
	int[] sincos;
	int[] width;
	int[] moduleSize;
	
	public static FinderPattern findFinderPattern(boolean[][] image)
			throws FinderPatternNotFoundException,
			VersionInformationException {
		Line[] lineAcross = findLineAcross(image);
		Line[] lineCross = findLineCross(lineAcross);
		Point[] center = null;
		try {
			center = getCenter(lineCross);
		} catch (FinderPatternNotFoundException e) {
			throw e;
		}
		int[] sincos = getAngle(center);
		center = sort(center, sincos);
		int[] width = getWidth(image, center, sincos);
		// moduleSize for version recognition
		int[] moduleSize = {(width[UL] << QRCodeImageReader.DECIMAL_POINT) / 7,
							(width[UR] << QRCodeImageReader.DECIMAL_POINT) / 7,
							(width[DL] << QRCodeImageReader.DECIMAL_POINT) / 7};
		int version = calcRoughVersion(center, width);
		if (version > 6) {
			try {
				version = calcExactVersion(center, sincos, moduleSize, image);
			} catch (VersionInformationException e) {
				//use rough version data
				// throw e;
				
			}
		}
		return new FinderPattern (center, version, sincos, width, moduleSize);
	}
	
	FinderPattern (Point[] center, int version, int[] sincos, int[] width, int[] moduleSize) {
		this.center = center;
		this.version = version;
		this.sincos = sincos;
		this.width = width;
		this.moduleSize = moduleSize;
	}
	
	public Point[] getCenter() {
		return center;
	}
	
	public Point getCenter(int position) {
		if (position >= UL && position <= DL)
			return center[position];	
		else
			return null;
	}
	
	public int getWidth(int position) {
		return width[position];
	}
	
	public int[] getAngle() {
		return sincos;
	}
	
	public int getVersion() {
		return version;
	}
	
	public int getModuleSize() {
		return moduleSize[UL];
	}
	public int getModuleSize(int place) {
		return moduleSize[place];
	}
	public int getSqrtNumModules() {
		return 17 + 4 * version;
	}
	
	/*
	 * At first, to detect Finder Pattern, liner pattern (D:L:D:L:D)=(1:1:3:1:1) 
	 * (D:dark point L:Light point) are extracted as line (both vertical and horizontal).
	 * INFO: Although this method detects lines does not across Finder Patterns too, 
	 *       these are ignored safely in after process (FinderPattern.findLineCross())
	 */
	static Line[] findLineAcross(boolean[][] image) {
		final int READ_HORIZONTAL = 0;
		final int READ_VERTICAL = 1;

		int imageWidth = image.length;
		int imageHeight = image[0].length;

		//int currentX = 0, currentY = 0;
		Point current = new Point();
		Vector lineAcross = new Vector();
		
		//buffer contains recent length of modules which has same brightness
		int[] lengthBuffer = new int[5];
		int  bufferPointer = 0;
		
		int direction = READ_HORIZONTAL; //start to read horizontally
		boolean lastElement = QRCodeImageReader.POINT_LIGHT;
	
		while(true) {
			//check points in image
			boolean currentElement = image[current.getX()][current.getY()];
			if (currentElement == lastElement) { //target point has same brightness with last point
				lengthBuffer[bufferPointer]++;
			}
			else { //target point has different brightness with last point
				if (currentElement == QRCodeImageReader.POINT_LIGHT) {
					if (checkPattern(lengthBuffer, bufferPointer)) { //detected pattern
						int x1, y1, x2, y2;
						if (direction == READ_HORIZONTAL) {
							//obtain X coordinates of both side of the detected horizontal pattern
							x1 = current.getX(); 
							for (int j = 0; j < 5; j++) {
								x1 -= lengthBuffer[j];
							}
							x2 = current.getX() - 1; //right side is last X coordinate
							y1 = y2 = current.getY();
						}
						else {
							x1 = x2 = current.getX();
							//obtain Y coordinates of both side of the detected vertical pattern
							// upper side is sum of length of buffer
							y1 = current.getY(); 
							for (int j = 0; j < 5; j++) {
								y1 -= lengthBuffer[j];
							}
							y2 = current.getY() - 1; // bottom side is last Y coordinate
						}
						lineAcross.addElement(new Line(x1, y1, x2, y2));
					}
				}
				bufferPointer = (bufferPointer + 1) % 5; 
				lengthBuffer[bufferPointer] = 1;
				lastElement = !lastElement;
			}
			
			// determine if read next, change read direction or terminate this loop
			if (direction == READ_HORIZONTAL) {
				if (current.getX() < imageWidth - 1) {
					current.translate(1, 0);
				}
				else if (current.getY() < imageHeight - 1) {
					current.set(0, current.getY() + 1);
					lengthBuffer =  new int[5];
				}
				else {
					current.set(0, 0); //reset target point
					lengthBuffer =  new int[5];
					direction = READ_VERTICAL; //start to read vertically
				}
			}
			else { //reading vertically
				if (current.getY() < imageHeight - 1)
					current.translate(0, 1);
				else if (current.getX() < imageWidth - 1) {
					current.set(current.getX() + 1, 0);
					lengthBuffer = new int[5];
				}
				else {
					break;
				}
			}
		}
		
		Line[] foundLines = new Line[lineAcross.size()];

		for (int i = 0; i < foundLines.length; i++)
			foundLines[i] = (Line) lineAcross.elementAt(i);
		
		canvas.drawLines(foundLines,Color.LIGHTGREEN);
		return foundLines;
	}
	
	static boolean checkPattern(int[] buffer, int pointer) {
		final int[] modelRatio = {1, 1, 3, 1, 1};	

		int baselength = 0;
		for (int i = 0; i < 5; i++) {
			baselength += buffer[i];
		}
		// pseudo fixed point calculation. I think it needs smarter code
		baselength <<= QRCodeImageReader.DECIMAL_POINT; 
		baselength /= 7;
		for  (int i = 0; i < 5; i++) {
			int leastlength = baselength * modelRatio[i] - baselength / 2;
			int mostlength = baselength * modelRatio[i] + baselength / 2;
			
			//TODO rough finder pattern detection
			
			int targetlength = buffer[(pointer + i + 1) % 5] << QRCodeImageReader.DECIMAL_POINT;
			if (targetlength < leastlength || targetlength > mostlength) {
				return false;
			}
		}
		return true;
	}

	
	//obtain lines cross at the center of Finder Patterns
	
	static Line[] findLineCross(Line[] lineAcross) {
		Vector crossLines = new Vector();
		Vector lineNeighbor = new Vector();
		Vector lineCandidate = new Vector();
		Line compareLine;
		for (int i = 0; i < lineAcross.length; i++)
			lineCandidate.addElement(lineAcross[i]);
		
		for (int i = 0; i < lineCandidate.size() - 1; i++) {
			lineNeighbor.removeAllElements();
			lineNeighbor.addElement(lineCandidate.elementAt(i));
			for (int j = i + 1; j < lineCandidate.size(); j++) {
				if (Line.isNeighbor((Line)lineNeighbor.lastElement(), (Line)lineCandidate.elementAt(j))) {
					lineNeighbor.addElement(lineCandidate.elementAt(j));
					compareLine = (Line)lineNeighbor.lastElement();
					if (lineNeighbor.size() * 5 > compareLine.getLength() &&
							j == lineCandidate.size() - 1) {
						crossLines.addElement(lineNeighbor.elementAt(lineNeighbor.size() / 2));
						for (int k = 0; k < lineNeighbor.size(); k++)
							lineCandidate.removeElement(lineNeighbor.elementAt(k));
					}
				}
				//terminate comparison if there are no possibility for found neighbour lines
				else if (cantNeighbor((Line)lineNeighbor.lastElement(), (Line)lineCandidate.elementAt(j)) ||
					(j == lineCandidate.size() - 1)) {
					compareLine = (Line)lineNeighbor.lastElement();
					/*
					 * determine lines across Finder Patterns when number of neighbour lines are 
					 * bigger than 1/6 length of theirselves
					 */					
					if (lineNeighbor.size() * 6 > compareLine.getLength()) {
						crossLines.addElement(lineNeighbor.elementAt(lineNeighbor.size() / 2));
						for (int k = 0; k < lineNeighbor.size(); k++) {
							lineCandidate.removeElement(lineNeighbor.elementAt(k));
						}
					}
					break;
				}
			}
		}	

		Line[] foundLines = new Line[crossLines.size()];
		for (int i = 0; i < foundLines.length; i++) {
			foundLines[i] = (Line) crossLines.elementAt(i);
		}
		return foundLines;
	}
	
	static boolean cantNeighbor(Line line1, Line line2) {
		if (Line.isCross(line1, line2))
			return true;
		return line1.isHorizontal()? Math.abs(line1.getP1().getY() - line2.getP1().getY()) > 1 : Math.abs(line1.getP1().getX() - line2.getP1().getX()) > 1;
	}
	
	//obtain slope of symbol
	static int[] getAngle(Point[] centers) {

		Line[] additionalLine = new Line[3];

		for (int i = 0; i < additionalLine.length; i++) {
			additionalLine[i] = new Line(centers[i],
					centers[(i + 1) % additionalLine.length]);
		}
		// remoteLine - does not contain UL center
		Line remoteLine = Line.getLongest(additionalLine);
		Point originPoint = new Point();
		for (int i = 0; i < centers.length; i++) {
			if (!remoteLine.getP1().equals(centers[i]) &&
				 !remoteLine.getP2().equals(centers[i])) {
				originPoint = centers[i];
				break;
			}
		}
		canvas.println("originPoint is: " + originPoint);
		Point remotePoint = new Point();

		//with origin that the center of Left-Up Finder Pattern, determine other two patterns center.
		//then calculate symbols angle
		if (originPoint.getY() <= remoteLine.getP1().getY() & //1st or 2nd quadrant
				originPoint.getY() <= remoteLine.getP2().getY())
			if (remoteLine.getP1().getX() < remoteLine.getP2().getX())
				remotePoint = remoteLine.getP2();
			else
				remotePoint = remoteLine.getP1();
		else if (originPoint.getX() >= remoteLine.getP1().getX() & //2nd or 3rd quadrant
				originPoint.getX() >= remoteLine.getP2().getX())
			if (remoteLine.getP1().getY() < remoteLine.getP2().getY())
				remotePoint = remoteLine.getP2();
			else
				remotePoint = remoteLine.getP1();
		else if (originPoint.getY() >= remoteLine.getP1().getY() & //3rd or 4th quadrant
				originPoint.getY() >= remoteLine.getP2().getY())
			if (remoteLine.getP1().getX() < remoteLine.getP2().getX())
				remotePoint = remoteLine.getP1();
			else
				remotePoint = remoteLine.getP2();
		else //1st or 4th quadrant
			if (remoteLine.getP1().getY() < remoteLine.getP2().getY())
				remotePoint = remoteLine.getP1();
			else
				remotePoint = remoteLine.getP2();
		
		int r = new Line(originPoint, remotePoint).getLength();
		//canvas.println(Integer.toString(((remotePoint.getX() - originPoint.getX()) << QRCodeImageReader.DECIMAL_POINT)));
		int angle[] = new int[2];
		angle[0] = ((remotePoint.getY() - originPoint.getY()) << QRCodeImageReader.DECIMAL_POINT) / r; //Sin
		angle[1] = ((remotePoint.getX() - originPoint.getX()) << (QRCodeImageReader.DECIMAL_POINT)) / r; //Cos
		
		return angle;
	}
	
	static Point[] getCenter(Line[] crossLines) 
			throws FinderPatternNotFoundException {
		Vector centers = new Vector();
		for (int i = 0; i < crossLines.length - 1; i++) {
			Line compareLine = crossLines[i];
			for (int j = i + 1; j < crossLines.length; j++) {
				Line comparedLine = crossLines[j];
				if (Line.isCross(compareLine, comparedLine)) {
					int x = 0;
					int y = 0;
					if (compareLine.isHorizontal()) {
						x = compareLine.getCenter().getX();
						y = comparedLine.getCenter().getY();		
					}
					else {
						x = comparedLine.getCenter().getX();
						y = compareLine.getCenter().getY();
					}
					centers.addElement(new Point(x,y));
				}
			}
		}
		
		Point[] foundPoints = new Point[centers.size()];
		
		for (int i = 0; i < foundPoints.length; i++) {
			foundPoints[i] = (Point)centers.elementAt(i);
			//System.out.println(foundPoints[i]);
		}
		//System.out.println(foundPoints.length);
		
		if (foundPoints.length == 3) {
			canvas.drawPolygon(foundPoints, Color.RED);	
			return foundPoints;
		}
		else
			throw new FinderPatternNotFoundException("Invalid number of Finder Pattern detected");
	}

	//sort center of finder patterns as Left-Up: points[0], Right-Up: points[1], Left-Down: points[2].
	static Point[] sort(Point[] centers, int[] angle) {

		Point[] sortedCenters = new Point[3];
		
		int quadrant = getURQuadrant(angle);
		switch (quadrant) {
		case 1:
			sortedCenters[1] = getPointAtSide(centers, Point.RIGHT, Point.BOTTOM);
			sortedCenters[2] = getPointAtSide(centers, Point.BOTTOM, Point.LEFT);
			break;
		case 2:
			sortedCenters[1] = getPointAtSide(centers, Point.BOTTOM, Point.LEFT);
			sortedCenters[2] = getPointAtSide(centers, Point.TOP, Point.LEFT);
			break;
		case 3:
			sortedCenters[1] = getPointAtSide(centers, Point.LEFT, Point.TOP);			
			sortedCenters[2] = getPointAtSide(centers, Point.RIGHT, Point.TOP);
			break;
		case 4:
			sortedCenters[1] = getPointAtSide(centers, Point.TOP, Point.RIGHT);
			sortedCenters[2] = getPointAtSide(centers, Point.BOTTOM, Point.RIGHT);
			break;
		}

		//last of centers is Left-Up patterns one
		for (int i = 0; i < centers.length; i++) {
			if (!centers[i].equals(sortedCenters[1]) && 
					!centers[i].equals(sortedCenters[2])) {
				sortedCenters[0] = centers[i];
			}
		}

		return sortedCenters;
	}
	
	static int getURQuadrant(int[] angle) {
		int sin = angle[0];
		int cos = angle[1];
		if (sin >= 0 && cos > 0)
			return 1;
		else if (sin > 0 && cos <= 0)
			return 2;
		else if (sin <= 0 && cos < 0)
			return 3;
		else if (sin < 0 && cos >= 0)
			return 4;
		
		return 0;
	}
	
	static Point getPointAtSide(Point[] points, int side1, int side2) {
		Point sidePoint = new Point();
		int x = ((side1 == Point.RIGHT || side2 == Point.RIGHT) ? 0 : Integer.MAX_VALUE);
		int y = ((side1 == Point.BOTTOM || side2 == Point.BOTTOM) ? 0 : Integer.MAX_VALUE);
		sidePoint = new Point(x, y);
			
		for (int i = 0; i < points.length; i++) {
			switch (side1) {
			case Point.RIGHT:
				if (sidePoint.getX() < points[i].getX()) {
					sidePoint = points[i];
				}
				else if (sidePoint.getX() == points[i].getX()) {
					if (side2 == Point.BOTTOM) {
						if (sidePoint.getY() < points[i].getY()) {
							sidePoint = points[i];
						}
					}
					else {
						if (sidePoint.getY() > points[i].getY()) {
							sidePoint = points[i];
						}
					}
				}
				break;
			case Point.BOTTOM:
				if (sidePoint.getY() < points[i].getY()) {
					sidePoint = points[i];
				}
				else if (sidePoint.getY() == points[i].getY()) {
					if (side2 == Point.RIGHT) {
						if (sidePoint.getX() < points[i].getX()) {
							sidePoint = points[i];
						}
					}
					else {
						if (sidePoint.getX() > points[i].getX()) {
							sidePoint = points[i];
						}
					}
				}
				break;
			case Point.LEFT:
				if (sidePoint.getX() > points[i].getX()) {
					sidePoint = points[i];
				}
				else if (sidePoint.getX() == points[i].getX()) {
					if (side2 == Point.BOTTOM) {
						if (sidePoint.getY() < points[i].getY()) {
							sidePoint = points[i];
						}
					}
					else {
						if (sidePoint.getY() > points[i].getY()) {
							sidePoint = points[i];
						}
					}
				}
				break;
			case Point.TOP:
				if (sidePoint.getY() > points[i].getY()) {
					sidePoint = points[i];
				}
				else if (sidePoint.getY() == points[i].getY()) {
					if (side2 == Point.RIGHT) {
						if (sidePoint.getX() < points[i].getX()) {
							sidePoint = points[i];
						}
					}
					else {
						if (sidePoint.getX() > points[i].getX()) {
							sidePoint = points[i];
						}
					}
				}
				break;
			}
		}
		return sidePoint;
	}
	
	static int[] getWidth(boolean[][] image, Point[] centers,  int[] sincos) 
		throws ArrayIndexOutOfBoundsException {

		int[] width = new int[3];
		
		for (int i = 0; i < 3; i++) {
			boolean flag = false;
			int lx, rx;
			int y = centers[i].getY();
			for (lx = centers[i].getX(); lx >= 0; lx--) {
				if (image[lx][y] == QRCodeImageReader.POINT_DARK &&
						image [lx - 1][y] == QRCodeImageReader.POINT_LIGHT) {
					if (flag == false) flag = true;
					else break;
				}
			}
			flag = false;
			for (rx = centers[i].getX(); rx < image.length; rx++) {
				if (image[rx][y] == QRCodeImageReader.POINT_DARK &&
						image[rx + 1][y] == QRCodeImageReader.POINT_LIGHT)  {
					if (flag == false) flag = true;
					else break;
				}
			}
			width[i] = (rx - lx + 1);
		}
		return width;
	}
	
	static int calcRoughVersion(Point[] center, int[] width) {
		final int dp = QRCodeImageReader.DECIMAL_POINT;
		int lengthAdditionalLine = (new Line(center[UL], center[UR]).getLength()) << dp ;
		int avarageWidth = ((width[UL] + width[UR]) << dp) / 14;
		int roughVersion = ((lengthAdditionalLine / avarageWidth) - 10) / 4;
		if (((lengthAdditionalLine / avarageWidth) - 10) % 4 >= 2) {
			roughVersion++;
		}

		return roughVersion;

		
	}
	
	static int calcExactVersion(Point[] centers, int[] angle, int[] moduleSize, boolean[][] image) 
	throws InvalidVersionInfoException, InvalidVersionException {
		boolean[] versionInformation = new boolean[18];
		Point[] points = new Point[18];
		Point target;
		Axis axis = new Axis(angle, moduleSize[UR]); //UR
		axis.setOrigin(centers[UR]);

		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 3; x++) {
				target = axis.translate(x - 7, y - 3);
				versionInformation[x + y * 3] = image[target.getX()][target.getY()];
				points[x + y * 3] = target;
			}
		}
		canvas.drawPoints(points, Color.RED);
		
		int exactVersion = 0;
		try {
			exactVersion = checkVersionInfo(versionInformation);
		} catch (InvalidVersionInfoException e) {
			canvas.println("Version info error. now retry with other place one.");
			axis.setOrigin(centers[DL]);
			axis.setModulePitch(moduleSize[DL]); //DL
			
			for (int x = 0; x < 6; x++) {
				for (int y = 0; y < 3; y++) {
					target = axis.translate(x - 3, y - 7);
					versionInformation[y + x * 3] = image[target.getX()][target.getY()];
					points[x + y * 3] = target;
				}
			}
			canvas.drawPoints(points, Color.RED);
			
			try {
				exactVersion = checkVersionInfo(versionInformation);
			} catch (VersionInformationException e2) {
				throw e2;
			}
		}
		return exactVersion;
	}
	
	static int checkVersionInfo(boolean[] target)
			throws InvalidVersionInfoException{
		// note that this method includes BCH 18-6 Error Correction
		// see page 67 on JIS-X-0510(2004) 
		int errorCount = 0, versionBase;
		for (versionBase = 0; versionBase < VersionInfoBit.length; versionBase++) {
			errorCount = 0;
			for (int j = 0; j < 18; j++) {
				if (target[j] ^ (VersionInfoBit[versionBase] >> j) % 2 == 1)
					errorCount++;
			}
			if (errorCount <= 3) break;
		}
		if (errorCount <= 3)
			return 7 + versionBase;
		else
			throw new InvalidVersionInfoException("Too many errors in version information");
	}
}
