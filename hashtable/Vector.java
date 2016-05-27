package hashtable;

/**
 * Vector = State data
 */
public class Vector {
	public String value = null;

	@Override
	public String toString() {
		return value;
	}

	@Override
	public int hashCode() {
		// note: Object.hashCode is non-deterministic (based on memory address), String.hashCode is.
		if (value == null) {
			return 0;
		}
		return Math.abs(value.hashCode());
	}

	@Override
	public boolean equals(Object o) {
		boolean result = false;

		if (value != null) {
			if (o instanceof String) {
				String s = (String) o;
				result = value.equals(s);
			}
		}

		return result;
	}
}
