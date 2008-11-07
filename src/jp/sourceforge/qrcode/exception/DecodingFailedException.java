package jp.sourceforge.qrcode.exception;

// Possible Exceptions
//
//DecodingFailedException
//- SymbolNotFoundException
//  - FinderPatternNotFoundException
//  - AlignmentPatternNotFoundException
//- SymbolDataErrorException
//  - IllegalDataBlockException
//	- InvalidVersionInfoException
//- UnsupportedVersionException

public class DecodingFailedException extends IllegalArgumentException {
	String message = null;
	public DecodingFailedException(String message) {
		this.message = message;
	}
	public String getMessage() {
		return message;
	}
}
