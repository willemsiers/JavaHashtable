package fast_set_java;

import fast_set_java.FastSet.Bucket;
import fast_set_java.FastSet.Vector;

public class Main {

	public static void main(String[] args)
	{
		System.out.println("starting");
		FastSet s = new FastSet(1, 1, 16, 16);
		
		int TO_INSERT = 16;
		Vector[] data = new Vector[TO_INSERT];
		for(int i = 0; i<TO_INSERT; i++)
		{
			data[i] = s.new Vector();
			data[i].value = "willem"+i;
		}
		
		for(int i = 0; i<data.length; i++)
		{
			boolean found = s.find_or_put(data[i], true); //if false, data is put
			System.out.println("found:" + found + " for "+data[i].toString());
		}

		for(Bucket b : s.buckets)
		{
			System.out.println("bucket:\t"+b);
		}
		for(Vector v : s.data)
		{
			System.out.println("data:\t"+v);
		}
	}
	

}
