package other;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BenchmarkResult {
	public static String system;
	public String mapImplementation;
	public int freeFactor;
	public final int run;
	public final int statespace_size;
	public final double diffSeconds;
	public final int num_of_threads;
	public final int insertedCounter;
	public final float overlap;
	public double speedup;

	public BenchmarkResult(int run, int statespace_size, double diffSeconds, int num_of_threads, int insertedCounter, float overlap) {
		this.run = run;
		this.statespace_size = statespace_size;
		this.diffSeconds = diffSeconds;
		this.num_of_threads = num_of_threads;
		this.insertedCounter = insertedCounter;
		this.overlap = overlap;
	}

	@Override
	public String toString() {
		String dateString = new SimpleDateFormat("HH:mm yyyy-MM-dd").format(new Date());
		return String.format(
				"type=%s" +
				" run#=%d" +
				" threads=%d" +
				" time=%.4f" +
				" statespace=%d" +
				" freefactor=%d" +
				" inserted=%d" +
				" speedup=%.3f" +
				" date=%s" +
				" host=%s" +
				" overlap=%.1f",
				mapImplementation, run, num_of_threads, diffSeconds, statespace_size,
				freeFactor,insertedCounter, speedup, dateString, system, overlap);
	}
}
