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
//		if (value == null) {
//			return 0;
//		}
		return value.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof Vector) && ((Vector)o).value.equals(this.value);
//		boolean result = false;
//
//		if (o instanceof Vector) {
//			String s = ((Vector) o).value;
//			result = value.equals(s);
//		}
//		return result;
	}
}
