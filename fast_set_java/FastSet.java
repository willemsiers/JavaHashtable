package fast_set_java;

import java.util.concurrent.atomic.AtomicInteger;
public class FastSet {

	final int LOG_LEVEL = 2;
	
	final int LOG_VERBOSE = 2;
	final int LOG_ERROR = 1;

	public void logV(String msg)
	{
			if(LOG_LEVEL >= LOG_VERBOSE)
			{
				System.out.println(msg);
			}
	}
	
	public void logE(String msg)
	{
			if(LOG_LEVEL >= LOG_ERROR)
			{
				System.out.println(msg);
			}
	}

	public Bucket[] 	buckets = null;
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
	public FastSet(int key_length, int data_length, int init_size, int max_size)
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
	
	int hash(Vector v, int count)
	{
		double result = v.hashCode();
		for(int i = 1; i<count; i++)
		{
			if (result <= 1) {
				throw new NullPointerException("Vector hash <=1");
			}
			result = Math.pow(result, 2);
		}
		return (int)result;// & (key_length * 32);
	}
	
	boolean
	find_or_put(Vector v, boolean insertAbsent)
	{
		int count = 1;
		int h = hash(v, count);
		//logV("Putting: "+h);
		int lineStart = h % size;
		final int THRESHOLD = 4;
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
					if (data[position].equals(v)) {
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
			lineStart = hash(v, count) % size;
		}
		
		logE(String.format("Unable to insert \"%s\" (Threshold exceeded)", v.toString()));
		return found;
	}
}
