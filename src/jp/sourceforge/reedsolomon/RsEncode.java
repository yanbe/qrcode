package jp.sourceforge.reedsolomon;

/**
 * タイトル: RSコード・エンコーダ
 *
 * @author Masayuki Miyazaki
 * http://sourceforge.jp/projects/reedsolomon/
 */
public class RsEncode {
	public static final int RS_PERM_ERROR = -1;
	private static final Galois galois = Galois.getInstance();
	private int npar;
	private int[] encodeGx;

	public RsEncode(int npar) {
		this.npar = npar;
		makeEncodeGx();
	}

	/**
	 * 生成多項式配列の作成
	 *		G(x)=Π[k=0,n-1](x + α^k)
	 *		encodeGxの添え字と次数の並びが逆なのに注意
	 *		encodeGx[0]        = x^(npar - 1)の項
	 *		encodeGx[1]        = x^(npar - 2)の項
	 *		...
	 *		encodeGx[npar - 1] = x^0の項
	 */
	private void  makeEncodeGx() {
		encodeGx = new int[npar];
		encodeGx[npar - 1] = 1;
		for(int kou = 0; kou < npar; kou++) {
			int ex = galois.toExp(kou);			// ex = α^kou
			// (x + α^kou)を掛る
			for(int i = 0; i < npar - 1; i++) {
				// 現在の項 * α^kou + 一つ下の次数の項
				encodeGx[i] = galois.mul(encodeGx[i], ex) ^ encodeGx[i + 1];
			}
			encodeGx[npar - 1] = galois.mul(encodeGx[npar - 1], ex);		// 最下位項の計算
		}
	}

	/**
	 * RSコードのエンコード
	 *
	 * @param data int[]
	 *		入力データ配列
	 * @param length int
	 *		入力データ長
	 * @param parity int[]
	 *		パリティ格納用配列
	 * @param parityStartPos int
	 *		パリティ格納用Index
	 * @return int
	 *		0 : ok
	 *		< 0: エラー
	 */
	public int encode(int[] data, int length, int[] parity, int parityStartPos)	{
		if(length < 0 || length + npar > 255) {
			return RS_PERM_ERROR;
		}

		/*
		 * パリティ格納用配列
		 * wr[0]        最上位
		 * wr[npar - 1] 最下位		なのに注意
		 * これでパリティを逆順に並べかえなくてよいので、arraycopyが使える
		 */
		int[] wr = new int[npar];

		for(int idx = 0; idx < length; idx++) {
			int code = data[idx];
			int ib = wr[0] ^ code;
			for(int i = 0; i < npar - 1; i++) {
				wr[i] = wr[i + 1] ^ galois.mul(ib, encodeGx[i]);
			}
			wr[npar - 1] = galois.mul(ib, encodeGx[npar - 1]);
		}
		if(parity != null) {
			System.arraycopy(wr, 0, parity, parityStartPos, npar);
		}
		return 0;
	}

	public int encode(int[] data, int length, int[] parity)	{
		return encode(data, length, parity, 0);
	}

	public int encode(int[] data, int[] parity)	{
		return encode(data, data.length, parity, 0);
	}

/*
	public static void main(String[] args) {
		int[] data = new int[] {32, 65, 205, 69, 41, 220, 46, 128, 236};
		int[] parity = new int[17];
		RsEncode enc = new RsEncode(17);
		enc.encode(data, parity);
		System.out.println(java.util.Arrays.toString(parity));
		System.out.println("[42, 159, 74, 221, 244, 169, 239, 150, 138, 70, 237, 85, 224, 96, 74, 219, 61]");
	}
*/
}
