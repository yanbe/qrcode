package jp.sourceforge.qrcode.reader;


import java.util.Vector;

import jp.sourceforge.qrcode.QRCodeDecoder;
import jp.sourceforge.qrcode.data.*;
import jp.sourceforge.qrcode.exception.AlignmentPatternNotFoundException;
import jp.sourceforge.qrcode.exception.FinderPatternNotFoundException;
import jp.sourceforge.qrcode.exception.SymbolNotFoundException;
import jp.sourceforge.qrcode.exception.InvalidVersionException;
import jp.sourceforge.qrcode.exception.VersionInformationException;
import jp.sourceforge.qrcode.geom.*;
import jp.sourceforge.qrcode.pattern.*;
import jp.sourceforge.qrcode.util.*;

public class QRCodeImageReader {
	DebugCanvas canvas;
	//DP = 
	//23 ...side pixels of image will be limited maximum 255 (8 bits)
	//22 .. side pixels of image will be limited maximum 511 (9 bits)
	//21 .. side pixels of image will be limited maximum 1023 (10 bits)
	
	//I think it's good idea to use DECIMAL_POINT with type "long" too.
	
	public static int DECIMAL_POINT = 21;
	public static final boolean POINT_DARK = true;
	public static final boolean POINT_LIGHT = false;
	SamplingGrid samplingGrid;
	boolean[][] bitmap;
	//int numModuleAtSide; //デコード対象のシンボルにおける一辺のモジュールの数

	
	public QRCodeImageReader() {
		this.canvas = QRCodeDecoder.getCanvas();
	}
	// local class for module pitch
	protected class ModulePitch
	{
		public int top;
		public int left;
		public int bottom;
		public int right;
	};


	boolean[][] applyMedianFilter(boolean[][] image, int threshold) {
		boolean[][] filteredMatrix = new boolean[image.length][image[0].length];
		//filtering noise in image with median filter
		int numPointDark;
		for (int y = 1; y < image[0].length - 1; y++) {
			for (int x = 1; x < image.length - 1; x++) {
			//if (image[x][y] == true) {
			numPointDark = 0;
			for (int fy = -1; fy < 2; fy++) {
				for (int fx = -1; fx < 2; fx++) {
					if (image[x + fx][y + fy] == true) {
						numPointDark++;
					}
				}
			}
			if (numPointDark > threshold) 
				filteredMatrix[x][y] = POINT_DARK;
			}
		}
		
		return filteredMatrix;
	}
	boolean[][] applyCrossMaskingMedianFilter(boolean[][] image, int threshold) {
		boolean[][] filteredMatrix = new boolean[image.length][image[0].length];
		//filtering noise in image with median filter
		int numPointDark;
		for (int y = 2; y < image[0].length - 2; y++) {
			for (int x = 2; x < image.length - 2; x++) {
			//if (image[x][y] == true) {
			numPointDark = 0;
			for (int f = -2; f < 3; f++) {
				if (image[x+f][y] == true)
					numPointDark++;
				
				if (image[x][y+f] == true)
					numPointDark++;
			}
			
			if (numPointDark > threshold) 
				filteredMatrix[x][y] = POINT_DARK;
			}
		}
		
		return filteredMatrix;
	}	
	boolean[][] filterImage(int[][] image) {
		imageToGrayScale(image);
		boolean[][] bitmap = grayScaleToBitmap(image);
		return bitmap;
	}
	
	void imageToGrayScale(int[][] image) {
		for (int y = 0; y < image[0].length; y++) {
			for (int x = 0; x < image.length; x++) {
				int r = image[x][y] >> 16 & 0xFF;
				int g = image[x][y] >> 8 & 0xFF;
				int b = image[x][y] & 0xFF;
				int m = (r * 30 + g * 59 + b * 11) / 100;
				image[x][y] = m;
			}
		}
	}
	
	boolean[][] grayScaleToBitmap(int[][] grayScale) {
		int[][] middle = getMiddleBrightnessPerArea(grayScale);
		int sqrtNumArea = middle.length;
		int areaWidth = grayScale.length / sqrtNumArea;
		int areaHeight = grayScale[0].length / sqrtNumArea;
		boolean[][] bitmap = new boolean[grayScale.length][grayScale[0].length];

		for (int ay = 0; ay < sqrtNumArea; ay++) {
			for (int ax = 0; ax < sqrtNumArea; ax++) {
				for (int dy = 0; dy < areaHeight; dy++) {
					for (int dx = 0; dx < areaWidth; dx++) {
						bitmap[areaWidth * ax + dx][areaHeight * ay + dy] = (grayScale[areaWidth * ax + dx][areaHeight * ay + dy] < middle[ax][ay]) ? true : false;
					}
				}
			}
		}
		return bitmap;
	}
	
	int[][] getMiddleBrightnessPerArea(int[][] image) {
		final int numSqrtArea = 4;
		//obtain middle brightness((min + max) / 2) per area
		int areaWidth = image.length / numSqrtArea;
		int areaHeight = image[0].length / numSqrtArea;
		int[][][] minmax = new int[numSqrtArea][numSqrtArea][2];
		for (int ay = 0; ay < numSqrtArea; ay++) {
			for (int ax = 0; ax < numSqrtArea; ax++) {
				minmax[ax][ay][0] = 0xFF;
				for (int dy = 0; dy < areaHeight; dy++) {
					for (int dx = 0; dx < areaWidth; dx++) {
						int target = image[areaWidth * ax + dx][areaHeight * ay + dy];
						if (target < minmax[ax][ay][0]) minmax[ax][ay][0] = target;
						if (target > minmax[ax][ay][1]) minmax[ax][ay][1] = target;
					}
				}
				//minmax[ax][ay][0] = (minmax[ax][ay][0] + minmax[ax][ay][1]) / 2;
			}
		}
		int[][] middle =  new int[numSqrtArea][numSqrtArea];
		for (int ay = 0; ay < numSqrtArea; ay++) {
			for (int ax = 0; ax < numSqrtArea; ax++) {
				middle[ax][ay] = (minmax[ax][ay][0] + minmax[ax][ay][1]) / 2;
				//System.out.print(middle[ax][ay] + ",");
			}
			//System.out.println("");
		}
		//System.out.println("");

		return middle;
	}
	
	public QRCodeSymbol getQRCodeSymbol(int[][] image) 
			throws SymbolNotFoundException {
		int longSide = (image.length < image[0].length) ? image[0].length : image.length;
		QRCodeImageReader.DECIMAL_POINT = 23 - QRCodeUtility.sqrt(longSide / 256);
		bitmap = filterImage(image);			
		canvas.println("Drawing matrix.");
		canvas.drawMatrix(bitmap);

		canvas.println("Scanning Finder Pattern.");
		FinderPattern finderPattern = null;
		try {
			finderPattern = FinderPattern.findFinderPattern(bitmap);
		} catch (FinderPatternNotFoundException e) {
			canvas.println("Not found, now retrying...");
			bitmap = applyCrossMaskingMedianFilter(bitmap, 5);
			canvas.drawMatrix(bitmap);
			try {
				finderPattern = FinderPattern.findFinderPattern(bitmap);
			} catch (FinderPatternNotFoundException e2) {
				throw new SymbolNotFoundException(e2.getMessage());
			} catch (VersionInformationException e2) {
				throw new SymbolNotFoundException(e2.getMessage());
			}
		} catch (VersionInformationException e) {
			throw new SymbolNotFoundException(e.getMessage());
		}


		canvas.println("FinderPattern at");
		String finderPatternCoordinates = 
			finderPattern.getCenter(FinderPattern.UL).toString() + 
			finderPattern.getCenter(FinderPattern.UR).toString() +
			finderPattern.getCenter(FinderPattern.DL).toString();
		canvas.println(finderPatternCoordinates);
 		int[] sincos = finderPattern.getAngle();
		canvas.println("Angle*4098: Sin " + Integer.toString(sincos[0]) + "  " + "Cos " + Integer.toString(sincos[1]));

		int version = finderPattern.getVersion();
		canvas.println("Version: " + Integer.toString(version));
		if (version < 1 || version > 40)
			throw new InvalidVersionException("Invalid version: " + version);
		
		AlignmentPattern alignmentPattern = null;
		try {
			alignmentPattern = AlignmentPattern.findAlignmentPattern(bitmap, finderPattern);
		} catch (AlignmentPatternNotFoundException e) {
			throw new SymbolNotFoundException(e.getMessage());
		}
		
		int matrixLength = alignmentPattern.getCenter().length;
		canvas.println("AlignmentPatterns at");
		for (int y = 0; y < matrixLength; y++) {
			String alignmentPatternCoordinates = "";
			for (int x = 0; x < matrixLength; x++) {
				alignmentPatternCoordinates += alignmentPattern.getCenter()[x][y].toString();
			}
			canvas.println(alignmentPatternCoordinates);
		}
		//for(int i = 0; i < 500000; i++) System.out.println("");

		canvas.println("Creating sampling grid.");
		//[TODO] need all-purpose method
		//samplingGrid = getSamplingGrid2_6(finderPattern, alignmentPattern);
		samplingGrid = getSamplingGrid(finderPattern, alignmentPattern);
		canvas.println("Reading grid.");
		boolean[][] qRCodeMatrix = null;
		try {
			qRCodeMatrix = getQRCodeMatrix(bitmap, samplingGrid);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new SymbolNotFoundException("Sampling grid exceeded image boundary");
		}
		//canvas.drawMatrix(qRCodeMatrix);
		return new QRCodeSymbol(qRCodeMatrix);
	}
	
	public QRCodeSymbol getQRCodeSymbolWithAdjustedGrid(Point adjust)
		throws IllegalStateException, SymbolNotFoundException {
		if (bitmap == null || samplingGrid == null) {
			throw new IllegalStateException("This method must be called after QRCodeImageReader.getQRCodeSymbol() called");
		}
		samplingGrid.adjust(adjust);
		canvas.println("Sampling grid adjusted d("+adjust.getX()+","+adjust.getY()+")");

		boolean[][] qRCodeMatrix = null;
		try { 
			qRCodeMatrix = getQRCodeMatrix(bitmap, samplingGrid);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new SymbolNotFoundException("Sampling grid exceeded image boundary");			
		}
		return new QRCodeSymbol(qRCodeMatrix);
	}
	
	/**
	 * Generic Sampling grid method
	 */
	SamplingGrid getSamplingGrid(FinderPattern finderPattern, AlignmentPattern alignmentPattern) {

		Point centers[][] = alignmentPattern.getCenter();

		int version = finderPattern.getVersion();
		int sqrtCenters = (version / 7) + 2;
		
		centers[0][0] = finderPattern.getCenter(FinderPattern.UL);
		centers[sqrtCenters-1][0] = finderPattern.getCenter(FinderPattern.UR);
		centers[0][sqrtCenters-1] = finderPattern.getCenter(FinderPattern.DL);
		//int sqrtNumModules = finderPattern.getSqrtNumModules(); /// The number of modules per one side is obtained
		int sqrtNumArea = sqrtCenters-1;
		
		//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//--//
		SamplingGrid samplingGrid = new SamplingGrid(sqrtNumArea);

		Line baseLineX, baseLineY, gridLineX, gridLineY;

		///???
		//Point[] targetCenters;

		//int logicalDistance = alignmentPattern.getLogicalDistance();
		Axis axis = new Axis(finderPattern.getAngle(), finderPattern.getModuleSize());
		ModulePitch modulePitch;

		// for each area :
		for (int ay = 0; ay < sqrtNumArea; ay++) {
			for (int ax = 0; ax < sqrtNumArea; ax++) {
				modulePitch = new ModulePitch();  /// Housing to order
				baseLineX = new Line();
				baseLineY = new Line();
				axis.setModulePitch(finderPattern.getModuleSize());

				Point logicalCenters[][]= AlignmentPattern.getLogicalCenter(finderPattern);

				Point upperLeftPoint	=	centers[ax][ay];
				Point upperRightPoint	=	centers[ax+1][ay];
				Point lowerLeftPoint	=	centers[ax][ay+1];
				Point lowerRightPoint	=	centers[ax+1][ay+1];

				Point logicalUpperLeftPoint	=	logicalCenters[ax][ay];
				Point logicalUpperRightPoint	=	logicalCenters[ax+1][ay];
				Point logicalLowerLeftPoint	=	logicalCenters[ax][ay+1];
				Point logicalLowerRightPoint	=	logicalCenters[ax+1][ay+1];

				if(ax==0 && ay==0) // left upper corner
				{
					if (sqrtNumArea == 1) {
						upperLeftPoint = axis.translate(upperLeftPoint,-3,-3);
						upperRightPoint = axis.translate(upperRightPoint,3,-3);
						lowerLeftPoint = axis.translate(lowerLeftPoint,-3,3);
						lowerRightPoint = axis.translate(lowerRightPoint,6,6);
	
						logicalUpperLeftPoint.translate(-6,-6);
						logicalUpperRightPoint.translate(3,-3);
						logicalLowerLeftPoint.translate(-3,3);						
						logicalLowerRightPoint.translate(6,6);						
					}
					else {
						upperLeftPoint = axis.translate(upperLeftPoint,-3,-3);
						upperRightPoint = axis.translate(upperRightPoint,0,-6);
						lowerLeftPoint = axis.translate(lowerLeftPoint,-6,0);
	
						logicalUpperLeftPoint.translate(-6,-6);
						logicalUpperRightPoint.translate(0,-6);
						logicalLowerLeftPoint.translate(-6,0);
					}
				}
				else if(ax==0 && ay==sqrtNumArea-1) // left bottom corner
				{
					upperLeftPoint = axis.translate(upperLeftPoint,-6,0);
					lowerLeftPoint = axis.translate(lowerLeftPoint,-3,3);
					lowerRightPoint = axis.translate(lowerRightPoint, 0, 6);


					logicalUpperLeftPoint.translate(-6,0);
					logicalLowerLeftPoint.translate(-6,6);
					logicalLowerRightPoint.translate(0,6);
				}
				else if(ax==sqrtNumArea-1 && ay==0) // right upper corner
				{
					upperLeftPoint = axis.translate(upperLeftPoint,0,-6);
					upperRightPoint = axis.translate(upperRightPoint,3,-3);
					lowerRightPoint = axis.translate(lowerRightPoint,6,0);

					logicalUpperLeftPoint.translate(0,-6);
					logicalUpperRightPoint.translate(6,-6);
					logicalLowerRightPoint.translate(6,0);
				}
				else if(ax==sqrtNumArea-1 && ay==sqrtNumArea-1) // right bottom corner
				{
					lowerLeftPoint = axis.translate(lowerLeftPoint,0,6);
					upperRightPoint = axis.translate(upperRightPoint,6,0);
					lowerRightPoint = axis.translate(lowerRightPoint,6,6);

					logicalLowerLeftPoint.translate(0,6);
					logicalUpperRightPoint.translate(6,0);
					logicalLowerRightPoint.translate(6,6);
				}
				else if(ax==0) // left side
				{
					upperLeftPoint = axis.translate(upperLeftPoint,-6,0);
					lowerLeftPoint = axis.translate(lowerLeftPoint,-6,0);

					logicalUpperLeftPoint.translate(-6,0);
					logicalLowerLeftPoint.translate(-6,0);

				}
				else if(ax==sqrtNumArea-1) // right
				{
					upperRightPoint = axis.translate(upperRightPoint,6,0);
					lowerRightPoint = axis.translate(lowerRightPoint,6,0);

					logicalUpperRightPoint.translate(6,0);
					logicalLowerRightPoint.translate(6,0);
				}
				else if(ay==0) // top
				{
					upperLeftPoint = axis.translate(upperLeftPoint,0,-6);
					upperRightPoint = axis.translate(upperRightPoint,0,-6);

					logicalUpperLeftPoint.translate(0,-6);
					logicalUpperRightPoint.translate(0,-6);

				}
				else if(ay==sqrtNumArea-1) // bottom
				{
					lowerLeftPoint = axis.translate(lowerLeftPoint,0,6);
					lowerRightPoint = axis.translate(lowerRightPoint,0,6);

					logicalLowerLeftPoint.translate(0,6);
					logicalLowerRightPoint.translate(0,6);
				}
				
				if(ax==0)
				{
					logicalUpperRightPoint.translate(1,0);
					logicalLowerRightPoint.translate(1,0);
				}
				else
				{
					logicalUpperLeftPoint.translate(-1,0);
					logicalLowerLeftPoint.translate(-1,0);
				}

				if(ay==0)
				{
					logicalLowerLeftPoint.translate(0,1);
					logicalLowerRightPoint.translate(0,1);
				}
				else
				{
					logicalUpperLeftPoint.translate(0,-1);
					logicalUpperRightPoint.translate(0,-1);
				}
				
				int logicalWidth=logicalUpperRightPoint.getX()-logicalUpperLeftPoint.getX();
				int logicalHeight=logicalLowerLeftPoint.getY()-logicalUpperLeftPoint.getY();

				if (version < 7) {
					logicalWidth += 3;
					logicalHeight += 3;
					
				}
				modulePitch.top = getAreaModulePitch(upperLeftPoint, upperRightPoint, logicalWidth-1);
				modulePitch.left = getAreaModulePitch(upperLeftPoint, lowerLeftPoint, logicalHeight-1);
				modulePitch.bottom = getAreaModulePitch(lowerLeftPoint, lowerRightPoint, logicalWidth-1);
				modulePitch.right = getAreaModulePitch(upperRightPoint, lowerRightPoint, logicalHeight-1);

				baseLineX.setP1(upperLeftPoint);
				baseLineY.setP1(upperLeftPoint);
				baseLineX.setP2(lowerLeftPoint);
				baseLineY.setP2(upperRightPoint);

				samplingGrid.initGrid(ax,ay,logicalWidth,logicalHeight);

				for (int i = 0; i < logicalWidth; i++) {
					gridLineX = new Line(baseLineX.getP1(), baseLineX.getP2());

					axis.setOrigin(gridLineX.getP1());
					axis.setModulePitch(modulePitch.top);
					gridLineX.setP1(axis.translate(i,0));

					axis.setOrigin(gridLineX.getP2());
					axis.setModulePitch(modulePitch.bottom);
					gridLineX.setP2(axis.translate(i,0));

					samplingGrid.setXLine(ax,ay,i,gridLineX);
				}

				for (int i = 0; i < logicalHeight; i++) {

					gridLineY = new Line(baseLineY.getP1(), baseLineY.getP2());

					axis.setOrigin(gridLineY.getP1());
					axis.setModulePitch(modulePitch.left);
					gridLineY.setP1(axis.translate(0,i));

					axis.setOrigin(gridLineY.getP2());
					axis.setModulePitch(modulePitch.right);
					gridLineY.setP2(axis.translate(0,i));

					samplingGrid.setYLine(ax,ay,i,gridLineY);

				}
			}
		}

		return samplingGrid;
	}


	
	//get module pitch in single area
	int getAreaModulePitch(Point start, Point end, int logicalDistance) {
		Line tempLine;
		tempLine = new Line(start, end);
		int realDistance = tempLine.getLength();
		int modulePitch = (realDistance << DECIMAL_POINT) / logicalDistance;
		return modulePitch;
	}

	
	//gridLines[areaX][areaY][direction(x=0,y=1)][EachLines]	
	boolean[][] getQRCodeMatrix(boolean[][] image, SamplingGrid gridLines) throws ArrayIndexOutOfBoundsException {
		//int gridSize = gridLines.getWidth() * gridLines.getWidth(0,0);
		int gridSize = gridLines.getTotalWidth();

// now this is done within the SamplingGrid class...
//		if (gridLines.getWidth() >= 2)
//			gridSize-=1;

		canvas.println("gridSize="+gridSize);
		//canvas.println("gridLines.getWidth() * gridLines.getWidth(0,0) = "+gridLines.getWidth() * gridLines.getWidth(0,0));
		Point bottomRightPoint = null;
		boolean[][] sampledMatrix = new boolean[gridSize][gridSize];
		for (int ay = 0; ay < gridLines.getHeight(); ay++) {
			for (int ax = 0; ax < gridLines.getWidth(); ax++) {
				Vector sampledPoints = new Vector(); //only for visualize;
				for (int y = 0; y < gridLines.getHeight(ax,ay); y++) {
					for (int x = 0; x < gridLines.getWidth(ax,ay); x++) {
						int x1 = gridLines.getXLine(ax,ay,x).getP1().getX();
						int y1 = gridLines.getXLine(ax,ay,x).getP1().getY();
						int x2 = gridLines.getXLine(ax,ay,x).getP2().getX();
						int y2 = gridLines.getXLine(ax,ay,x).getP2().getY();
						int x3 = gridLines.getYLine(ax,ay,y).getP1().getX();
						int y3 = gridLines.getYLine(ax,ay,y).getP1().getY();
						int x4 = gridLines.getYLine(ax,ay,y).getP2().getX();
						int y4 = gridLines.getYLine(ax,ay,y).getP2().getY();
						
						int e = (y2 - y1) * (x3 - x4) - (y4 - y3) * (x1 - x2);
						int f = (x1 * y2 - x2 * y1) * (x3 - x4) - (x3 * y4 - x4 * y3) * (x1 - x2);
						int g = (x3 * y4 - x4 * y3) * (y2 - y1) - (x1 * y2 - x2 * y1) * (y4 - y3);
						sampledMatrix[gridLines.getX(ax, x)][gridLines.getY(ay, y)] = image[f / e][g / e];
						if ((ay == gridLines.getHeight() -1 && ax == gridLines.getWidth() - 1) &&
								y == gridLines.getHeight(ax, ay) - 1 && x == gridLines.getWidth(ax, ay) -1)
							bottomRightPoint = new Point(f / e,g / e);
						//calling canvas.drawPoint in loop can be very slow.
						// use canvas.drawPoints if you need
						//canvas.drawPoint(new Point(f / e,g / e), Color.RED);
					}
				}
			}
		}
    if (bottomRightPoint != null && (bottomRightPoint.getX() > image.length - 1 || bottomRightPoint.getY() > image[0].length - 1))
			throw new ArrayIndexOutOfBoundsException("Sampling grid pointed out of image");
		canvas.drawPoint(bottomRightPoint, Color.BLUE);
			
		return sampledMatrix;
	}
}
