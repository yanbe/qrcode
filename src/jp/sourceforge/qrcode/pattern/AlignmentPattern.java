package jp.sourceforge.qrcode.pattern;


import jp.sourceforge.qrcode.QRCodeDecoder;
import jp.sourceforge.qrcode.reader.*;
import jp.sourceforge.qrcode.exception.AlignmentPatternNotFoundException;
import jp.sourceforge.qrcode.exception.InvalidVersionException;
import jp.sourceforge.qrcode.geom.*;
import jp.sourceforge.qrcode.util.*;

public class AlignmentPattern {
	static final int RIGHT = 1;
	static final int BOTTOM = 2;
	static final int LEFT = 3;
	static final int TOP = 4;

	static DebugCanvas canvas = QRCodeDecoder.getCanvas();
	Point[][] center;
	//int sqrtCenters;  ///The number per 1 sides of alignment pattern
	int patternDistance;
	
	AlignmentPattern(Point[][] center, int patternDistance) {
		this.center = center;
		this.patternDistance = patternDistance;
	}
	
	public static AlignmentPattern findAlignmentPattern(boolean[][] image, FinderPattern finderPattern) 
		throws AlignmentPatternNotFoundException, InvalidVersionException {

		Point[][] logicalCenters = getLogicalCenter(finderPattern);
		int logicalDistance = logicalCenters[1][0].getX() - logicalCenters[0][0].getX();
		//With it converts in order to handle in the same way
		Point[][] centers = null;
		centers = getCenter(image, finderPattern, logicalCenters);
		return new AlignmentPattern(centers, logicalDistance);

	}
	
	public Point[][] getCenter() {
		return center;
	}
	
	// for only trancparency access in version 1, which has no alignment pattern
	public void setCenter(Point[][] center) {
		this.center = center;
	}
	
	public int getLogicalDistance() {
		return patternDistance;
	}
	
	static Point[][] getCenter(boolean[][] image, FinderPattern finderPattern, Point[][] logicalCenters) 
			throws AlignmentPatternNotFoundException {
		int moduleSize = finderPattern.getModuleSize();

		Axis axis = new Axis(finderPattern.getAngle(), moduleSize);
		int sqrtCenters = logicalCenters.length;
		Point[][] centers = new Point[sqrtCenters][sqrtCenters];
		
		axis.setOrigin(finderPattern.getCenter(FinderPattern.UL));
		centers[0][0] = axis.translate(3, 3);
		canvas.drawCross(centers[0][0], Color.BLUE);

		axis.setOrigin(finderPattern.getCenter(FinderPattern.UR));
		centers[sqrtCenters - 1][0] = axis.translate(-3, 3);
		canvas.drawCross(centers[sqrtCenters - 1][0], Color.BLUE);

		axis.setOrigin(finderPattern.getCenter(FinderPattern.DL));
		centers[0][sqrtCenters - 1] = axis.translate(3, -3);
		canvas.drawCross(centers[0][sqrtCenters - 1], Color.BLUE);

		Point tmpPoint=centers[0][0];

		for (int y = 0; y < sqrtCenters; y++) {
			for (int x = 0; x < sqrtCenters; x++) {
				if ((x==0 && y==0) || (x==0 && y==sqrtCenters-1) || (x==sqrtCenters-1 && y==0)) {
//					canvas.drawCross(centers[x][y], java.awt.Color.MAGENTA);
					continue;
				}
				Point target = null;
				if (y == 0) {
					if (x > 0 && x < sqrtCenters - 1) {
						target = axis.translate(centers[x - 1][y], logicalCenters[x][y].getX() - logicalCenters[x - 1][y].getX(), 0);
            centers[x][y] = new Point(target.getX(), target.getY());
            canvas.drawCross(centers[x][y],Color.RED);
					}
				}
				else if (x == 0) {
					if (y > 0 && y < sqrtCenters - 1) {
						target = axis.translate(centers[x][y - 1], 0, logicalCenters[x][y].getY() - logicalCenters[x][y - 1].getY());
            centers[x][y] = new Point(target.getX(), target.getY());
            canvas.drawCross(centers[x][y], Color.RED);
					}
				}
				else {
					Point t1 = axis.translate(centers[x - 1][y], logicalCenters[x][y].getX() - logicalCenters[x - 1][y].getX(), 0);
					Point t2 = axis.translate(centers[x][y - 1], 0, logicalCenters[x][y].getY() - logicalCenters[x][y - 1].getY());
					centers[x][y] = new Point((t1.getX() + t2.getX()) / 2, (t1.getY() + t2.getY()) / 2 + 1);
				}
				if (finderPattern.getVersion() > 1) {
					Point precisionCenter = getPrecisionCenter(image, centers[x][y]);
	
					//if (centers[x][y].distanceOf(precisionCenter) < 6) {
						canvas.drawCross(centers[x][y],Color.RED);
						int dx = precisionCenter.getX() - centers[x][y].getX();
						int dy = precisionCenter.getY() - centers[x][y].getY();
						canvas.println("Adjust AP(" + x + ","+ y+") to d("+dx+","+dy+")");				
						
						centers[x][y] = precisionCenter;
					//}
				}
				canvas.drawCross(centers[x][y], Color.BLUE);
				canvas.drawLine(new Line(tmpPoint, centers[x][y]), Color.LIGHTBLUE);
				tmpPoint=centers[x][y];
				// Top row
/*				if (x >= 1 && x<(sqrtCenters-1) && y == 0 && sqrtCenters >= 3) {
					//center-top alignment pattern in version 7-13
					centers[x][y] = Point.getCenter(centers[0][0], centers[sqrtCenters - 1][0]);
				}
				// left column
				else if (x == 0 && y<(sqrtCenters-1) && y >= 1 && sqrtCenters >= 3) {
					//left-center alignment pattern in version 7-13
					centers[x][y] = Point.getCenter(centers[0][0], centers[0][sqrtCenters - 1]);					
				}
				// inside
				else if (x >= 1 && y >= 1){
					Line[] additionalLines = { 
							new Line(centers[x - 1][y - 1], centers[x][y - 1]),
							new Line(centers[x - 1][y - 1], centers[x - 1][y])};
					int dx = centers[x - 1][y].getX() - centers[x - 1][y - 1].getX();
					int dy = centers[x - 1][y].getY() - centers[x - 1][y - 1].getY();
					additionalLines[0].translate(dx,dy);
					dx = centers[x][y - 1].getX() - centers[x - 1][y - 1].getX();
					dy = centers[x][y - 1].getY() - centers[x - 1][y - 1].getY();
					additionalLines[1].translate(dx,dy);
					centers[x][y] = Point.getCenter(additionalLines[0].getP2(), additionalLines[1].getP2());
				}
*/
//				Sorry but I hate continue
//				else // dummy alignment pattern (source is finder pattern)
//					continue;

				/*canvas.drawLine(tmpPoint, centers[x][y], java.awt.Color.YELLOW);
				tmpPoint=centers[x][y];

				// if it is not one of the 3 big alignment patter,
				if(!(x==0 && y==0) && !(x==0 && y==sqrtCenters-1) && !(x==sqrtCenters-1 && y==0))
				{
					try {
						centers[x][y] = getPrecisionCenter(image, centers[x][y]);
						canvas.drawCross(getPrecisionCenter(image, centers[x][y]), java.awt.Color.YELLOW);
					} catch (AlignmentPatternEdgeNotFoundException e) {
						e.printStackTrace();
						throw e;
					}
					canvas.drawCross(centers[x][y], java.awt.Color.RED);
				}
				else
				{
					canvas.drawCross(centers[x][y], java.awt.Color.GREEN);
				}*/
			}
		}
		return centers;
	}

/*
		static Point[][] getCenter(boolean[][] image, FinderPattern finderPattern, Point[][] logicalCenters) 
			throws AlignmentPatternEdgeNotFoundException {
		int moduleSize = finderPattern.getModuleSize();
		int sin = finderPattern.getAngle()[0];
		int cos = finderPattern.getAngle()[1];

		Axis axis = new Axis(sin, cos, moduleSize);


		int sqrtCenters = logicalCenters.length;

		Point[][] centers = new Point[sqrtCenters][sqrtCenters];
		
		axis.setOrigin(finderPattern.getCenter(FinderPattern.UL));
		centers[0][0] = axis.translate(3, 3);
		//centers[0][0] = finderPattern.getCenter(FinderPattern.UL);
		axis.setOrigin(finderPattern.getCenter(FinderPattern.UR));
		centers[sqrtCenters - 1][0] = axis.translate(-3, 3);
		//centers[sqrtCenters - 1][0] = finderPattern.getCenter(FinderPattern.UR);
		axis.setOrigin(finderPattern.getCenter(FinderPattern.DL));
		centers[0][sqrtCenters - 1] = axis.translate(3, -3);
		//centers[0][sqrtCenters - 1] = finderPattern.getCenter(FinderPattern.DL);

		for (int y = 0; y < sqrtCenters; y++) {
			for (int x = 0; x < sqrtCenters; x++) {
				if (x == 1 && y == 0 && sqrtCenters == 3) { //型番7〜13の中央上の位置合せパターン
					centers[x][y] = Point.getCenter(centers[0][0], centers[sqrtCenters - 1][0]);
				}
				else if (x == 0 && y == 1 && sqrtCenters == 3) {//型番7〜13の左中央の位置合せパターン
					centers[x][y] = Point.getCenter(centers[0][0], centers[0][sqrtCenters - 1]);					
				}
				else if (x >= 1 && y >= 1){

					Line[] additionalLines = { 
							new Line(centers[x - 1][y - 1], centers[x][y - 1]),
							new Line(centers[x - 1][y - 1], centers[x - 1][y])};
					int dx = centers[x - 1][y].getX() - centers[x - 1][y - 1].getX();
					int dy = centers[x - 1][y].getY() - centers[x - 1][y - 1].getY();
					additionalLines[0].translate(dx,dy);
					dx = centers[x][y - 1].getX() - centers[x - 1][y - 1].getX();
					dy = centers[x][y - 1].getY() - centers[x - 1][y - 1].getY();
					additionalLines[1].translate(dx,dy);
					centers[x][y] = Point.getCenter(additionalLines[0].getP2(), additionalLines[1].getP2());
				}
				else // dummy alignment pattern (source is finder pattern)
					continue;
				try {
					centers[x][y] = getPrecisionCenter(image, centers[x][y]);
				} catch (AlignmentPatternEdgeNotFoundException e) {
					e.printStackTrace();
					throw e;
				}
				canvas.drawCross(centers[x][y], java.awt.Color.RED);
			}
			//System.out.println("");
		}
		return centers;
	}
*/




	static Point getPrecisionCenter(boolean[][] image, Point targetPoint) 
		throws AlignmentPatternNotFoundException {
		// find nearest dark point and update it as new rough center point 
		// when original rough center points light point 
		int tx = targetPoint.getX(), ty = targetPoint.getY();
		if ((tx < 0 || ty < 0) || (tx > image.length - 1 || ty > image[0].length -1))
			throw new AlignmentPatternNotFoundException("Alignment Pattern finder exceeded out of image");
			
		if (image[targetPoint.getX()][targetPoint.getY()] == QRCodeImageReader.POINT_LIGHT) {
			int scope = 0;
			boolean found = false;
			while (!found) {
				scope++;
				for (int dy = scope; dy > -scope; dy--) {
					for (int dx = scope; dx > -scope; dx--) {
						int x = targetPoint.getX() + dx;
						int y = targetPoint.getY() + dy;
						if ((x < 0 || y < 0) || (x > image.length - 1 || y > image[0].length -1))
							throw new AlignmentPatternNotFoundException("Alignment Pattern finder exceeded out of image");
						if (image[x][y] == QRCodeImageReader.POINT_DARK) {
							targetPoint = new Point(targetPoint.getX() + dx,targetPoint.getY() + dy);
              canvas.drawPoint(targetPoint, Color.RED);
							found = true;
						}
					}
				}
			}
		}
		int x, lx, rx, y, uy, dy;
		x = lx = rx = targetPoint.getX();
		y = uy = dy = targetPoint.getY();

		// GuoQing Hu's FIX
		while (lx >= 1                   && !targetPointOnTheCorner(image, lx, y, lx - 1, y)) lx--;
		while (rx < image.length - 1     && !targetPointOnTheCorner(image, rx, y, rx + 1, y)) rx++;
		while (uy >= 1                   && !targetPointOnTheCorner(image, x, uy, x, uy - 1)) uy--;
		while (dy < image[0].length - 1  && !targetPointOnTheCorner(image, x, dy, x, dy + 1)) dy++;
		
		return new Point((lx + rx + 1) / 2, (uy + dy + 1) / 2);
	}

	static boolean targetPointOnTheCorner(boolean[][] image, int x, int y, int nx, int ny) {
		if( x < 0 || y < 0 || nx < 0 || ny < 0 || x > image.length || y > image[0].length || nx > image.length || ny > image[0].length) {
			// System.out.println("Overflow: x="+x+", y="+y+" nx="+nx+" ny="+ny+" x.max="+image.length+", y.max="+image[0].length);
			throw new AlignmentPatternNotFoundException("Alignment Pattern Finder exceeded image edge");
			//return true;
		}
		else {
			return(image[x][y] == QRCodeImageReader.POINT_LIGHT &&
			image[nx][ny] == QRCodeImageReader.POINT_DARK);
		}
	}
	
	//get logical center coordinates of each alignment patterns
	public static Point[][] getLogicalCenter(FinderPattern finderPattern) {
		int version = finderPattern.getVersion();
		Point[][] logicalCenters = new Point[1][1];
		int[] logicalSeeds = new int[1];
		//create "column(row)-coordinates" which based on relative coordinates
			//int sqrtCenters = (version / 7) + 2;
			//logicalSeeds = new int[sqrtCenters];
			//for(int i=0 ; i<sqrtCenters ; i++) {
			//	logicalSeeds[i] = 6 + i * (4 + 4 * version) / (sqrtCenters - 1);
			//	logicalSeeds[i] -= (logicalSeeds[i] - 2) % 4;
			//}
		logicalSeeds = LogicalSeed.getSeed(version);
		logicalCenters = new Point[logicalSeeds.length][logicalSeeds.length];
		
		//create real relative coordinates
		for (int col = 0; col < logicalCenters.length; col++) { //列　///Line
			for (int row = 0; row < logicalCenters.length; row++) { //行 ///Line
				logicalCenters[row][col] = new Point(logicalSeeds[row], logicalSeeds[col]);
			}
		}
		return logicalCenters;
		
	}
	
}
