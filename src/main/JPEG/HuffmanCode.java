package main.JPEG;

import java.util.ArrayList;
import java.util.List;

import main.Util.BitConverter;

public class HuffmanCode {
	
	private StartOfFrameMarker sofMarker;
	private HuffmanTable huffmanTable;
	private String data;
	private List<DCTMatrix> decodedData;
	private DCTMatrix[] decodedData_array;
	private List<Integer> encodedData;
	private int startSpectralSelection; //not used
	private int endSpectralSelection; //not used
	private int height;//not used
	private int width;//not used

	public HuffmanCode(StartOfFrameMarker sofMarker, HuffmanTable huffmanTable, String encodedData, int startspectralselection, int endspectralselection /*, int width, int height*/)
	{
		this.sofMarker = sofMarker;
		this.huffmanTable = huffmanTable;
		this.data = encodedData;
		this.startSpectralSelection = startspectralselection;
		this.endSpectralSelection = endspectralselection;
		//this.height = height;
		//this.width = width;
		
		this.decodedData = new ArrayList<>();
	}
	
	public DCTMatrix[] getDecodedData()
	{
		return this.decodedData_array;
	}
	
	public List getEncodedData()
	{
		return this.encodedData;
	}
	
	private void reduceData(int beginIndex, int endIndex)
	{
		this.data = this.data.substring(beginIndex, endIndex);
	}
	
	public void decode()
	{
		int matrixwidth = 8;
		int matrixheight = 8;
		int hsum = 0;
		int wsum = 0;
		boolean done = false;
		while (!done)
		{
			for (int i = 0; i < this.sofMarker.getNumberOfComponents(); i++)
			{
				for (int horizontal = 0; horizontal < this.sofMarker.getHorizontalFactors()[i]; horizontal++)
				{
					for (int vertical = 0; vertical < this.sofMarker.getVerticalFactors()[i]; vertical++)
					{
						if (i == 0)
						{
							DCTMatrix luminanceMatrix = new DCTMatrix(Matrix.LUMINANCE, matrixwidth, matrixheight);
							int dcvalue = this.decodeDC(this.huffmanTable.LuminanceDC);
							luminanceMatrix.setDC(dcvalue);
							this.decodeAC(this.huffmanTable.LuminanceAC, luminanceMatrix);
							this.decodedData.add(luminanceMatrix);
						}
						else if (i == 1)
						{
							DCTMatrix chrominanceMatrixCb = new DCTMatrix(Matrix.CB, matrixwidth ,matrixheight);
							int dcvalue = this.decodeDC(this.huffmanTable.ChrominanceDC);
							chrominanceMatrixCb.setDC(dcvalue);
							this.decodeAC(this.huffmanTable.ChrominanceAC, chrominanceMatrixCb);
							this.decodedData.add(chrominanceMatrixCb);
						}
						else if(i == 2)
						{
							DCTMatrix chrominanceMatrixCr = new DCTMatrix(Matrix.CR, matrixwidth, matrixheight);
							int dcvalue = this.decodeDC(this.huffmanTable.ChrominanceDC);
							chrominanceMatrixCr.setDC(dcvalue);
							this.decodeAC(this.huffmanTable.ChrominanceAC, chrominanceMatrixCr);
							this.decodedData.add(chrominanceMatrixCr);
						}
					}
				}
			}
			
			hsum += matrixheight;
			wsum += matrixwidth;
			if (this.data.length() < 8)
			{
				done = true;
			}
		}
		//this.decodedData_array = (DCTMatrix[]) this.decodedData.toArray();
		this.decodedData_array = new DCTMatrix[this.decodedData.size()];
		System.arraycopy(this.decodedData.toArray(), 0, this.decodedData_array, 0, this.decodedData.size());
	}
	//the method to call
	private int decodeDC(String[][] table)
	{
		String c = this.getNextHuffmanDecodedValue(table);
		if (c == null) {
			throw new RuntimeException("decodeDC: c is null!");
		} //TODO react when error occurs
		else 
		{
			int code = Integer.parseInt(c);
			if (code > 0)
			{
				String bits = this.data.substring(0, code);
				this.reduceData(code, this.data.length()); //remove the additional bits from the data stream
				
				int dcvalue = this.getDecodedDCACValue(bits, code); //the actual DC Value
				return dcvalue;
			}
			else
			{
				return 0;
			}
		} 
		//return 0;
	}
	
	private DCTMatrix decodeAC(String[][] table, DCTMatrix matrix)
	{
		for (int position = 1; position <= this.endSpectralSelection; position++)
		{
			String code_s = this.getNextHuffmanDecodedValue(table);
			if (code_s == null) { throw new RuntimeException("decodeAC: code_s is null!"); } //TODO react when error occurs
			else 
			{
				int code = Integer.parseInt(code_s);
				if(code == 0) //remaining values are zero
				{
					return matrix;
				}				
				else if(code == 0xF0) //next 16 values are zero
				{
					for(int j=0;j<16;j++) {
						matrix.setAC(position, 0);
						position++;
					}
					//to avoid to jump over one position; 
					//the for loop does the last position++; 
					//positon has to stay on the last inserted value
					position--;
				}
				else
				{
					int zeroRun = BitConverter.getHigherBits(code);
					int magnitude = BitConverter.getLowerBits(code);					
					
					String bits = this.data.substring(0, magnitude);
					this.reduceData(magnitude, this.data.length());
					
					int value = this.getDecodedDCACValue(bits, magnitude);					
					
					for(int j = 0;j<zeroRun;j++) {
						matrix.setAC(position, 0);
						position++;
					}
					
					matrix.setAC(position,value);
				}
			}
		}
		return matrix;		
	}
	
	
	/* 
	 * gets the real DC/AC value of the additional bits
	 * for AC 0 Value is not possible
	 * 
	 * DC Code	Size	Additional Bits									DC Value
	 * 00		0	 					  									  0
	 * 01		1						0	1							   -1	1
	 * 02		2					00,01	10,11					    -3,-2	2,3
	 * 03		3		  000,001,010,011	100,101,110,111		  -7,-6,-5,-4	4,5,6,7
	 * 04		4			0000,...,0111	1000,...,1111		   -15,...,-8	8,...,15
	 * 05		5			  0 0000,...	...,1 1111			  -31,...,-16	16,...,31
	 * 06		6			 00 0000,...	...,11 1111			  -63,...,-32	32,...,63
	 * 07		7		    000 0000,...	...,111 1111		 -127,...,-64	64,...,127
	 * 08		8		   0000 0000,...	...,1111 1111		-255,...,-128	128,...,255
	 * 09		9		 0 0000 0000,...	...,1 1111 1111		-511,...,-256	256,...,511
	 * 0A		10		00 0000 0000,...	...,11 1111 1111   -1023,...,-512	512,...,1023
	 * 0B		11		000 0000 0000,...	...,111 1111 1111 -2047,...,-1024	1024,...,2047
	 *
	 *@param bits - the bits in the data stream
	 *@param length - the lengths of the bits
	 *@return DC Value
	 */
	private int getDecodedDCACValue(String bits, int length)
	{
		double x = Math.pow(2, length);
		int min = ((int)x-1) * (-1);
		int highestnegativ = (int) x / (-2);
		
		int c = 0;
		for (int i = 0; i < bits.length(); i++)
		{
			double mult = Math.pow(2, i);
			if (bits.charAt(bits.length()-i-1) == '1')
			{
				c += (mult);
			}
		}
		
		int k = min + c;
		if (k <= highestnegativ)
		{
			return k;
		}
		else
		{
			int temp = k - highestnegativ;
			k = highestnegativ * (-1) + (temp-1);
			return k;
		}
	}
	
	//returns the next huffman encoded value in the data stream
	private String getNextHuffmanDecodedValue(String[][] table)
	{
		//System.out.println("Data: " + this.data);
		for (int i = 0; i < table.length; i++)
		{
			//System.out.println("Trying: "+table[i][0]);
			if (this.data.startsWith(table[i][0]))
			{
				this.reduceData(table[i][0].length(), this.data.length());
				return table[i][1];
			}
		}
		return null;
	}

	
	public void encode()
	{
		String encodedBinary = "";
		for (int i = 0; i < this.decodedData_array.length; i++)
		{
			encodedBinary += encodeDCValue(i);
			encodedBinary += encodeACValue(i);
		}
		int x = encodedBinary.length() % 8;
		for (int i = 0; i < 8-x; i++)
		{
			encodedBinary += "1";
		}
		this.encodedData = BitConverter.convertBitStringToIntegerList(encodedBinary);
	}

	private String encodeDCValue(int i) 
	{
		String encodedBits = "";
		int dcValue = ((DCTMatrix)this.decodedData_array[i]).getValue(0);
		if (dcValue == 0)
		{
			encodedBits = this.getEncodedBitsDependingOnMatrixType(true, i, dcValue);
			return encodedBits;
		}
		
		String encodedDC = this.getEncodedDCACValue(dcValue);
		String bitsForLength = getEncodedBitsDependingOnMatrixType(true, i, encodedDC.length());
		if (bitsForLength == null) { 
			System.err.println("encodeDCValue: bitsForLength is null!");
			throw new RuntimeException("encodeDCValue: bitsForLength is null!");
			} //TODO react when error occurs
		else
		{
			encodedBits = bitsForLength + encodedDC;
		}
		return encodedBits;
	}
	
	private String encodeACValue(int i)
	{
		String acString = "";
		int zerorun = 0;
		int zrlrun = 0;
		for (int pos = 1; pos <= this.endSpectralSelection; pos++)
		{
			int acValue = ((DCTMatrix) this.decodedData_array[i]).getValue(pos);
			if (acValue == 0)
			{
				zerorun++;
				if (zerorun == 16)
				{
					zrlrun++;
					String zrl = getEncodedBitsDependingOnMatrixType(false, i, 0xF0);
					if (zrl == null) {
						System.out.println("encodeACValue: zrl is null!");
						throw new RuntimeException("encodeACValue: zrl is null!");} //TODO react on error
					else
					{
						acString += zrl;
					}
					zerorun = 0;
				}
				
			}
			else
			{
				String encodedAC = this.getEncodedDCACValue(acValue);
				int v = zerorun * 16 + encodedAC.length();
				String x = getEncodedBitsDependingOnMatrixType(false, i, v);
				if (x == null) {
					System.out.println("encodeACValue: x is null!"); 
					throw new RuntimeException("encodeACValue: x is null!");
				} //TODO react on error
				else
				{
					acString += x + encodedAC;
				}
				zerorun = 0;
				zrlrun = 0;
			}
		}
		String endOfBlock = getEncodedBitsDependingOnMatrixType(false, i, 0);
		if (zrlrun > 0)
		{
			String zrlBits = getEncodedBitsDependingOnMatrixType(false, i, 0xF0);
			while (acString.endsWith(zrlBits))
			{
				acString = acString.substring(0, acString.length()-zrlBits.length());
			}
			acString += endOfBlock;
		}
		else
		{
			if (zerorun > 0)
			{
				acString += endOfBlock;
			}
		}
		return acString;
	}
	
	private String getEncodedDCACValue(int value)
	{
		int positivValue = Math.abs(value); 
		double log = Math.log10(positivValue) / Math.log10(2);
		int length = (int) Math.ceil(log);
		String bits = BitConverter.convertToBitString(positivValue, length);
		if (value > 0) { return bits; }
		else { return BitConverter.invertBitString(bits); }
	}
	
	private String getEncodedBitsDependingOnMatrixType(boolean DC, int i, int value)
	{
		String x = null;
		if (((DCTMatrix) this.decodedData_array[i]).getMatrixType() == Matrix.LUMINANCE)
		{
			if (DC)
			{
				x = getHuffmanEncodedBits(this.huffmanTable.LuminanceDC, value);
			}
			else
			{
				x = getHuffmanEncodedBits(this.huffmanTable.LuminanceAC, value);
			}
		}
		else
		{
			if (DC)
			{
				x = getHuffmanEncodedBits(this.huffmanTable.ChrominanceDC, value);
			}
			else
			{
				x = getHuffmanEncodedBits(this.huffmanTable.ChrominanceAC, value);
			}
			
		}
		return x;
	}
	
	private String getHuffmanEncodedBits(String[][] table, int value)
	{
		for (int i = 0; i < table.length; i++)
		{
			if (Integer.parseInt(table[i][1]) == value)
			{
				return table[i][0];
			}
		}
		return null;
	}

}

