package jp.sourceforge.reedsolomon;

import java.util.Arrays;

/**
 * タイトル: GF(2^8)
 *
 * @author Masayuki Miyazaki
 * http://sourceforge.jp/projects/reedsolomon/
 */
public final class Galois {
	public static final int POLYNOMIAL = 0x1d;
	private static final Galois instance = new Galois();
	private int[] expTbl = new int[255 * 2];	// 二重にもつことによりmul, div等を簡略化
	private int[] logTbl = new int[255 + 1];

	private Galois() {
		initGaloisTable();
	}

	public static Galois getInstance() {
		return instance;
	}

	/**
	 * スカラー、ベクターの相互変換テーブルの作成
	 */
	private void initGaloisTable() {
		int d = 1;
		for(int i = 0; i < 255; i++) {
			expTbl[i] = expTbl[255 + i] = d;
			logTbl[d] = i;
			d <<= 1;
			if((d & 0x100) != 0) {
				d = (d ^ POLYNOMIAL) & 0xff;
			}
		}
	}

	/**
	 * スカラー -> ベクター変換
	 *
	 * @param a int
	 * @return int
	 */
	public int toExp(int a) {
		return expTbl[a];
	}

	/**
	 * ベクター -> スカラー変換
	 *
	 * @param a int
	 * @return int
	 */
	public int toLog(int a) {
		return logTbl[a];
	}

	/**
	 * 誤り位置インデックスの計算
	 *
	 * @param length int
	 * 		データ長
	 * @param a int
	 * 		誤り位置ベクター
	 * @return int
	 * 		誤り位置インデックス
	 */
	public int toPos(int length, int a) {
		return length - 1 - logTbl[a];
	}

	/**
	 * 掛け算
	 *
	 * @param a int
	 * @param b int
	 * @return int
	 * 		= a * b
	 */
	public int mul(int a, int b)	{
		return (a == 0 || b == 0)? 0 : expTbl[logTbl[a] + logTbl[b]];
	}

	/**
	 * 掛け算
	 *
	 * @param a int
	 * @param b int
	 * @return int
	 * 		= a * α^b
	 */
	public int mulExp(int a, int b)	{
		return (a == 0)? 0 : expTbl[logTbl[a] + b];
	}

	/**
	 * 割り算
	 *
	 * @param a int
	 * @param b int
	 * @return int
	 * 		= a / b
	 */
	public int div(int a, int b) {
		return (a == 0)? 0 : expTbl[logTbl[a] - logTbl[b] + 255];
	}

	/**
	 * 割り算
	 *
	 * @param a int
	 * @param b int
	 * @return int
	 * 		= a / α^b
	 */
	public int divExp(int a, int b) {
		return (a == 0)? 0 : expTbl[logTbl[a] - b + 255];
	}

	/**
	 * 逆数
	 *
	 * @param a int
	 * @return int
	 * 		= 1/a
	 */
	public int inv(int a) {
		return expTbl[255 - logTbl[a]];
	}

	/**
	 * 数式の掛け算
	 *
	 * @param seki int[]
	 * 		seki = a * b
	 * @param a int[]
	 * @param b int[]
	 */
	public void mulPoly(int[] seki, int[] a, int[] b) {
		Arrays.fill(seki, 0);
		for(int ia = 0; ia < a.length; ia++) {
			if(a[ia] != 0) {
				int loga = logTbl[a[ia]];
				int ib2 = Math.min(b.length, seki.length - ia);
				for(int ib = 0; ib < ib2; ib++) {
					if(b[ib] != 0) {
						seki[ia + ib] ^= expTbl[loga + logTbl[b[ib]]];	// = a[ia] * b[ib]
					}
				}
			}
		}
	}

	/**
	 * シンドロームの計算
	 * @param data int[]
	 *		入力データ配列
	 * @param length int
	 *		データ長
	 * @param syn int[]
	 *		(x - α^0) (x - α^1) (x - α^2) ...のシンドローム
	 * @return boolean
	 *		true: シンドロームは総て0
	 */
	public boolean calcSyndrome(int[] data, int length, int[] syn) {
		int hasErr = 0;
		for(int i = 0; i < syn.length;  i++) {
			int wk = 0;
			for(int idx = 0; idx < length; idx++) {
				 wk = data[idx] ^ ((wk == 0)? 0 : expTbl[logTbl[wk] + i]);		// wk = data + wk * α^i
			}
			syn[i] = wk;
			hasErr |= wk;
		}
		return hasErr == 0;
	}
}
