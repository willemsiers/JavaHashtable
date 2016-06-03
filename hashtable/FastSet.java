package hashtable;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static other.Logger.*;

public class FastSet implements AbstractFastSet{

	public final Vector[] data;
	public final int size;
	private final int sizeMask;
	private final long indicesBase;
	private boolean cleaned_up = false;

	private final static int LONG_SIZE_BYTES = 8;
	private final static long LONG_MASK_OCCUPIED = 1l << (LONG_SIZE_BYTES * 8 - 1);
	private final static long LONG_MASK_WRITING = 1l << (LONG_SIZE_BYTES * 8 - 2);
	private final static long LONG_MASK_HASH = (~(LONG_MASK_OCCUPIED | LONG_MASK_WRITING));
	private static final long LONG_KEYVALUE_EMPTY = 0;
	private final static int KEY_SIZE_BYTES = LONG_SIZE_BYTES;
	private final Unsafe unsafe;


	public FastSet(int init_size) {
		this.size = init_size;
		this.sizeMask = this.size - 1;

		//TODO: Maybe change size to first power of 2?
		if( (this.size == 0) || ((this.size & (this.size - 1)) != 0))
		{
			throw new IllegalArgumentException("size must be a power of two.");
		}

		this.data = new Vector[this.size];

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

		indicesBase = unsafe.allocateMemory(this.size * KEY_SIZE_BYTES);
		this.clear();
	}

	public void clear() {
		if(cleaned_up){
			throw new RuntimeException("FastSet was already freed");
		}
		this.unsafe.setMemory(indicesBase, this.size * LONG_SIZE_BYTES, (byte) 0);
		for (int i = 0; i < data.length; i++) { //slow, but effective
			data[i] = new Vector();
		}
//        Arrays.fill(data, null);
	}

	public static int stringHash(String s, int iteration) {
		return s.hashCode() * iteration;


		/** See https://github.com/boundary/high-scale-lib/blob/master/src/main/java/org/cliffc/high_scale_lib/NonBlockingHashMap.java
		     h += (h <<  15) ^ 0xffffcd7d;
			 h ^= (h >>> 10);
			 h += (h <<   3);
			 h ^= (h >>>  6);
			 h += (h <<   2) + (h << 14);
			 return h ^ (h >>> 16);
		 */

		// TODO: better hash function
		/**
		 * Based on s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
		 **/
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

	public static boolean isBucketEmpty(long bucketValue) {
		boolean result = (bucketValue & LONG_MASK_OCCUPIED) != LONG_MASK_OCCUPIED;
		return result;
	}

	public static boolean isBucketWriting(long bucketValue) {
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

	public  void removeWritingFlag(int bucketOffsetBytes, int hashValue) {
		//Should be called after setWritingCAS has succeeded, and data was written.
		if (false) {//temp assertion
			long expected = hashValue | LONG_MASK_WRITING | LONG_MASK_OCCUPIED;
			long actual = unsafe.getLong(indicesBase + bucketOffsetBytes);
			if (expected != actual) {
				throw new AssertionError(String.format("Expected value %d != %d\n", expected, actual));
			}
		}

		long newValue = (hashValue | LONG_MASK_OCCUPIED) & (~LONG_MASK_WRITING); //should effectively only remove WRITING flag, compared to the current value stored at the address
		unsafe.putLong(indicesBase + bucketOffsetBytes, newValue);
	}

	/**
	 * @Return true if v was found. false if v was not found, but it was inserted
	 */
	@Override
	public boolean findOrPut(Vector v) {
		if(cleaned_up){
			throw new RuntimeException("FastSet was already freed");
		}
		final int THRESHOLD = 1000; //How many times to rehash before giving up
		final int CACHE_LINE_SIZE = 8;
		int count = 1;
		int h = stringHash(v.value, count);
		final int originalHash = h; //Will be stored in the bucket (indices buffer)
		int lineStart = h & this.sizeMask;   //Where to start probing

		boolean found = false;
		while (count < THRESHOLD) {
			for (int i = lineStart; i < lineStart + CACHE_LINE_SIZE; i++) {
				int bucketOffsetLongs = i & this.sizeMask;
				int bucketOffsetBytes = bucketOffsetLongs * LONG_SIZE_BYTES;
				long bucketValue = unsafe.getLong(indicesBase + bucketOffsetBytes); //TODO: Maybe 'getLong' inside methods instead of passing "old" value around
				if (isBucketEmpty(bucketValue) && setWritingCAS(bucketOffsetBytes, originalHash)) { //TODO: isBucketEmpty call may be unnecessary?
						data[bucketOffsetLongs] = v;
						removeWritingFlag(bucketOffsetBytes, originalHash);
						return false;
				} else // if( !buckets[position].isEmpty())
				{
					//Debug.colissions.incrementAndGet();
					bucketValue = unsafe.getLong(indicesBase + bucketOffsetBytes);
					//TODO: maybe instead of storing and comparing hashes, store and compare the index?
					//Before entering "while(isWriting)", check if hashes are even the same (hash value is already written on the CAS)
					if ( (bucketValue & LONG_MASK_HASH) == (originalHash & LONG_MASK_HASH)) {
						while (isBucketWriting(bucketValue)) {
							bucketValue = unsafe.getLong(indicesBase + bucketOffsetBytes);
							if(!NO_LOGGING){logV("waiting...");}
						}

						if (data[bucketOffsetLongs].equals(v)) { //TODO: Can == be used?
							if(!NO_LOGGING){logV("equal data found");}
							return true;
						} else {
							if(!NO_LOGGING){logV(String.format("Non equal data (%s instead of %s) in bucket (%d (l), %d)", data[bucketOffsetLongs], v, bucketOffsetLongs,
									bucketValue));}
							// else continue probing
						}
					}else{
					}

//					if (data[bucketOffsetLongs] == null) {
//						throw new AssertionError("bucket wasn't empty, but data related to that bucket was null");
//					}
				}
			}

			count++;
			if(!NO_LOGGING){logV("rehashing #" + count);}
			lineStart = stringHash(v.value, count) & this.sizeMask;
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
