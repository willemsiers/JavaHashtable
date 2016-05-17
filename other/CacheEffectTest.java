package other;

import org.junit.Test;

public class CacheEffectTest {

	final static int SIZE = 1024*100;
	final static long NUM_OPERATIONS = 5000*1024*1024;
	
	@Test
	public void cacheTest1()
	{
		int[] arr = new int[SIZE];
		int index = 0;
		final int STEP = 33;
		
		for(long op = 0; op<NUM_OPERATIONS; op++)
		{
			index = (index+STEP)%SIZE;
			arr[index] *= 2;
		}
	}

	@Test
	public void cacheTest2()
	{
		int[] arr = new int[SIZE];
		int index = 0;
		final int STEP = 1;
		
		for(long op = 0; op<NUM_OPERATIONS; op++)
		{
			index = (index+STEP)%SIZE;
			arr[index] *= 2;
		}
	}
}
