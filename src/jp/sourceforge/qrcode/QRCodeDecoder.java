/*
 * created 2004/09/12
 */
package jp.sourceforge.qrcode;

import java.util.Vector;

import jp.sourceforge.qrcode.data.QRCodeImage;
import jp.sourceforge.qrcode.data.QRCodeSymbol;
import jp.sourceforge.qrcode.exception.DecodingFailedException;
import jp.sourceforge.qrcode.exception.InvalidDataBlockException;
import jp.sourceforge.qrcode.exception.SymbolNotFoundException;
import jp.sourceforge.qrcode.geom.Point;
//import jp.sourceforge.qrcode.ecc.ReedSolomon;
import jp.sourceforge.qrcode.util.DebugCanvas;
import jp.sourceforge.qrcode.util.DebugCanvasAdapter;
import jp.sourceforge.qrcode.reader.QRCodeDataBlockReader;
import jp.sourceforge.qrcode.reader.QRCodeImageReader;
import jp.sourceforge.reedsolomon.RsDecode;

public class QRCodeDecoder {
	int numTryDecode;
	QRCodeSymbol qrCodeSymbol;
	Vector results;
	Vector lastResults = new Vector();
	static DebugCanvas canvas;
	QRCodeImageReader imageReader;
	int numLastCorrectionFailures;

	class DecodeResult {
		int numCorrectionFailures;
		byte[] decodedBytes;
		public DecodeResult(byte[] decodedBytes,int numCorrectionFailures) {
			this.decodedBytes = decodedBytes;
			this.numCorrectionFailures = numCorrectionFailures;
		}
		public byte[] getDecodedBytes() {
			return decodedBytes;
		}
		public int getNumCorrectuionFailures() {
			return numCorrectionFailures;
		}
		public boolean isCorrectionSucceeded() {
			return numLastCorrectionFailures == 0;
		}
	}
	
	public static void setCanvas(DebugCanvas canvas) {
		QRCodeDecoder.canvas = canvas;
	}

	public static DebugCanvas getCanvas() {
		return QRCodeDecoder.canvas;
	}

	public QRCodeDecoder() {
		numTryDecode = 0;
		results = new Vector();
		QRCodeDecoder.canvas = new DebugCanvasAdapter();
	}
	
	public byte[] decode(QRCodeImage qrCodeImage) throws DecodingFailedException {
		Point[] adjusts = getAdjustPoints();
		Vector results = new Vector();
    numTryDecode = 0;
		while (numTryDecode < adjusts.length) {
			try {
				DecodeResult result = decode(qrCodeImage, adjusts[numTryDecode]);
				if (result.isCorrectionSucceeded()) {
					return result.getDecodedBytes();
				}
				else {
					results.addElement(result);
					canvas.println("Decoding succeeded but could not correct");
					canvas.println("all errors. Retrying..");
				}
			} catch (DecodingFailedException dfe) {
				if (dfe.getMessage().indexOf("Finder Pattern") >= 0)
					throw dfe;
			} finally {
				numTryDecode += 1;
			}
		}
		
		if (results.size() == 0)
			throw new DecodingFailedException("Give up decoding");
		
		int minErrorIndex = -1;
		int minError = Integer.MAX_VALUE;
		for (int i = 0; i < results.size(); i++) {
			DecodeResult result = (DecodeResult)results.elementAt(i);
			if (result.getNumCorrectuionFailures() < minError) {
				minError = result.getNumCorrectuionFailures();
				minErrorIndex = i;
			}
		}
		canvas.println("All trials need for correct error");
		canvas.println("Reporting #" + (minErrorIndex)+" that,");
		canvas.println("corrected minimum errors (" +minError + ")");
		canvas.println("Decoding finished.");
		return ((DecodeResult)results.elementAt(minErrorIndex)).getDecodedBytes();
	}
	
	Point[] getAdjustPoints() {
		// note that adjusts affect dependently
		// i.e. below means (0,0), (2,3), (3,4), (1,2), (2,1), (1,1), (-1,-1)
		
//		Point[] adjusts = {new Point(0,0), new Point(2,3), new Point(1,1), 
//				new Point(-2,-2), new Point(1,-1), new Point(-1,0), new Point(-2,-2)};
		Vector adjustPoints = new Vector();
		for (int d = 0; d < 4; d++)
			adjustPoints.addElement(new Point(1, 1));
		int lastX = 0, lastY = 0;
		for (int y = 0; y > -4; y--) {
			for (int x = 0; x > -4; x--) {
				if (x != y && ((x+y) % 2 == 0)) {
					adjustPoints.addElement(new Point(x-lastX, y-lastY));
					lastX = x;
					lastY = y;
				}
			}
		}
		Point[] adjusts = new Point[adjustPoints.size()];
		for (int i = 0; i < adjusts.length; i++)
			adjusts[i] = (Point)adjustPoints.elementAt(i);
		return adjusts;
	}
	
	DecodeResult decode(QRCodeImage qrCodeImage, Point adjust) 
		throws DecodingFailedException {
		try {
			if (numTryDecode == 0) {
				canvas.println("Decoding started");
				int[][] intImage = imageToIntArray(qrCodeImage);
				imageReader = new QRCodeImageReader();
				qrCodeSymbol = imageReader.getQRCodeSymbol(intImage);
			} else {
				canvas.println("--");
				canvas.println("Decoding restarted #" + (numTryDecode));
				qrCodeSymbol = imageReader.getQRCodeSymbolWithAdjustedGrid(adjust);
			}
		} catch (SymbolNotFoundException e) {
			throw new DecodingFailedException(e.getMessage());
		}
		canvas.println("Created QRCode symbol.");
		canvas.println("Reading symbol.");
		canvas.println("Version: " + qrCodeSymbol.getVersionReference());		
		canvas.println("Mask pattern: " + qrCodeSymbol.getMaskPatternRefererAsString());
		// blocks contains all (data and RS) blocks in QR Code symbol
		int[] blocks = qrCodeSymbol.getBlocks();
		canvas.println("Correcting data errors.");
		// now blocks turn to data blocks (corrected and extracted from original blocks)
    blocks = correctDataBlocks(blocks);

		try {
			byte[] decodedByteArray = 
				getDecodedByteArray(blocks, qrCodeSymbol.getVersion(), qrCodeSymbol.getNumErrorCollectionCode());
			return new DecodeResult(decodedByteArray, numLastCorrectionFailures);
		} catch (InvalidDataBlockException e) {
			canvas.println(e.getMessage());
			throw new DecodingFailedException(e.getMessage());
		}
	}
	
	
	int[][] imageToIntArray(QRCodeImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int[][] intImage = new int[width][height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				intImage[x][y] = image.getPixel(x,y);
			}
		}
		return intImage;
	}
	
	int[] correctDataBlocks(int[] blocks) {
		int numSucceededCorrections = 0;
    int numCorrectionFailures = 0;
		int dataCapacity = qrCodeSymbol.getDataCapacity();
		int[] dataBlocks = new int[dataCapacity];
		int numErrorCollectionCode = qrCodeSymbol.getNumErrorCollectionCode();
		int numRSBlocks = qrCodeSymbol.getNumRSBlocks();
		int eccPerRSBlock = numErrorCollectionCode / numRSBlocks;
		if (numRSBlocks == 1) {
      RsDecode corrector = new RsDecode(eccPerRSBlock / 2);
      int ret = corrector.decode(blocks);
      if (ret > 0)
        numSucceededCorrections += ret;
      else if (ret < 0)
        numCorrectionFailures++;
			return blocks;
		}
		else  { //we have to interleave data blocks because symbol has 2 or more RS blocks
			int numLongerRSBlocks = dataCapacity % numRSBlocks;
			if (numLongerRSBlocks == 0) { //symbol has only 1 type of RS block
				int lengthRSBlock = dataCapacity / numRSBlocks;
				int[][] RSBlocks = new int[numRSBlocks][lengthRSBlock];
				//obtain RS blocks
				for (int i = 0; i < numRSBlocks; i++) {
					for (int j = 0; j < lengthRSBlock; j++) {
						RSBlocks[i][j] = blocks[j * numRSBlocks + i];
					}
          canvas.println("eccPerRSBlock=" + eccPerRSBlock );
          RsDecode corrector = new RsDecode(eccPerRSBlock / 2);
          int ret = corrector.decode(RSBlocks[i]);
          if (ret > 0)
            numSucceededCorrections += ret;
          else if (ret < 0)
            numCorrectionFailures++;
				}
				//obtain only data part
				int p = 0;
				for (int i = 0; i < numRSBlocks; i++) {
					for (int j = 0; j < lengthRSBlock - eccPerRSBlock; j++) {
						dataBlocks[p++] = RSBlocks[i][j];
					}
				}
			}
			else { //symbol has 2 types of RS blocks
				int lengthShorterRSBlock = dataCapacity / numRSBlocks;
				int lengthLongerRSBlock = dataCapacity / numRSBlocks + 1;
				int numShorterRSBlocks = numRSBlocks - numLongerRSBlocks;
				int[][] shorterRSBlocks = new int[numShorterRSBlocks][lengthShorterRSBlock];
				int[][] longerRSBlocks = new int[numLongerRSBlocks][lengthLongerRSBlock];
				for (int i = 0; i < numRSBlocks; i++) {
					if (i < numShorterRSBlocks) { //get shorter RS Block(s)
						int mod = 0;
						for (int j = 0; j < lengthShorterRSBlock; j++) {
							if (j == lengthShorterRSBlock - eccPerRSBlock) mod = numLongerRSBlocks;
							shorterRSBlocks[i][j] = blocks[j * numRSBlocks + i + mod];
						}
            canvas.println("eccPerRSBlock(shorter)=" + eccPerRSBlock );
            RsDecode corrector = new RsDecode(eccPerRSBlock / 2);
            int ret = corrector.decode(shorterRSBlocks[i]);
            if (ret > 0)
              numSucceededCorrections += ret;
            else if (ret < 0)
              numCorrectionFailures++;

					}
					else { 	//get longer RS Blocks
						int mod = 0;
						for (int j = 0; j < lengthLongerRSBlock; j++) {
							if (j == lengthShorterRSBlock - eccPerRSBlock) mod = numShorterRSBlocks;
							longerRSBlocks[i - numShorterRSBlocks][j] = blocks[j * numRSBlocks + i - mod];
						}
            canvas.println("eccPerRSBlock(longer)=" + eccPerRSBlock );
            RsDecode corrector = new RsDecode(eccPerRSBlock / 2);
            int ret = corrector.decode(longerRSBlocks[i - numShorterRSBlocks]);
            if (ret > 0)
              numSucceededCorrections += ret;
            else if (ret < 0)
              numCorrectionFailures++;
					}
				}
				int p = 0;
				for (int i = 0; i < numRSBlocks; i++) {
					if (i < numShorterRSBlocks) {
						for (int j = 0; j < lengthShorterRSBlock - eccPerRSBlock; j++) {
							dataBlocks[p++] = shorterRSBlocks[i][j];
						}
					}
					else {
						for (int j = 0; j < lengthLongerRSBlock - eccPerRSBlock; j++) {
							dataBlocks[p++] = longerRSBlocks[i - numShorterRSBlocks][j];
						}
					}
				}
			}
			if (numSucceededCorrections > 0)
				canvas.println(String.valueOf(numSucceededCorrections) + " data errors corrected successfully.");
			else
				canvas.println("No errors found.");		
			numLastCorrectionFailures = numCorrectionFailures;
			return dataBlocks;
		}
	}
	
	byte[] getDecodedByteArray(int[] blocks, int version, int numErrorCorrectionCode) throws InvalidDataBlockException {
		byte[] byteArray;
		QRCodeDataBlockReader reader = new QRCodeDataBlockReader(blocks, version, numErrorCorrectionCode);
		try {
			byteArray = reader.getDataByte();
		} catch (InvalidDataBlockException e) {
			throw e;
		}
		return byteArray;
	}

}
