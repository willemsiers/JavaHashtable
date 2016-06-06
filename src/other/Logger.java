package other;

public class Logger {

	public static final int LOG_LEVEL = 1;
	public static final int LOG_VERBOSE = 2;
	public static final int LOG_ERROR = 1;
	public static final boolean NO_LOGGING = Logger.LOG_LEVEL <= LOG_ERROR;

	public static void logV(String msg) {
		if (LOG_LEVEL >= LOG_VERBOSE) {
			System.out.println(msg);
		}
	}

	public static void logE(String msg) {
		if (LOG_LEVEL >= LOG_ERROR) {
			System.out.println(msg);
		}
	}
}
