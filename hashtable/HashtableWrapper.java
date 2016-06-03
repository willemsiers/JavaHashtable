package hashtable;

import java.util.Map;

/**
 * Wraps java.util.Map implementations in the same interface as FastSet
 */
public class HashtableWrapper implements AbstractFastSet {

	private final Map<Vector, Vector> map;

	public HashtableWrapper(Map<Vector, Vector> map) {
		this.map = map;
	}

	@Override
	public boolean findOrPut(Vector v) {
		boolean found = false;
		found = (map.put(v,v) != null);
		return found;
	}

	@Override
	public Vector[] getData() {
		Vector[] ss = new Vector[map.size()];
		map.values().toArray(ss);
		return ss;
	}

	@Override
	public void cleanup(){
	}

	@Override
	public String toString() {
		return map.getClass().getCanonicalName();
	}
}