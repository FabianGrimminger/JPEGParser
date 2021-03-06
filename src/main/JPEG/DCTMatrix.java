package main.JPEG;

public class DCTMatrix {
	private Matrix matrixType; // not used!

	private int[][] matrix;
	private int ACindex;

	private int width;
	private int height;
	
	private static final int[] zigzag = {0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5, 12, 19, 26, 33,	40,	48,	41, 34,	27,	20,	13, 6, 7, 14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44, 51, 58, 59, 52, 45, 38, 31, 39, 46, 53, 60, 61, 54, 47, 55, 62, 63};
	

	public DCTMatrix(Matrix matrixtype, int width, int height) {
		this.matrixType = matrixtype;
		this.width = width;
		this.height = height;

		this.matrix = new int[height][width];
		this.ACindex = 0;
	}

	public int[][] getMatrix() {
		return this.matrix;
	}
	
	public Matrix getMatrixType()
	{
		return this.matrixType;
	}

	public void setDC(int value) {
		this.matrix[0][0] = value;
	}
	
	/*
	 *		0  1  2  3  4  5  6  7
	 *_____________________________
	 *0|	0  1  5  6  14 15 27 28
	 *1|	2  4  7  13 16 26 29 42
	 *2|	3  8  12 17 25 30 41 43
	 *3|	9  11 18 24 31 40 44 53
	 *4|	10 19 23 32 39 45 52 54
	 *5|	20 22 33 38 46 51 55 60
	 *6|	21 34 37 47 50 56 59 61
	 *7|	35 36 48 49 57 58 62 63 
	 * 
	 * 
	 * 
	 */
	public void setAC(int position, int value){
		//zigzag for a 8x8 matrix
		int matrixPos = zigzag[position];
		matrix[matrixPos/8][matrixPos%8] = value;		
	}
	
	public int getValue(int position)
	{
		int matrixPos = zigzag[position];
		return this.matrix[matrixPos/8][matrixPos%8];
	}

	@Override
	public String toString() {
		String result =this.matrixType.toString()+" "+this.ACindex+"\n";
		for(int[] i : this.matrix) {
			for(int j : i) {
				result += j+" ";
			}
			result +="\n";
		}
		return result;
	}

	public void setValue(int position, int value) {
		if(position ==0) {
			this.setDC(value);
		}else {
			this.setAC(position, value);
		}
		
	}
	
	@Override
	public DCTMatrix clone() {
		DCTMatrix matrix = new DCTMatrix(this.matrixType, this.width, this.height);
		for(int i=0;i<64;i++) {
			matrix.setValue(i, this.getValue(i));
		}
		return matrix;
	}
}
