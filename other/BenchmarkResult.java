package other;

public class BenchmarkResult {
	public static String system;
	public static String mapImplementation;
	public final int run;
	public final int statespace_size;
	public final double diffSeconds;
	public final int num_of_threads;
	public final int insertedCounter;
	public double speedup;
	public double relSpeedup;

	public BenchmarkResult(int run, int statespace_size, double diffSeconds, int num_of_threads, int insertedCounter) {
		this.run = run;
		this.statespace_size = statespace_size;
		this.diffSeconds = diffSeconds;
		this.num_of_threads = num_of_threads;
		this.insertedCounter = insertedCounter;
	}

	@Override
	public String toString() {
		return String.format("run#=%d\tthreadCnt=%d\ttime=%.4f\tstatespace=%d\tinsertedCnt=%d\tspeedup=%.3f\trelSpeedup=%.3f", run, num_of_threads, diffSeconds, statespace_size, insertedCounter, speedup, relSpeedup);
	}
}
