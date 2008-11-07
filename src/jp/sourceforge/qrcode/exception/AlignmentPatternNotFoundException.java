package jp.sourceforge.qrcode.exception;
public class AlignmentPatternNotFoundException extends IllegalArgumentException {
	String message  = null;
	public AlignmentPatternNotFoundException(String message) {
		this.message = message;
	}
	public String getMessage() {
		return message;
	}
}
