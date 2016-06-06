package hashtable;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import other.BenchmarkResult;
import other.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static other.Logger.logV;

//NOTE: run with jvm arguments to increase heap size if necessary, for example: -Xms512m -Xmx6g
public class Main {

	static final float STATESPACE_OVERLAP =  1f;
	//FREE_FACTOR: how much to over-allocate in the hashtable (determines load-factor)
	static final int[] FREE_FACTORS = {2,4};
	//Total insertion attempts will be STATESPACE_SIZE * STATESPACE_OVERLAP, where STATESPACE_SIZE insertions have "unique" data
	static final int STATESPACE_SIZE = (int) Math.pow(2, 14) / FREE_FACTORS[FREE_FACTORS.length-1]; //assuming FREE_FACTORS has largest value at the end
	//THREADCOUNTS: for each entry a benchmark will be performed using this many threads
	static final int[] THREADCOUNTS = {1,1,1,1,2,2,2,2,4,4,4,4,8,8,8,8,16,16,16,16};

	public enum MapType {
		FASTSET,
		CONCURRENT_HASHMAP,
		NONBLOCKING_HASHMAP,
		HASHTABLE,
		LOCKLESS_HASHTABLE
	};

	public static void main(String[] args) throws InterruptedException {
		printMemoryMax();
		BenchmarkResult.system = getHostname();
		final ArrayList<BenchmarkResult[]> results = new ArrayList<BenchmarkResult[]>();

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				System.out.println("Shutting down...");
				printResults(results);
				System.out.println("Program shut down unfinished");
			}
		});

		try {
			for (int freeFactor : FREE_FACTORS) {
				System.out.println("Starting benchmarks with FREE_FACTOR: " + freeFactor);
				results.add(performBenchmark(MapType.LOCKLESS_HASHTABLE, freeFactor));
				results.add(performBenchmark(MapType.CONCURRENT_HASHMAP, freeFactor));
				results.add(performBenchmark(MapType.FASTSET, freeFactor));
				results.add(performBenchmark(MapType.HASHTABLE, freeFactor));
				results.add(performBenchmark(MapType.NONBLOCKING_HASHMAP, freeFactor));
			}
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			printResults(results);
		}
	}

	private static void printResults(ArrayList<BenchmarkResult[]> results) {
		for (BenchmarkResult[] resultSet : results) {
			for (BenchmarkResult result : resultSet) {
				System.out.printf("Benchmark results for %s at %s (System: %s), Free-factor: %d, Overlap: %f\n", result.mapImplementation, new SimpleDateFormat("HH:mm yyyy-MM-dd").format(new Date()), BenchmarkResult.system, result.freeFactor, STATESPACE_OVERLAP);

				//first determine value that has speedup of exactly 1, assumes THREADCOUNTS is incremental
				int lastIndexLowestThreaded = 0;
				int lowestThreadCount = THREADCOUNTS[0];
				while ((lastIndexLowestThreaded + 1) < THREADCOUNTS.length && THREADCOUNTS[lastIndexLowestThreaded + 1] == lowestThreadCount) {
					lastIndexLowestThreaded++;
				}

				System.out.println();
				if (result != null) {
					if (resultSet[lastIndexLowestThreaded] != null) {
						result.speedup = resultSet[lastIndexLowestThreaded].diffSeconds / result.diffSeconds;
						result.relSpeedup = result.speedup / result.num_of_threads;
					}
					System.out.println(result.toString());
				}
			}
		}
	}

	public static BenchmarkResult[] performBenchmark(final MapType MAP_TYPE, final int FREE_FACTOR) {

		BenchmarkResult[] benchmarkResults = new BenchmarkResult[THREADCOUNTS.length];
			for (int run = 0; run < THREADCOUNTS.length; run++) {
				final AbstractFastSet s; //Note: virtual dispatch appears to be slightly slower
				switch (MAP_TYPE) {
					case FASTSET:
						s = new FastSet(STATESPACE_SIZE * FREE_FACTOR);
						break;
					case CONCURRENT_HASHMAP:
						s = new HashtableWrapper(new ConcurrentHashMap<Vector, Vector>(STATESPACE_SIZE * FREE_FACTOR));
						break;
					case HASHTABLE:
						s = new HashtableWrapper(new Hashtable<Vector, Vector>(STATESPACE_SIZE * FREE_FACTOR));
						break;
					case NONBLOCKING_HASHMAP:
						s = new HashtableWrapper(new NonBlockingHashMap<Vector, Vector>(STATESPACE_SIZE * FREE_FACTOR));
						break;
					case LOCKLESS_HASHTABLE:
						s = new HashtableWrapper(new nl.utwente.csc.fmt.locklesshashtable.generalhashtable.NonBlockingHashMap<Vector,Vector>(STATESPACE_SIZE * FREE_FACTOR));
						break;
					default:
						throw new IllegalArgumentException("No such map type");
				}

				try{
					final Vector[] statespace = new Vector[STATESPACE_SIZE];
					for (int i = 0; i < STATESPACE_SIZE; i++) {
						statespace[i] = new Vector();
						statespace[i].value = "w" + i;
					}

					final int NUM_OF_THREADS = THREADCOUNTS[run]; //powers of 2
					System.out.printf("Starting run #%d on %d threads (System: %s)\n", run, NUM_OF_THREADS, BenchmarkResult.system);
					final CyclicBarrier barrier = new CyclicBarrier(NUM_OF_THREADS + 1);
					final Thread[] threads = new Thread[NUM_OF_THREADS];
					int from = 0;
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
									barrier.await(360, TimeUnit.SECONDS);
								} catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
									e.printStackTrace();
								}
								logV("starting thread-" + Thread.currentThread().getName());

								for (int i = 0; i < workSize; i++) {
									boolean found = s.findOrPut(unprocessed[i]); // if false, data is put
//									if (!Logger.NO_LOGGING) {
//										logV("found:" + found + " for " + unprocessed[i].toString());
//									}
								}
							}
						}

						from %= STATESPACE_SIZE;
						final int toTake = (int) ((STATESPACE_SIZE / NUM_OF_THREADS) + (STATESPACE_OVERLAP * STATESPACE_SIZE));
						Vector[] vs = new Vector[toTake];
						from = take(statespace,from,toTake,vs);
						final Work work = new Work(vs);

						Thread worker = new Thread(work);
						worker.setName("worker-" + threadNo);
						threads[threadNo] = worker;
					}

					for (Thread worker : threads) {
						worker.start();
					}

					System.gc();
					try {
						Thread.sleep(1000);
						barrier.await(360, TimeUnit.SECONDS);
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
					HashSet<Vector> tmpSet = new HashSet<Vector>(resultData.length);
					for (int i = 0; i < resultData.length; i++) {
						if (resultData[i].value != null) {
							insertedCounter++;
							boolean inserted = tmpSet.add(resultData[i]);
							if(!inserted){
								System.out.printf("Element %d with value %s exists!\n", i, resultData[i]);
							}
						}
						if (!Logger.NO_LOGGING) {
							logV("data " + i + ":\t" + resultData[i]);
						}
					}

//				printMemoryStatistics();
					benchmarkResults[run] = new BenchmarkResult(run, STATESPACE_SIZE, diffSeconds, NUM_OF_THREADS, insertedCounter);
					benchmarkResults[run].mapImplementation = s.toString();
					benchmarkResults[run].freeFactor = FREE_FACTOR;
					if (insertedCounter != STATESPACE_SIZE) {
						System.out.println("insertedCounter == "+insertedCounter + " STATESPACE_SIZE=="+STATESPACE_SIZE);
						throw new AssertionError("insertedCounter != STATESPACE_SIZE");
					}
				}catch(Exception e) {
					e.printStackTrace();
				}finally{
					if(s != null) {
						s.cleanup();
					}
				}
			}

		return benchmarkResults;
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

		hostname = hostname.split("\n")[0];
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

	private static int take(Vector[] src, final int startIndex, int n, Vector[] dst){
		if(dst.length != n) {
			throw new IllegalArgumentException("dst[] is not large enough");
		}
		int srcPtr = startIndex;
		int taken = 0;
		while(taken != n){
//			int toTake = Math.min(src.length, n-taken);
			//TODO: fix length
			int toTake = Math.min(1, n-taken);
			System.arraycopy(src, srcPtr, dst, taken, toTake);	// (src, srcPos, dst, dstPos,length)
			taken += toTake;
			srcPtr = (srcPtr + toTake) % src.length;
		}
		return srcPtr;
	}
}