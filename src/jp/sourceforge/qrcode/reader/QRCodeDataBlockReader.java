/*
 * created: 2004/10/04
 */
package jp.sourceforge.qrcode.reader;

import jp.sourceforge.qrcode.QRCodeDecoder;
import jp.sourceforge.qrcode.exception.InvalidDataBlockException;
import jp.sourceforge.qrcode.util.DebugCanvas;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class QRCodeDataBlockReader {
	int[] blocks;
	int dataLengthMode;
	int blockPointer;
	int bitPointer;
	int dataLength;
	int numErrorCorrectionCode;
	DebugCanvas canvas;
	static final int MODE_NUMBER = 1;
	static final int MODE_ROMAN_AND_NUMBER = 2;
	static final int MODE_8BIT_BYTE = 4;
	static final int MODE_KANJI = 8;
	// this constant come from p16, JIS-X-0510(2004) 
	final int[][] sizeOfDataLengthInfo = {
		{10, 9, 8, 8}, {12, 11, 16, 10}, {14, 13, 16, 12}
	};

	public QRCodeDataBlockReader(int[] blocks, int version, int numErrorCorrectionCode) {
		blockPointer = 0;
		bitPointer = 7;
		dataLength = 0;
		this.blocks = blocks;
		this.numErrorCorrectionCode = numErrorCorrectionCode;
		if (version <= 9) dataLengthMode = 0;
		else if (version >= 10 && version <= 26) dataLengthMode = 1;
		else if (version >= 27 && version <= 40) dataLengthMode = 2;
		canvas = QRCodeDecoder.getCanvas();
	}
	
	int getNextBits(int numBits) throws ArrayIndexOutOfBoundsException {
//		System.out.println("numBits:" + String.valueOf(numBits));
//		System.out.println("blockPointer:" + String.valueOf(blockPointer));
//		System.out.println("bitPointer:" + String.valueOf(bitPointer));
		int bits = 0;
		if (numBits < bitPointer + 1) { // next word fits into current data block
			int mask = 0;
			for (int i = 0; i < numBits; i++) {
				mask += 1 << i;
			}
			mask <<= (bitPointer - numBits + 1);
			
			bits = (blocks[blockPointer] & mask) >> (bitPointer - numBits + 1);
			bitPointer -= numBits;
			return bits;
		}
		else if (numBits < bitPointer + 1 + 8) { // next word crosses 2 data blocks
			int mask1 = 0;
			for (int i = 0; i < bitPointer + 1; i++) {
				mask1 += 1 << i;
			}
			bits = (blocks[blockPointer] & mask1) << (numBits - (bitPointer + 1));
			blockPointer++;
			bits += (blocks[blockPointer]) >> (8 - (numBits - (bitPointer + 1)));

			bitPointer = bitPointer - numBits % 8;
			if (bitPointer < 0) {
				bitPointer = 8 + bitPointer;
			}
			return bits;	
		}
		else if (numBits < bitPointer + 1 + 16) { // next word crosses 3 data blocks
			int mask1 = 0; // mask of first block
			int mask3 = 0; // mask of 3rd block
			//bitPointer + 1 : number of bits of the 1st block
			//8 : number of the 2nd block (note that use already 8bits because next word uses 3 data blocks)
			//numBits - (bitPointer + 1 + 8) : number of bits of the 3rd block 
			for (int i = 0; i < bitPointer + 1; i++) {
				mask1 += 1 << i;
			}
			int bitsFirstBlock = (blocks[blockPointer] & mask1) << (numBits - (bitPointer + 1));
			blockPointer++;

			int bitsSecondBlock = blocks[blockPointer] << (numBits - (bitPointer + 1 + 8));
			blockPointer++;
			
			for (int i = 0; i < numBits - (bitPointer + 1 + 8); i++) {
				mask3 += 1 << i;
			}
			mask3 <<= 8 - (numBits - (bitPointer + 1 + 8));
			int bitsThirdBlock = (blocks[blockPointer] & mask3) >> (8 - (numBits - (bitPointer + 1 + 8)));
			
			bits = bitsFirstBlock + bitsSecondBlock + bitsThirdBlock;
			bitPointer = bitPointer - (numBits - 8) % 8;
			if (bitPointer < 0) {
				bitPointer = 8 + bitPointer;
			}
			return bits;
		}
		else {
			System.out.println("ERROR!");
			return 0;
		}
	}	
	
	int getNextMode() throws ArrayIndexOutOfBoundsException {
		//canvas.println("data blocks:"+ (blocks.length - numErrorCorrectionCode));
		if ((blockPointer > blocks.length - numErrorCorrectionCode -2))
			return 0;
		else
			return getNextBits(4);
	}
	
	int guessMode(int mode) {
		//correct modes: 0001 0010 0100 1000
		//possible data: 0000 0011 0101 1001 0110 1010 1100
		//               0111 1101 1011 1110 1111
//		MODE_NUMBER = 1;
//		MODE_ROMAN_AND_NUMBER = 2;
//		MODE_8BIT_BYTE = 4;
//		MODE_KANJI = 8;
		switch (mode) {
		case 3:
			return MODE_NUMBER;
		case 5:
			return MODE_8BIT_BYTE;
		case 6:
			return MODE_8BIT_BYTE;
		case 7:
			return MODE_8BIT_BYTE;
		case 9:
			return MODE_KANJI;
		case 10:
			return MODE_KANJI;
		case 11:
			return MODE_KANJI;
		case 12:
			return MODE_8BIT_BYTE;
		case 13:
			return MODE_8BIT_BYTE;
		case 14:
			return MODE_8BIT_BYTE;
		case 15:
			return MODE_8BIT_BYTE;
		default:
			return MODE_KANJI;
		}
	}

	int getDataLength(int modeIndicator) throws ArrayIndexOutOfBoundsException {
		int index = 0;
		while(true) {
			if ((modeIndicator >> index) == 1)
				break;
			index++;
		}
		
		return getNextBits(sizeOfDataLengthInfo[dataLengthMode][index]);
	}

	public byte[] getDataByte() throws InvalidDataBlockException {
		canvas.println("Reading data blocks.");
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		try {
			do {
				int mode = getNextMode();
				//canvas.println("mode: " + mode);
				if (mode == 0) {
					if (output.size() > 0)
						break;
					else
						throw new InvalidDataBlockException("Empty data block");
				}
				//if (mode != 1 && mode != 2 && mode != 4 && mode != 8)
				//	break;
				//}
				if (mode != MODE_NUMBER && mode != MODE_ROMAN_AND_NUMBER &&
						mode != MODE_8BIT_BYTE && mode != MODE_KANJI) {
/*					canvas.println("Invalid mode: " + mode);
					mode = guessMode(mode);
					canvas.println("Guessed mode: " + mode); */
					throw new InvalidDataBlockException("Invalid mode: " + mode + " in (block:"+blockPointer+" bit:"+bitPointer+")");
				}
				dataLength = getDataLength(mode);
				if (dataLength < 1)
					throw new InvalidDataBlockException("Invalid data length: " + dataLength);
				//canvas.println("length: " + dataLength);
				switch (mode) {
				case MODE_NUMBER:
					//canvas.println("Mode: Figure");
					output.write(getFigureString(dataLength).getBytes());
					break;
				case MODE_ROMAN_AND_NUMBER:
					//canvas.println("Mode: Roman&Figure");
					output.write(getRomanAndFigureString(dataLength).getBytes());
					break;
				case MODE_8BIT_BYTE:
					//canvas.println("Mode: 8bit Byte");
					output.write(get8bitByteArray(dataLength));
					break;
				case MODE_KANJI:
					//canvas.println("Mode: Kanji");
					output.write(getKanjiString(dataLength).getBytes());
					break;
				}
	//			
				//canvas.println("DataLength: " + dataLength);
				//System.out.println(dataString);
			} while (true);
		} catch (ArrayIndexOutOfBoundsException e) {
			//e.printStackTrace();
			throw new InvalidDataBlockException("Data Block Error in (block:"+blockPointer+" bit:"+bitPointer+")");
		} catch (IOException e) {
			throw new InvalidDataBlockException(e.getMessage());
		}
		return output.toByteArray();
	}
	
	public String getDataString() throws ArrayIndexOutOfBoundsException {
		canvas.println("Reading data blocks...");
		String dataString = "";
		do {
			int mode = getNextMode();
			canvas.println("mode: " + mode);
			if (mode == 0)
				break;
			//if (mode != 1 && mode != 2 && mode != 4 && mode != 8)
			//	break;
			//}
			if (mode != MODE_NUMBER && mode != MODE_ROMAN_AND_NUMBER &&
					mode != MODE_8BIT_BYTE && mode != MODE_KANJI) {
				// mode = guessMode(mode);
				//System.out.println("guessed mode: " + mode);

			}
				
			dataLength = getDataLength(mode);
			canvas.println(Integer.toString(blocks[blockPointer]));
			System.out.println("length: " + dataLength);
			switch (mode) {
			case MODE_NUMBER: 
				//canvas.println("Mode: Figure");
				dataString += getFigureString(dataLength);
				break;
			case MODE_ROMAN_AND_NUMBER:
				//canvas.println("Mode: Roman&Figure");
				dataString += getRomanAndFigureString(dataLength);
				break;
			case MODE_8BIT_BYTE:
				//canvas.println("Mode: 8bit Byte");
				dataString += get8bitByteString(dataLength);
				break;
			case MODE_KANJI:
				//canvas.println("Mode: Kanji");
				dataString += getKanjiString(dataLength);
				break;
			}
			//canvas.println("DataLength: " + dataLength);
			//System.out.println(dataString);
		} while (true);
		System.out.println("");
		return dataString;
	}
	
	
	String getFigureString(int dataLength) throws ArrayIndexOutOfBoundsException {
		int length = dataLength;
		int intData = 0;
		String strData = "";
		do {
			if (length >= 3) {
				intData = getNextBits(10);
				if (intData < 100) strData += "0";
				if (intData < 10) strData += "0";
				length -= 3;
			}
			else if (length == 2) {
				intData = getNextBits(7);
				if (intData < 10) strData += "0";
				length -= 2;
			}
			else if (length == 1) {
				intData = getNextBits(4);
				length -= 1;
			}				
			strData += Integer.toString(intData);
		} while (length > 0);
		
		return strData;
	}
	
	String getRomanAndFigureString(int dataLength) throws ArrayIndexOutOfBoundsException  {
		int length = dataLength;
		int intData = 0;
		String strData = "";
		final char[] tableRomanAndFigure = {
			 '0', '1', '2', '3', '4', '5',
	 		 '6', '7', '8', '9', 'A', 'B',
			 'C', 'D', 'E', 'F', 'G', 'H',
			 'I', 'J', 'K', 'L', 'M', 'N',
			 'O', 'P', 'Q', 'R', 'S', 'T',
			 'U', 'V', 'W', 'X', 'Y', 'Z',
			 ' ', '$', '%', '*', '+', '-',
			 '.', '/', ':'
			 };
		do {
			if (length > 1) {
				intData = getNextBits(11);
				int firstLetter = intData / 45;
				int secondLetter = intData % 45;
				strData += String.valueOf(tableRomanAndFigure[firstLetter]);
				strData += String.valueOf(tableRomanAndFigure[secondLetter]);
				length -= 2;
			}
			else if (length == 1) {
				intData = getNextBits(6);
				strData += String.valueOf(tableRomanAndFigure[intData]);
				length -= 1;
			}
		} while (length > 0);
		
		return strData;
	}
	
	public byte[] get8bitByteArray(int dataLength) throws ArrayIndexOutOfBoundsException  {
		int length = dataLength;
		int intData = 0;
		ByteArrayOutputStream output=new ByteArrayOutputStream();

		do {
			intData = getNextBits(8);
			output.write((byte)intData);
			length--;
		} while (length > 0);
		return output.toByteArray();
	}

	String get8bitByteString(int dataLength) throws ArrayIndexOutOfBoundsException  {
		int length = dataLength;
		int intData = 0;
		String strData = "";
		do {
			intData = getNextBits(8);
			strData+=(char)intData;
			length--;
		} while (length > 0);
		return strData;
	}

	String getKanjiString(int dataLength) throws ArrayIndexOutOfBoundsException {
		int length = dataLength;
		int intData = 0;
		String unicodeString = "";
		do {
			intData = getNextBits(13);
			int lowerByte = intData % 0xC0;
			int higherByte = intData / 0xC0;

			int tempWord = (higherByte << 8) + lowerByte;
			int shiftjisWord = 0;
			if (tempWord + 0x8140 <= 0x9FFC) { // between 8140 - 9FFC on Shift_JIS character set
				shiftjisWord = tempWord + 0x8140;
			}
			else { // between E040 - EBBF on Shift_JIS character set
				shiftjisWord = tempWord + 0xC140;
			}

			byte[] tempByte = new byte[2];
			tempByte[0] = (byte)(shiftjisWord >> 8);
			tempByte[1] = (byte)(shiftjisWord & 0xFF);
      try{ 
			  unicodeString += new String(tempByte, "Shift_JIS");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
			length--;
		} while (length > 0);

			
		return unicodeString;
	}
	
}
