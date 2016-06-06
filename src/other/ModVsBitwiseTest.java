package other;

import org.junit.Test;

import static other.CacheEffectTest.NUM_OPERATIONS;

//Obviously not a GOOD microbenchmark.. but bitwise seems considerably faster (9ms vs 15617ms)
public class ModVsBitwiseTest {

	final int NUM_OPERATIONS = Integer.MAX_VALUE;
	final int REPEAT = 8;

	@Test
	public void modTest() {
//		final int SIZE = 1024;
		int a = 0;
		for(int i = 0; i<REPEAT; i++) {
			for (int op = 0; op < NUM_OPERATIONS; op++) {
				a = op % 1024;
			}
		}
		System.out.println(a);
	}

	@Test
	public void bitwiseTest() {
		final int SIZE = 1024;
		final int SIZE_MASK = SIZE - 1;
		int a = 0;
		for(int i = 0; i<REPEAT; i++) {
			for (int op = 0; op < NUM_OPERATIONS; op++) {
				a = op & SIZE_MASK;
			}
		}
		System.out.println(a);
	}
}