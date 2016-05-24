package fast_hashtable;

import java.util.concurrent.atomic.AtomicInteger;

import static other.Logger.*;

public class FastSet {

	
	public Bucket[]	buckets = null;
	public Vector[] data = null;
	int key_length = 0;
	int data_length = 0;
	int total_length = 0;
	int size = 0;

	/**
	 * Vector = State data
	 */
	public  class Vector{
		public String value = null;
		
		@Override
		public String toString()
		{
			return value;
		}
		
		@Override
		public int hashCode()
		{
			//note: Object.hashCode is non-deterministic (based on memory address), String.hashCode is.
			return Math.abs(value.hashCode());
		}
	}
	
	public class Bucket{
		//value of a Bucket is the data's hash
		
		private static final int VALUE_EMPTY = 0;
		//todo: look into atomicreference classes
		public AtomicInteger value = new AtomicInteger(VALUE_EMPTY);
		//public State state = State.Empty;
		//int vInt = Integer.parseUnsignedInt("4294967295");
		private int mask_writing = 1<<31;
		
		public boolean isEmpty()
		{
			return value.get() == VALUE_EMPTY;
		}

		public boolean isWriting()
		{
			boolean writeFlag = (value.get() >> 31) == 1;
			return writeFlag;
		}

		public boolean setWritingCAS(int hash)
		{
			boolean success = false;
			// <h,WRITE>
			int newValue = hash | mask_writing;
			success = this.value.compareAndSet(VALUE_EMPTY, newValue);
			return success;
		}

		public void setDone(int hash)
		{
			int newValue = hash & (~mask_writing);
			value.set(newValue);
		}
		
		@Override
		public String toString()
		{
			int value = this.value.get();
			String result = "bckt_"+(value == VALUE_EMPTY ? "empty" : String.format("%d (%d)", getNumerical(), getNumerical()%size ));
			return result;
		}
		
		int getNumerical()
		{
			return (value.get() & (~mask_writing));
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if(obj instanceof Bucket)
			{
				return ((Bucket) obj).value.get() == this.value.get();
			}
			else {
				return false;
			}
		}
	}
	
	//fset_t * fset_create (size_t key_length, size_t data_length, size_t init_size, size_t max_size)
	public FastSet(int key_length, int data_length, int init_size)
	{
		this.buckets = new Bucket[init_size];
		this.data = new Vector[init_size];
		this.size = init_size;
		this.key_length = key_length;
		this.data_length = data_length;
		this.total_length = key_length + data_length;
		
		for(int i =  0; i< this.size; i++)
		{
			buckets[i] = new Bucket();
		}


		//todo
		// dbs->data = RTalign (CACHE_LINE_SIZE, dbs->total_length * dbs->size_max);
		// dbs->todo_data = RTalign (CACHE_LINE_SIZE, dbs->total_length * dbs->size_max);

	}
	
    /**Based on s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]**/
    public int stringHash(String s, int iteration) 
    {
    	//TODO: better hash function
        int hash = 0;
        int prime = Primes.primes[iteration + 10];
        int length = s.length();
        char[] chars = s.toCharArray();
		for (int i = 0; i < length; i++) 
		{
			long h = (long) (chars[i] * Math.pow((double)prime , (double) length-1));
			hash += (int) h;
		}
        return hash;
    }

	
	boolean
	find_or_put(Vector v, boolean insertAbsent)
	{
		int count = 1;
		int h = Math.abs(stringHash(v.value, count));
		//logV("Putting: "+h);
		int lineStart = h % size;
		final int THRESHOLD = 1000;
		final int CACHE_LINE_SIZE = 4;
		
		boolean found = false;
		while(count < THRESHOLD)
		{
			for (int i = lineStart; i < lineStart+CACHE_LINE_SIZE; i++) 
			{
				int position = i % this.size;
				if(buckets[position].isEmpty()){
					if(buckets[position].setWritingCAS(h))
					{
						data[position] = v;
						buckets[position].setDone(h);
						return false;
					}else
					{
						
					}
				} else // if( !buckets[position].isEmpty())
				{
					while(buckets[position].isWriting()){
						logV("waiting...");
						//wait...
                   	}
					if (data[position].equals(v)) 
		{
						logV("equal data found");
						return true;
					}else
					{
						logV(String.format("Non equal data (%s instead of %s) in bucket (%d, %s)", data[position], v, position, buckets[position]));
						// else continue probing
					}
				}
			}

			count++;
			logV("rehashing");
			lineStart = Math.abs(stringHash(v.value, count) % size);
		}
		
		logE(String.format("Unable to insert \"%s\" (Threshold exceeded)", v.toString()));
		return found;
	}
}
