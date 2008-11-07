package jp.sourceforge.qrcode.util;

import jp.sourceforge.qrcode.geom.Line;
import jp.sourceforge.qrcode.geom.Point;
/* 
 * This class must be an "edition independent" class for debug information controll.
 * I think it's good idea to modify this class with a adapter pattern
 */
public class DebugCanvasAdapter implements DebugCanvas{
	public  void println(String string){};
	public  void drawPoint(Point point, int color){};
	public  void drawCross(Point point, int color){};
	public  void drawPoints(Point[] points, int color){};
	public  void drawLine(Line line, int color){};
	public  void drawLines(Line[] lines, int color){};
	public  void drawPolygon(Point[] points, int color){};
	public  void drawMatrix(boolean[][] matrix){};
}

