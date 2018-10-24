package test.java.Util;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import test.java.JPEG.TestBitData;
import test.java.JPEG.TestDCTMatrix;
import test.java.JPEG.TestHuffmanDecode;
import test.java.JPEG.TestHuffmanEncode;
import test.java.JPEG.TestImageData;
import test.java.steganography.TestChangeEmbeder;

@RunWith(Suite.class)
@SuiteClasses({ TestBitConverter.class, TestHuffmanDecode.class, TestDCTMatrix.class,
	TestImageData.class, TestHuffmanEncode.class, TestBitData.class, TestPathgenerator.class,
	TestChangeEmbeder.class})
public class AllTests {

}
