package hashtable;

import java.util.Map;

/**
 * Wraps java.util.Map implementations in the same interface as FastSet
 */
public class HashtableWrapper<V> implements AbstractFastSet<V> {

	private final Map<V, V> map;

	public HashtableWrapper(Map<V, V> map) {
		this.map = map;
	}

	@Override
	public boolean findOrPut(V v) {
		boolean found = false;
		found = (map.put(v,v) != null);
		return found;
	}

	@Override
	public V[] getData() {
		V[] ss = (V[]) new Object[map.size()];
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