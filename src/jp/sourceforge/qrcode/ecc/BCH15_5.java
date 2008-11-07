package jp.sourceforge.qrcode.ecc;
public class BCH15_5 {
	int[][] gf16;
	boolean[] receiveData;
	int numCorrectedError;
	public BCH15_5(boolean[] source) {
		gf16 = createGF16();
		receiveData = source;
		//printBit("receive data", receiveData);		
	}
	
	public boolean[] correct() {
		int[] s = calcSyndrome(receiveData);
		
		int[] errorPos = detectErrorBitPosition(s);
		boolean[] output = correctErrorBit(receiveData, errorPos);
		return output;
	}
	
	int[][] createGF16() {
		gf16 = new int[16][4];
		int[] seed = {1, 1, 0, 0};
		for (int i = 0; i < 4; i++)
			gf16[i][i] = 1;
		for (int i = 0; i < 4; i++)
			gf16[4][i] = seed[i];
		for (int i = 5; i < 16; i++) {
			for (int j = 1; j < 4; j++) {
				gf16[i][j] = gf16[i - 1][j - 1];
			}
			if (gf16[i - 1][3] == 1) {
				for (int j = 0; j < 4; j++)
					gf16[i][j] = (gf16[i][j] + seed[j]) % 2;
			}
		}
		return gf16;
	}
	
	int searchElement(int[] x) {
		int k;
		for (k = 0; k < 15; k++) {
			if (   x[0] == gf16[k][0]
					&& x[1] == gf16[k][1]
					&& x[2] == gf16[k][2]
			    && x[3] == gf16[k][3]
				 ) break;
		}
		return k;
	}
	
  /*String getInputString() {
		String inputString = null;
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		try {
			inputString = br.readLine();
		} catch (IOException e){}
		return inputString;
  }*/
  
	/*public int getInput() {
		System.out.print("Input Number 0-127: ");
		String str = getInputString();
		int input = Integer.parseInt(str);
		return input;
	}*/
	
	int[] getCode(int input) {
		int[] f = new int[15];
		int[] r = new int[8];
		
		for (int i = 0; i < 15; i++) {
			//1 + x + x^3
			int w1, w2;
			int yin;
			
			w1 = r[7];
			if (i < 7) {
				yin = (input >> (6 - i)) % 2;
				w2 = (yin + w1) % 2;
			}
			else {
				yin = w1;
				w2 = 0;
			}
			r[7] = (r[6] + w2) % 2;
			r[6] = (r[5] + w2) % 2;
			r[5] = r[4];
			r[4] = (r[3] + w2) % 2;
			r[3] = r[2];
			r[2] = r[1];
			r[1] = r[0];
			r[0] = w2;
			f[14 - i] = yin;
		}
		return f;
	}
	
	static String[] bitName = {"c0", "c1", "c2","c3", "c4", "c5","c6", "c7", "c8", "c9", 
															"d0", "d1", "d2", "d3", "d4", };
	
//  static void printBit(String title, boolean[] bit) {
//		System.out.print(title+": ");
//		for (int i = 0; i < 15; i++) {
//			if (i == 5) System.out.print(" ");
//			System.out.print((bit[14 - i] == true) ? "1" : "0" );				
//		}
//		System.out.print("  (");
//		for (int i = 0; i < 15; i++) {
//			if (i == 5) System.out.print(" ");
//			System.out.print(bitName[14 - i]);				
//		}
//		System.out.println(")");
//	}
	
	int addGF(int arg1, int arg2) {		
		int[] p = new int[4];
		for (int m = 0; m < 4; m++) {
			int w1 = (arg1 < 0 || arg1 >= 15) ? 0 : gf16[arg1][m];
			int w2 = (arg2 < 0 || arg2 >= 15) ? 0 : gf16[arg2][m];
			p[m] = (w1 + w2) % 2;
		}
		return searchElement(p);
	}

	/*void addRandomError(boolean[] f) {
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		
		int r;
		int numError = 3; //[TODO]change number of error and check
		
		for (int i = 0; i < numError; i++) {
			r = random.nextInt();
			if (r < 0) r = -r;
			if (r / (double)Integer.MAX_VALUE < 0.9) {
				int errorPos = r % 15;
				f[errorPos] = !f[errorPos];
			}	
		}
	}*/

	int[] calcSyndrome(boolean[] y) {
		int[] s = new int[5];
		int[] p = new int[4];
		int k;
		for (k = 0; k < 15; k++) {
			if (y[k] == true) for (int m = 0; m < 4; m++) 
				p[m] = (p[m] + gf16[k][m]) % 2;
		}
		k = searchElement(p);
		s[0] = (k >= 15)? -1 : k;
		/*System.out.println("SyndromeS1 = " + ((s[0] == -1) ?
				"0" : 
				"α^" + String.valueOf(s[0]))
		);*/
		
		s[1] = (s[0] < 0) ? -1 : (s[0] * 2) % 15;
		/*System.out.println("SyndromeS2 = " + ((s[1] == -1) ?
				"0" : 
				"α^" + String.valueOf(s[1]))
		);*/
		
		p = new int[4];
		for (k = 0; k < 15; k++) {
			if (y[k] == true) for (int m = 0; m < 4; m++) 
				p[m] = (p[m] + gf16[(k * 3) % 15][m]) % 2;
		}
			
		k = searchElement(p);

		s[2] = (k >= 15) ? -1 : k;
		/*System.out.println("SyndromeS3 = " + ((s[2] == -1) ?
				"0" : 
				"α^" + String.valueOf(s[2]))
		);*/
		
		s[3] = (s[1] < 0) ? -1 : (s[1] * 2) % 15;
		/*System.out.println("SyndromeS4 = " + ((s[3] == -1) ?
				"0" : 
				"α^" + String.valueOf(s[3]))
		);*/
		
		p = new int[4];
		for (k = 0; k < 15; k++) {
			if (y[k] == true) for (int m = 0; m < 4; m++) 
				p[m] = (p[m] + gf16[(k * 5) % 15][m]) % 2; 
		}
		k = searchElement(p);
		s[4] = (k >= 15)? -1 : k;
		/*System.out.println("SyndromeS5 = " + ((s[4] == -1) ?
				"0" : 
				"α^" + String.valueOf(s[4]))
		);*/
		
		return s;
	}

	
	int[] calcErrorPositionVariable(int[] s) {
		int[] e = new int[4];
		// calc σ1
		e[0] = s[0];
		//System.out.println("σ1 = " + String.valueOf(e[0]));
		
		// calc σ2
		int t = (s[0] + s[1]) % 15;
		int mother = addGF(s[2], t);
		mother = (mother >= 15) ? -1 : mother;
		
		t = (s[2] + s[1]) % 15;
		int child = addGF(s[4], t);
		child = (child >= 15) ? -1 : child;
		e[1] = (child < 0 && mother < 0) ? -1 : (child - mother + 15) % 15;
		
		//System.out.println("σ2 = " + String.valueOf(e[1]));
		
		// calc σ3
		t = (s[1] + e[0]) % 15;
		int t1 = addGF(s[2], t);
		t = (s[0] + e[1]) % 15;
		e[2] = addGF(t1, t);
		//System.out.println("σ3 = " + String.valueOf(e[2]));
		
		return e;
	}
	
	int[] detectErrorBitPosition(int[] s) {
		
		int[] e = calcErrorPositionVariable(s);
		int[] errorPos = new int[4];
		if (e[0] == -1) {
			//System.out.println("No errors.");
			return errorPos;
		}
		else if (e[1] == -1) {
			/*System.out.println("1 error. position is "+ 
					String.valueOf(e[0]) +
					" (" + bitName[e[0]] + ")")*/;
			errorPos[0] = 1;
			errorPos[1] = e[0];
			return errorPos;
		}
		//else {
			//System.out.println("2 or more errors.");			
		//}
		//int numError = 0;
		//int[] p;
		int x3, x2, x1;
		int t, t1, t2, anError;
		//error detection
		for (int i = 0; i < 15; i++) {
			//calc x^3 + σ1*x^2 + σ2*x + σ3 = 0
			x3 = (i * 3) % 15;
			x2 = (i * 2) % 15;
			x1 = i;
			
			//p = new int[4];
			
			t = (e[0] + x2) % 15;
			t1 = addGF(x3, t);
			
			t = (e[1] + x1) % 15;
			t2 = addGF(t, e[2]);
			
			anError = addGF(t1,t2);
			
			if (anError >= 15) {
				/*System.out.println("Error found. position is " + 
						String.valueOf(i) +
						"(" + bitName[i]+ ")");*/
				errorPos[0]++;
				errorPos[errorPos[0]] = i;
			}
		}
		
		return errorPos;
	}
	
	boolean[] correctErrorBit(boolean[] y, int[] errorPos) {
		//errorPos[0]にエラーの数、errorPos[1],[2],[3]に実際の位置が入っている
		for (int i = 1; i <= errorPos[0]; i++)
			y[errorPos[i]] = !y[errorPos[i]];
		
		numCorrectedError = errorPos[0];
		//printBit("Collected data", y);
		return y;
	}
	
	public int getNumCorrectedError() {
		return numCorrectedError;
	}
	
  /*boolean[] parseBooleanArray(String source)
  {
      int i = Integer.parseInt(source, 2);
      boolean b[] = new boolean[source.length()];
      for(int j = 0; j < 15; j++)
      {
          int t = i >> j & 1;
          if(t == 1)
              b[j] = true;
          else
              b[j] = false;
      }

      return b;
  }*/
}


