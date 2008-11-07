package jp.sourceforge.qrcode.exception;
public class FinderPatternNotFoundException extends Exception {
	String message = null;
	public FinderPatternNotFoundException(String message) {
		this.message = message;
	}
	public String getMessage() {
		return message;
	}
}
