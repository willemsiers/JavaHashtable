package hashtable;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static other.Logger.*;

public class FastSet implements AbstractFastSet{

	private boolean cleaned_up = false;

	private static final long LONG_KEYVALUE_EMPTY = 0;
	final int LONG_SIZE_BYTES = 8;
	final long LONG_MASK_OCCUPIED = 1l << (LONG_SIZE_BYTES * 8 - 1);
	final long LONG_MASK_WRITING = 1l << (LONG_SIZE_BYTES * 8 - 2);
	final int KEY_SIZE_BYTES = LONG_SIZE_BYTES;
	public final Unsafe unsafe;

	public final long indicesBase;
	public final Vector[] data;
	int size = 0;

	// fset_t * fset_create (size_t key_length, size_t data_length, size_t init_size, size_t max_size)
	public FastSet(int key_length, int init_size) {
		this.data = new Vector[init_size];
		this.size = init_size;

		// todo
		// dbs->data = RTalign (CACHE_LINE_SIZE, dbs->total_length * dbs->size_max);
		// dbs->todo_data = RTalign (CACHE_LINE_SIZE, dbs->total_length * dbs->size_max);
		Unsafe defUnsafe;
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			defUnsafe = (Unsafe) f.get(null);
		} catch (NoSuchFieldException e) {
			defUnsafe = null;
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			defUnsafe = null;
			e.printStackTrace();
		}
		this.unsafe = defUnsafe;

		if (this.unsafe == null) {
			throw new NullPointerException("\"Unsafe\" isn't initialized");
		}

		indicesBase = unsafe.allocateMemory(init_size * KEY_SIZE_BYTES);
		this.clear();
	}

	public void clear() {
		if(cleaned_up){
			throw new RuntimeException("FastSet was already freed");
		}
		this.unsafe.setMemory(indicesBase, this.size * LONG_SIZE_BYTES, (byte) 0);
		for (int i = 0; i < data.length; i++) {
			data[i] = new Vector();
		}
//        Arrays.fill(data, null);
	}

	/**
	 * Based on s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
	 **/
	public int stringHash(String s, int iteration) {
		return s.hashCode() * iteration;

		// TODO: better hash function
//		int hash = 0;
//		int prime = Primes.primes[iteration + 10];
//		int length = s.length();
//		byte[] chars = s.toCharArray();
//		for (int i = 0; i < length; i++) {
//			long h = (long) (chars[i] * Math.pow((double) prime, (double) length - 1));
//			hash += (int) h;
//		}
//		if(iteration == 1){
//			System.out.println(hash);
//		}
//		return hash;
	}

	public boolean isBucketEmpty(long bucketValue) {
		boolean result = (bucketValue & LONG_MASK_OCCUPIED) != LONG_MASK_OCCUPIED;
		return result;
	}

	public boolean isBucketWriting(long bucketValue) {
		boolean result = (bucketValue & LONG_MASK_WRITING) == LONG_MASK_WRITING;
		return result;
	}

	public boolean setWritingCAS(long bucketOffsetBytes, long hashValue) {
		boolean success = false;
		// <h,WRITE>
		long newValue = hashValue | LONG_MASK_WRITING | LONG_MASK_OCCUPIED;
		success = this.unsafe.compareAndSwapLong(null, indicesBase + bucketOffsetBytes, LONG_KEYVALUE_EMPTY, newValue);
		return success;
	}

	public void removeWritingFlag(int bucketOffsetBytes, int hashValue) {
		if (true) {//temp assertion
			long expected = hashValue | LONG_MASK_WRITING | LONG_MASK_OCCUPIED;
			long actual = unsafe.getLong(indicesBase + bucketOffsetBytes);
			if (expected != actual) {
				throw new AssertionError(String.format("Expected value %d != %d\n", expected, actual));
			}
		}

		long newValue = hashValue | LONG_MASK_OCCUPIED; //should effectively only remove WRITING flag, compared to the current value stored at the address
		unsafe.putLong(indicesBase + bucketOffsetBytes, newValue);
	}

	@Override
	public boolean findOrPut(Vector v, boolean insertAbsent) {
		if(cleaned_up){
			throw new RuntimeException("FastSet was already freed");
		}
		int count = 1;
		int h = Math.abs(stringHash(v.value, count));
		int lineStart = h % size;
		final int THRESHOLD = 1000;
		final int CACHE_LINE_SIZE = 8;

		boolean found = false;
		while (count < THRESHOLD) {
			for (int i = lineStart; i < lineStart + CACHE_LINE_SIZE; i++) {
				int bucketOffsetLongs = i % this.size;
				int bucketOffsetBytes = bucketOffsetLongs * LONG_SIZE_BYTES;
				long bucketValue = unsafe.getLong(indicesBase + bucketOffsetBytes); //TODO: Maybe 'getLong' inside methods instead of passing "old" value around
				if (isBucketEmpty(bucketValue)) {
					if (setWritingCAS(bucketOffsetBytes, h)) {
						data[bucketOffsetLongs] = v;
						removeWritingFlag(bucketOffsetBytes, h);
						return false;
					} else {
						//cas failed, aka bucket != empty. continue.
					}
				} else // if( !buckets[position].isEmpty())
				{
					Debug.colissions.incrementAndGet();
					bucketValue = unsafe.getLong(indicesBase + bucketOffsetBytes);
					while (isBucketWriting(bucketValue)) {
						bucketValue = unsafe.getLong(indicesBase + bucketOffsetBytes);
						//TODO: compare the hashes if they are equal, only then wait (to check if data is equal when writing is finished)
						if(!NO_LOGGING){logV("waiting...");}
					}

					if (data[bucketOffsetLongs] == null) {
						throw new AssertionError("bucket wasn't empty, but data related to that bucket was null");
					}
					if (data[bucketOffsetLongs].equals(v)) {
						if(!NO_LOGGING){logV("equal data found");}
						return true;
					} else {
						if(!NO_LOGGING){logV(String.format("Non equal data (%s instead of %s) in bucket (%d (l), %d)", data[bucketOffsetLongs], v, bucketOffsetLongs,
								bucketValue));}
						// else continue probing
					}
				}
			}

			count++;
			if(!NO_LOGGING){logV("rehashing #" + count);}
			lineStart = Math.abs(stringHash(v.value, count) % size);
		}

		logE(String.format("Unable to insert \"%s\" (Threshold exceeded)", v.toString()));
		return found;
	}

	@Override
	public Vector[] getData() {
		return data;
	}

	@Override
	public void cleanup()
	{
		if(cleaned_up){
			throw new RuntimeException("FastSet was already freed");
		}
		cleaned_up = true;
		unsafe.freeMemory(indicesBase);
	}

	@Override
	public String toString() {
		return "FastSet";
	}
}
