package jp.sourceforge.qrcode.data;

import java.util.Vector;

import jp.sourceforge.qrcode.ecc.BCH15_5;
import jp.sourceforge.qrcode.geom.*;
import jp.sourceforge.qrcode.pattern.LogicalSeed;

public class QRCodeSymbol {
	int version;
	int errorCollectionLevel;
	int maskPattern;
	int dataCapacity;
	boolean[][] moduleMatrix;
	int width, height;
	Point[][] alignmentPattern;
	final int[][] numErrorCollectionCode = {
		{ 7,10,13,17 },
		{ 10,16,22,28 }, { 15,26,36,44 }, { 20,36,52,64 }, { 26,48,72,88 }, { 36,64,96,112 },
		{ 40,72,108,130 }, { 48,88,132,156 }, { 60,110,160,192 }, { 72,130,192,224 }, { 80,150,224,264 }, { 96,176,260,308 }, { 104,198,288,352 },
		{ 120,216,320,384 }, { 132,240,360,432 }, { 144,280,408,480 }, { 168,308,448,532 }, { 180,338,504,588 }, { 196,364,546,650 }, { 224,416,600,700 },
		{ 224,442,644,750 }, { 252,476,690,816 }, { 270,504,750,900 }, { 300,560,810,960 }, { 312,588,870,1050 }, { 336,644,952,1110 }, { 360,700,1020,1200 },
		{ 390,728,1050,1260 }, { 420,784,1140,1350 }, { 450,812,1200,1440 }, { 480,868,1290,1530 }, { 510,924,1350,1620 }, { 540,980,1440,1710 }, { 570,1036,1530,1800 },
		{ 570,1064,1590,1890 }, { 600,1120,1680,1980 }, { 630,1204,1770,2100 }, { 660,1260,1860,2220 }, { 720,1316,1950,2310 }, { 750,1372,2040,2430 }
	};

	final int[][] numRSBlocks = {
		{ 1,1,1,1 },
		{ 1,1,1,1 }, { 1,1,2,2 }, { 1,2,2,4 }, { 1,2,4,4 }, { 2,4,4,4 },
		{ 2,4,6,5 }, { 2,4,6,6 }, { 2,5,8,8 }, { 4,5,8,8 }, { 4,5,8,11 }, { 4,8,10,11 }, { 4,9,12,16 },
		{ 4,9,16,16 }, { 6,10,12,18 }, { 6,10,17,16 }, { 6,11,16,19 }, { 6,13,18,21 }, { 7,14,21,25 }, { 8,16,20,25 },
		{ 8,17,23,25 }, { 9,17,23,34 }, { 9,18,25,30 }, { 10,20,27,32 }, { 12,21,29,35 }, {12,23,34,37 }, { 12,25,34,40 },
		{ 13,26,35,42 }, { 14,28,38,45 }, { 15,29,40,48 }, { 16,31,43,51 }, { 17,33,45,54 }, { 18,35,48,57 }, { 19,37,51,60 },
		{ 19,38,53,63 }, { 20,40,56,66 }, { 21,43,59,70 }, { 22,45,62,74 }, { 24,47,65,77 }, { 25,49,68,81 }
	};
	
	public boolean getElement(int x, int y) {
		return moduleMatrix[x][y];
	}
	
	
	public int getNumErrorCollectionCode() {
		return numErrorCollectionCode[version - 1][errorCollectionLevel];
	}
	
	public int getNumRSBlocks() {
		return numRSBlocks[version - 1][errorCollectionLevel];
	}
	
	public QRCodeSymbol(boolean[][] moduleMatrix) {
		this.moduleMatrix = moduleMatrix;
		width = moduleMatrix.length;
		height = moduleMatrix[0].length;
		initialize();
	}
	

	
	void initialize() {
		//calculate version by number of side modules
		version = (width - 17) / 4;
		Point[][] alignmentPattern = new Point[1][1];
	
		int[] logicalSeeds = new int[1];
/*	
		//create "row coordinates" be based on relative coordinates
		if (version >= 2 && version <= 6) {
			logicalSeeds = new int[2];
			logicalSeeds[0] = 6;
			logicalSeeds[1] = 10 + 4 * version;
			alignmentPattern = new Point[logicalSeeds.length][logicalSeeds.length];
		}
		else if (version >= 7 && version <= 13) {
			logicalSeeds = new int[3];
			logicalSeeds[0] = 6;
			logicalSeeds[1] = 8 + 2 * version;
			logicalSeeds[2] = 10 + 4 * version;
			alignmentPattern = new Point[logicalSeeds.length][logicalSeeds.length];
		}
*/

		if (version>=2 && version <=40) {
			//int sqrtCenters = (version / 7) + 2;
			//logicalSeeds = new int[sqrtCenters];
			//for(int i=0 ; i<sqrtCenters ; i++) {
			//	logicalSeeds[i] = 6 + i * (4 + 4 * version) / (sqrtCenters - 1);
			//	logicalSeeds[i] -= (logicalSeeds[i] - 2) % 4;
			//}
			logicalSeeds = LogicalSeed.getSeed(version);
			alignmentPattern = new Point[logicalSeeds.length][logicalSeeds.length];
		}
		
		//obtain alignment pattern's center coordintates by logical seeds
		for (int col = 0; col < logicalSeeds.length; col++) { //列　
			for (int row = 0; row < logicalSeeds.length; row++) { //行
				alignmentPattern[row][col] = new Point(logicalSeeds[row], logicalSeeds[col]);
			}
		}
		this.alignmentPattern = alignmentPattern;

		
		dataCapacity = calcDataCapacity();
		
		boolean[] formatInformation = readFormatInformation();
		decodeFormatInformation(formatInformation);
		
		unmask();
	}
	public int getVersion() {
		return version;
	}
	public String getVersionReference() {
		final char[] versionReferenceCharacter = {'L', 'M', 'Q', 'H'};
		
		return Integer.toString(version)+ "-" + 
			versionReferenceCharacter[errorCollectionLevel];
	}
	
	public Point[][] getAlignmentPattern() {
		return alignmentPattern;
	}

	boolean[] readFormatInformation() {
		boolean[] modules = new boolean[15];

		//obtain format information from symbol
		for (int i = 0; i <= 5; i++)
			modules[i] = getElement(8, i);
		
		modules[6] = getElement(8, 7);
		modules[7] = getElement(8, 8);
		modules[8] = getElement(7, 8);
		
		for (int i = 9; i <= 14; i++)
			modules[i] = getElement(14 - i, 8);
		
		//unmask Format Information's with given mask pattern. (JIS-X-0510(2004), p65)
		int maskPattern = 0x5412;
		
		for (int i = 0; i <= 14; i++) {
			boolean xorBit = false;
			if (((maskPattern >>> i) & 1) == 1)
				xorBit = true;
			else
				xorBit = false;
			
			// get unmasked format information with bit shift
			if (modules[i] == xorBit) 
				modules[i] = false;
			else
				modules[i] = true;
		}
		
		BCH15_5 corrector = new BCH15_5(modules);
		boolean[] output = corrector.correct();
		//int numError = corrector.getNumCorrectedError();
		//if (numError > 0)
		//	canvas.println(String.valueOf(numError) + " format errors corrected.");
		boolean[] formatInformation = new boolean[5];
		for (int i = 0; i < 5; i++)
			formatInformation[i] = output[10 + i];
		
		return formatInformation;
		
	}
	
	void unmask() {
		boolean[][] maskPattern = generateMaskPattern();

		int size = getWidth();
		
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				if (maskPattern[x][y] == true) {
					reverseElement(x, y);
				}
			}
		}
	}
	
	
	boolean[][] generateMaskPattern() {
		int maskPatternReferer = getMaskPatternReferer();
		
		int width = getWidth();
		int height = getHeight();
		boolean[][] maskPattern = new boolean[width][height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (isInFunctionPattern(x, y))
					continue;
				switch (maskPatternReferer) {
				case 0: // 000
					if ((y + x) % 2 == 0)
						maskPattern[x][y] = true;
					break;
				case 1: // 001
					if (y % 2 == 0)
						maskPattern[x][y] = true;
					break;
				case 2: // 010
					if (x % 3 == 0)
						maskPattern[x][y] = true;
					break;
				case 3: // 011
					if ((y + x) % 3 == 0)
						maskPattern[x][y] = true;
					break;
				case 4: // 100
					if ((y / 2 + x / 3) % 2 == 0)
						maskPattern[x][y] = true;
					break;
				case 5: // 101
					if ((y * x) % 2 + (y * x) % 3 == 0)
						maskPattern[x][y] = true;
					break;
				case 6: // 110
					if (((y * x) % 2 + (y * x) % 3) % 2 == 0)
						maskPattern[x][y] = true;
					break;
				case 7: // 111
					if (((y * x) % 3 + (y + x) % 2) % 2 == 0)
						maskPattern[x][y] = true;
					break;
				}
			}
		}
		return maskPattern;
	}
	
	private int calcDataCapacity() {
		int numFunctionPatternModule = 0;
		int numFormatAndVersionInfoModule = 0;
		int version = this.getVersion();
		//System.out.println("Version:" + String.valueOf(version));

		if (version <= 6)
			numFormatAndVersionInfoModule = 31;
		else
			numFormatAndVersionInfoModule = 67;
		

		// the number of finter patterns :
		int sqrtCenters = (version / 7) + 2;
		// the number of modules left when we remove the patterns modules
		// 3*64 for the 3 big ones,
		// sqrtCenters*sqrtCenters)-3)*25 for the small ones
		int modulesLeft = (version==1?192:192+((sqrtCenters*sqrtCenters)-3)*25);
		// Don't ask me how I found that one...
		//
		numFunctionPatternModule = modulesLeft +8 * version + 2 - (sqrtCenters-2)*10;

		int dataCapacity = (width * width - numFunctionPatternModule - numFormatAndVersionInfoModule) / 8;

		return dataCapacity;
	}

	
	public int getDataCapacity() {
		return this.dataCapacity;
	}
	
	void decodeFormatInformation(boolean[] formatInformation) {
		if (formatInformation[4] == false)
			if (formatInformation[3] == true)
				errorCollectionLevel = 0;
			else
				errorCollectionLevel = 1;
		else
			if (formatInformation[3] == true)
				errorCollectionLevel = 2;
			else
				errorCollectionLevel = 3;

		for (int i = 2; i >= 0; i--)
			if (formatInformation[i] == true)
				maskPattern += 1 << i;
	}
	public int getErrorCollectionLevel() {
		return errorCollectionLevel;
	}
	public int getMaskPatternReferer() {
		return maskPattern;
	}
	
	// for debug
	public String getMaskPatternRefererAsString() {
		String maskPattern = Integer.toString(getMaskPatternReferer() ,2);
		int length = maskPattern.length();
		for (int i = 0; i < 3 - length; i++)
			maskPattern = "0" + maskPattern;
		return maskPattern;
	}	
	
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	
	public int[] getBlocks() {
		int width = getWidth();
		//System.out.println("SymbolWidth:" + Integer.toString(symbol.getWidth()));
		//System.out.println("SymbolHeight:" + Integer.toString(symbol.getHeight()));
		int height = getHeight();
		int x = width - 1;
		int y = height - 1;
		Vector codeBits = new Vector();
		Vector codeWords = new Vector();
		int tempWord = 0;
		int figure = 7;
		int isNearFinish = 0;
		final boolean READ_UP = true;
		final boolean READ_DOWN = false;
		boolean direction = READ_UP;
		do {
			//canvas.drawPoint(new Point(x * 4 +8 , y * 4 + 47), Color.RED);
			codeBits.addElement(new Boolean(getElement(x, y)));
			//System.out.println(Integer.toString(codeBits.size()));
			//int ratio = canvas.getWidth() / symbol.getWidth();
			//int offsetX = (canvas.getWidth() - symbol.getWidth() * ratio) / 2;
			//int offsetY = (canvas.getHeight() - symbol.getHeight() * ratio) / 2;
			//canvas.drawPoint(new Point(offsetX + x * ratio + 3, offsetY + y * ratio + 3), 0xFF0000);
			if (getElement(x, y) == true) {
				tempWord += 1 << figure;
			}
			//System.out.println(new Point(x, y).toString() + " " + symbol.getElement(x, y));
			figure--;
			if (figure == -1) {
				codeWords.addElement(new Integer(tempWord));
				//System.out.print(codeWords.size() + ": ");
				//System.out.println(tempWord);
				figure = 7;
				tempWord = 0;
			}
			// determine module that read next
			do {
				if (direction == READ_UP) {
					if ((x + isNearFinish) % 2 == 0) //if right side of two column
						x--; // to left
					else { 
						if (y > 0) { //be able to move upper side
							x++;
							y--;
						}
						else { //can't move upper side
							x--; //change direction
							if (x == 6){
								x--;
								isNearFinish=1; // after through horizontal Timing Pattern, move pattern is changed
							}
							direction = READ_DOWN;
						}
					}			
				}
				
				else {
					if ((x + isNearFinish) % 2 == 0) //if left side of two column
						x--; 
					else {
						if (y < height - 1) {
							x++;
							y++;
						}
						else {
							x--;
							if (x == 6){
								x--;
								isNearFinish=1;
							}
							direction = READ_UP;
						}
					}				
				}
			} while (isInFunctionPattern(x, y));

		} while (x != -1);
		
		int[] gotWords = new int[codeWords.size()];
		for (int i = 0; i < codeWords.size(); i++) {
			Integer temp = (Integer)codeWords.elementAt(i);
			gotWords[i] = temp.intValue();
		}
		return gotWords; 
	}	
	
	public void reverseElement(int x, int y) {
		moduleMatrix[x][y] = !moduleMatrix[x][y];
	}
	public boolean isInFunctionPattern(int targetX, int targetY) {
		if (targetX < 9 && targetY < 9) //in Left-Up Finder Pattern or function patterns around it
			return true;
		if (targetX > getWidth() - 9 && targetY < 9) //in Right-up Finder Pattern or function patterns around it
			return true;
		if (targetX < 9  && targetY > getHeight() - 9) //in Left-bottom Finder Pattern or function patterns around it
			return true;
		
		if (version >= 7) {
			if (targetX > getWidth() - 12  && targetY < 6)
				return true;
			if (targetX < 6 && targetY > getHeight() - 12)
				return true;
		}
		// in timing pattern
		if (targetX == 6 || targetY == 6)
			return true;

		// in alignment pattern. 		
		Point[][] alignmentPattern = getAlignmentPattern();
		int sideLength = alignmentPattern.length;

		for (int y = 0; y < sideLength; y++) {
			for (int x = 0; x < sideLength; x++) {
					if (!(x == 0 && y == 0) && !(x == sideLength - 1 && y == 0) && !(x == 0 && y == sideLength - 1)) 
					if (Math.abs(alignmentPattern[x][y].getX() - targetX) < 3 &&
							Math.abs(alignmentPattern[x][y].getY() - targetY) < 3)
						return true;
			}
		}

		return false;
	}
}
