package jp.sourceforge.qrcode.exception;
public class SymbolNotFoundException extends IllegalArgumentException {
	String message = null;
	public SymbolNotFoundException(String message) {
		this.message = message;
	}
	public String getMessage() {
		return message;
	}
}

