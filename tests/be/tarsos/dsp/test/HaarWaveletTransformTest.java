package be.tarsos.dsp.test;

import static org.junit.Assert.*;

import org.junit.Test;

import be.tarsos.dsp.wavelet.HaarWaveletTransform;

public class HaarWaveletTransformTest {

	@Test
	public void testTransform() {
		HaarWaveletTransform ht = new HaarWaveletTransform();
		float[] data = {5,1,2,8};
		ht.transform(data);
		float[] expected = {4,2,-1,-3};
		assertArrayEquals(expected,data);
		
		float[] otherData = {3,1,0,4,8,6,9,9};
		ht.transform(otherData);
		float[] expectedResult = {5,1,0,-2,-3,1,-1,0};
		assertArrayEquals(expectedResult,otherData);
	}
	
	@Test
	public void testInverseTransform() {
		HaarWaveletTransform ht = new HaarWaveletTransform();
		float[] data = {4,2,-1,-3};
		ht.inverseTransform(data);
		float[] expected = {5,1,2,8};
		assertArrayEquals(expected,data);
		
		float[] otherData = {5,1,0,-2,-3,1,-1,0};
		ht.inverseTransform(otherData);
		float[] expectedResult = {3,1,0,4,8,6,9,9};
		assertArrayEquals(expectedResult,otherData);
	}

	private void assertArrayEquals(float[] expecteds, float[] actuals) {
		for(int i=0;i<expecteds.length;i++){
			assertEquals(expecteds[i],actuals[i],0.0001);
		}
	}

}
