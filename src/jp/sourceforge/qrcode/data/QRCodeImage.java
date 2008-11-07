package jp.sourceforge.qrcode.data;
/*
  this interface aiming platform independent implementation,
  will be used from QRCodeUtility.parseImage()
*/
public interface QRCodeImage {
	public int getWidth();
	public int getHeight();
	public int getPixel(int x, int y);
}
