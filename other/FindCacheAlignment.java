package other;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class FindCacheAlignment {

	public static void main(String[] args) {
		Unsafe unsafe;
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = (Unsafe) f.get(null);
		} catch (NoSuchFieldException e) {
			unsafe = null;
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			unsafe = null;
			e.printStackTrace();
		}

		final int SIZE_LONG = 8;

		long test = 0;
		final int SIZE = 16;
		final int RUN_SIZE = 8;

		final long NUM_OF_RUNS = 8l*1024*1024l*1024l;
		long indicesBase = unsafe.allocateMemory(SIZE * SIZE_LONG);

		final int startNumber = 2;
		for(int a = 0; a<5; a++) {
			for (int offsetOF = startNumber; offsetOF < SIZE - RUN_SIZE + startNumber; offsetOF++) {
				int offset = offsetOF % RUN_SIZE;
				long start = System.currentTimeMillis();
				for (long run = 0; run < offset + NUM_OF_RUNS; run++) {
					for (int i = offset; i < offset + RUN_SIZE; i++) {
						unsafe.getLong(indicesBase + i * SIZE_LONG);
					}
				}
				System.out.printf("offset %d \t= %dms\n", offset, System.currentTimeMillis() - start);
			}

		}

		System.out.println(test);
		unsafe.freeMemory(indicesBase);
	}
}