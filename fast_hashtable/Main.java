package fast_hashtable;

import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fast_hashtable.FastSet.Bucket;
import fast_hashtable.FastSet.Vector;

import static other.Logger.logV;

public class Main {

	public static void main(String[] args)
	{
        final int STATESPACE_SIZE = 8 * 1024;
        final int FREE_FACTOR = 8;
		FastSet s = new FastSet(1, 1, STATESPACE_SIZE * FREE_FACTOR);
		Vector[] statespace = new Vector[STATESPACE_SIZE];
		for (int i = 0; i < STATESPACE_SIZE; i++) 
		{
			statespace[i] = s.new Vector();
			statespace[i].value = "" + i;
		}

		final int NUM_OF_THREADS = 2; //powers of 2
		CyclicBarrier barrier = new CyclicBarrier(NUM_OF_THREADS + 1);
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
					try {
						barrier.await(10, TimeUnit.SECONDS);
					} catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
						e.printStackTrace();
					}
					logV("starting thread-"+Thread.currentThread().getName());

					for (int i = 0; i < workSize; i++) 
					{
						boolean found = s.find_or_put(unprocessed[i], true); // if false, data is put
						logV("found:" + found + " for " + unprocessed[i].toString());
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
		
		try {
			barrier.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException | BrokenBarrierException | TimeoutException e1) {
			e1.printStackTrace();
		}
		
		long startMs = System.currentTimeMillis();

		for (Thread thread : threads) 
		{
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		long endMs = System.currentTimeMillis();
		long diffMs = endMs - startMs;
		double diffSeconds = diffMs / 1000f;
		
		for (Bucket b : s.buckets) 
		{
			logV("bucket:\t" + b);
		}
		int insertedCounter = 0;
		for (int i = 0; i<s.data.length; i++) 
		{
			if(s.data[i] != null)
			{
				insertedCounter ++ ;
			}
			logV("data "+i+":\t" + s.data[i]);
		}

		System.out.printf("Inserting %d vectors took %.4f seconds on %d threads. %d/%d data[] places were filled.", STATESPACE_SIZE, diffSeconds, NUM_OF_THREADS, insertedCounter, s.data.length);
	}
}