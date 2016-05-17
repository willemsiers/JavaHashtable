package fast_hashtable;

import java.util.Arrays;

import fast_hashtable.FastSet.Bucket;
import fast_hashtable.FastSet.Vector;

public class Main {

	public static void main(String[] args)
	{
        final int STATESPACE_SIZE = 256;
		FastSet s = new FastSet(1, 1, STATESPACE_SIZE);
		Vector[] statespace = new Vector[STATESPACE_SIZE];
		for (int i = 0; i < STATESPACE_SIZE; i++) 
		{
			statespace[i] = s.new Vector();
			statespace[i].value = "willem" + i;
		}

		final int NUM_OF_THREADS = 4; //powers of 2
		Thread[] threads = new Thread[NUM_OF_THREADS];

		int workLeft = STATESPACE_SIZE;
		for (int threadNo = 0; threadNo < NUM_OF_THREADS; threadNo++) 
		{
			
			class Work implements Runnable {

				public Vector[] unprocessed = null;
				public int workSize = 0;

				public Work(Vector[] data)
				{
					this.unprocessed = data;
					this.workSize = data.length;
				}

				@Override
				public void run()
				{
					//System.out.println("starting thread-"+Thread.currentThread().getName());

					for (int i = 0; i < workSize; i++) 
		{
						boolean found = s.find_or_put(unprocessed[i], true); // if false, data is put
						//System.out.println("found:" + found + " for " + unprocessed[i].toString());
					}
				}
				
			}

			int from = STATESPACE_SIZE - workLeft;
			int to = from + STATESPACE_SIZE / NUM_OF_THREADS;
			Work work = new Work(Arrays.copyOfRange(statespace, from, to));

			Thread worker = new Thread(work);
			worker.setName("worker-"+threadNo);
			threads[threadNo] = worker;
			workLeft -= (to-from);
		}
		if(workLeft != 0)
		{
			throw new RuntimeException("workLeft != 0, but "+workLeft);
		}
		
		for (Thread worker : threads) 
		{
			worker.start();
		}

		for (Thread thread : threads) 
		{
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		for (Bucket b : s.buckets) 
		{
			//System.out.println("bucket:\t" + b);
		}
		for (int i = 0; i<s.data.length; i++) 
		{
			//System.out.println("data "+i+":\t" + s.data[i]);
		}
		
		
//		long a = Long.MAX_VALUE;
//		System.out.println((int)a);
        
        
	}
}