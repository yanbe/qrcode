package jp.sourceforge.reedsolomon;

/**
 * タイトル: BCH(15, 5)符号のエンコード/デコード
 *
 * @author Masayuki Miyazaki
 * http://sourceforge.jp/projects/reedsolomon/
 */
public final class BCH_15_5 {
	private static final int GX = 0x137;
	private static final BCH_15_5 instance = new BCH_15_5();
	private int[] trueCodes = new int[32];

	private BCH_15_5() {
		makeTrueCodes();
	}

	public static BCH_15_5 getInstance() {
		return instance;
	}

	/**
	 * 正規のコード・テーブルの作成
	 */
	private void makeTrueCodes() {
		for(int i = 0; i < trueCodes.length; i++) {
			trueCodes[i] = slowEncode(i);
		}
	}

	private int slowEncode(int data) {
		int wk = 0;
		data <<= 5;
		for(int i = 0; i < 5; i++) {
			wk <<= 1;
			data <<= 1;
			if(((wk ^ data) & 0x400) != 0) {
				wk ^= GX;
			}
		}
		return (data & 0x7c00) | (wk & 0x3ff);
	}

	public int encode(int data) {
		return trueCodes[data & 0x1f];
	}

	/**
	 * ハミング距離の計算
	 *
	 * @param c1 int
	 * @param c2 int
	 * @return int
	 */
	private static int calcDistance(int c1, int c2) {
		int n = 0;
		int wk = c1 ^ c2;
		while(wk != 0) {
			if((wk & 1) != 0) {
				n++;
			}
			wk >>= 1;
		}
		return n;
	}

	/**
	 * BCH(15, 5)符号のデコード
	 *
	 * @param data int
	 *		入力データ
	 * @return int
	 *		-1 : エラー訂正不能
	 *		>= 0 : 訂正データ
	 */
	public int decode(int data) {
		data &= 0x7fff;
		/*
		 * 最小符号間距離が7であるので、ハミング距離が3以下の正規のコードを探す
		 * エラー訂正と検出の組み合わせは、以下の通り
		 *		訂正	検出
		 *		  3
		 *		  2		  4
		 *		  1		  5
		 */
		for(int i = 0; i < trueCodes.length; i++) {
			int code = trueCodes[i];
			if(calcDistance(data, code) <= 3) {
				return code;
			}
		}
		return -1;
	}
}
