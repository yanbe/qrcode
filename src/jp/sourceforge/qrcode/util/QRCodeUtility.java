package jp.sourceforge.qrcode.util;

/* 
 * This class must be modified as a adapter class for "edition dependent" methods
 */

public class QRCodeUtility {
	// Because CLDC1.0 does not support Math.sqrt(), we have to define it manually.
	// faster sqrt (GuoQing Hu's FIX)
	public static int sqrt(int val) { 
//		using estimate method from http://www.azillionmonkeys.com/qed/sqroot.html 
//		System.out.print(val + ", " + (int)Math.sqrt(val) + ", "); 
		int temp, g=0, b = 0x8000, bshft = 15; 
		do { 
			if (val >= (temp = (((g << 1) + b)<<bshft--))) { 
				g += b; 
				val -= temp; 
			} 
		} while ((b >>= 1) > 0); 
	
		return g; 
	} 

// for au by KDDI Profile Phase 3.0
//	public static int[][] parseImage(Image image) {
//		int width = image.getWidth();
//		int height = image.getHeight();
//		Image mutable = Image.createImage(width, height);
//		Graphics g = mutable.getGraphics();
//		g.drawImage(image, 0, 0, Graphics.TOP|Graphics.LEFT);
//		ExtensionGraphics eg = (ExtensionGraphics) g;
//		int[][] result = new int[width][height];
//		
//		for (int x = 0; x < width; x++) {
//			for (int y = 0; y < height; y++) {
//				result[x][y] = eg.getPixel(x, y);
//			}
//		}
//		return result;
//	}
//	
//	public static int[][] parseImage(byte[] imageData) {
//		return parseImage(Image.createImage(imageData, 0, imageData.length));
//	}
	

}
