package hashtable;

import other.BenchmarkResult;
import other.Logger;

import javax.swing.text.html.HTMLWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static other.Logger.logE;
import static other.Logger.logV;

public class Main {

	static final int STATESPACE_SIZE = 1 * 1024 * 1024;
	static final int FREE_FACTOR = 4;
	static final int[] THREADCOUNTS = {1,1,2,4,8};

	public enum MapType {
		FASTSET,
		CONCURRENT_HASHMAP,
		HASHTABLE
	};

	public static void main(String[] args) {
		printMemoryMax();
		performBenchmark(MapType.CONCURRENT_HASHMAP);
		performBenchmark(MapType.HASHTABLE);
		performBenchmark(MapType.FASTSET);
	}

	public static void performBenchmark(final MapType MAP_TYPE) {

		BenchmarkResult[] benchmarkResults = new BenchmarkResult[THREADCOUNTS.length];
																																																																															
																																																																																	String hostname = getHostname();
		BenchmarkResult.system = hostname;

		for (int run = 0; run < THREADCOUNTS.length; run++) {
			final AbstractFastSet s;

			switch (MAP_TYPE){
				case FASTSET:
					s = new FastSet(1, STATESPACE_SIZE * FREE_FACTOR);
					break;
				case CONCURRENT_HASHMAP:
					s = new HashtableWrapper(new ConcurrentHashMap<Vector,Vector>(STATESPACE_SIZE * FREE_FACTOR));
					break;
				case HASHTABLE:
					s = new HashtableWrapper(new Hashtable<Vector,Vector>(STATESPACE_SIZE * FREE_FACTOR));
					break;
				default:
					throw new IllegalArgumentException("No such map type");
			}

			BenchmarkResult.mapImplementation = s.toString();
			Vector[] statespace = new Vector[STATESPACE_SIZE];
			for (int i = 0; i < STATESPACE_SIZE; i++) {
				statespace[i] = new Vector();
				statespace[i].value = "" + i;
			}

			final int NUM_OF_THREADS = THREADCOUNTS[run]; //powers of 2
			System.out.printf("Starting run #%d on %d threads\n", run, NUM_OF_THREADS);
			final CyclicBarrier barrier = new CyclicBarrier(NUM_OF_THREADS + 1);
			final Thread[] threads = new Thread[NUM_OF_THREADS];
			int workLeft = STATESPACE_SIZE;
			for (int threadNo = 0; threadNo < NUM_OF_THREADS; threadNo++) {
				class Work implements Runnable {

					public Vector[] unprocessed = null;
					public int workSize = 0;

					public Work(Vector[] data) {
						this.unprocessed = data;
						this.workSize = data.length;
					}

					@Override
					public void run() {
						try {
							barrier.await(10, TimeUnit.SECONDS);
						} catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
							e.printStackTrace();
						}
						logV("starting thread-" + Thread.currentThread().getName());

						for (int i = 0; i < workSize; i++) {
							boolean found = s.findOrPut(unprocessed[i], true); // if false, data is put
							if(!Logger.NO_LOGGING) {
								logV("found:" + found + " for " + unprocessed[i].toString());
							}
						}
					}
				}

				int from = STATESPACE_SIZE - workLeft;
				int to = from + STATESPACE_SIZE / NUM_OF_THREADS;
				Work work = new Work(Arrays.copyOfRange(statespace, from, to));

				Thread worker = new Thread(work);
				worker.setName("worker-" + threadNo);
				threads[threadNo] = worker;
				workLeft -= (to - from);
			}
			if (workLeft != 0) {
				throw new RuntimeException("workLeft != 0, but " + workLeft);
			}

			for (Thread worker : threads) {
				worker.start();
			}

			System.gc();
			try {
				Thread.sleep(200);
				barrier.await(10, TimeUnit.SECONDS);
			} catch (InterruptedException | BrokenBarrierException | TimeoutException e1) {
				e1.printStackTrace();
			}

			long startMs = System.currentTimeMillis();

			for (Thread thread : threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			long endMs = System.currentTimeMillis();
			long diffMs = endMs - startMs;
			double diffSeconds = diffMs / 1000f;

			int insertedCounter = 0;
			Vector[] resultData = s.getData();
			for (int i = 0; i < resultData.length; i++) {
				if (resultData[i].value != null) {
					insertedCounter++;
				}
				if(!Logger.NO_LOGGING) {
					logV("data " + i + ":\t" + resultData[i]);
				}
			}

			printMemoryStatistics();
			s.cleanup();

			benchmarkResults[run] = new BenchmarkResult(run, STATESPACE_SIZE, diffSeconds, NUM_OF_THREADS, insertedCounter);
			benchmarkResults[run].speedup = benchmarkResults[0].diffSeconds / benchmarkResults[run].diffSeconds;
			benchmarkResults[run].relSpeedup = benchmarkResults[run].speedup / benchmarkResults[run].num_of_threads;
//			System.out.printf("Inserting %d vectors took %.4f seconds on %d threads. %d/%d data[] places were filled.\n", STATESPACE_SIZE, diffSeconds, NUM_OF_THREADS, insertedCounter, resultData.length);
		}

		System.out.println("Colisssions: "+ Debug.colissions);
		Debug.colissions.set(0);
		System.out.printf("Benchmark results for %s at %s (System: %s)\n", BenchmarkResult.mapImplementation, new SimpleDateFormat("HH:mm yyyy-MM-dd").format(new Date()), BenchmarkResult.system);
		System.out.println("Free-factor: " + FREE_FACTOR);
		for (BenchmarkResult result : benchmarkResults) {
			System.out.println(result.toString());
		}
	}

	public static String getHostname() {
		String hostname;
		try {
			Process proc = Runtime.getRuntime().exec("hostname");
			try (InputStream stream = proc.getInputStream()) {
				try (Scanner s = new Scanner(stream).useDelimiter("\\A")) {
					hostname = s.hasNext() ? s.next() : "unknown";
				}
			}
		} catch (IOException e) {
			hostname = "unknown";
			e.printStackTrace();
		}
		return hostname;
	}


	public static void printMemoryStatistics()
	{
		final int MB = 1024*1024;
		Runtime rt = Runtime.getRuntime();
		System.out.printf("Free:%dMB\tTotal%dMB\tMax%dMB\n", rt.freeMemory() / MB, rt.totalMemory() / MB, rt.maxMemory() / MB);
	}

	public static void printMemoryMax()
	{
		Runtime rt = Runtime.getRuntime();
		System.out.printf("Max memory: %d MB\n",rt.maxMemory() / (1024*1024));
	}
}