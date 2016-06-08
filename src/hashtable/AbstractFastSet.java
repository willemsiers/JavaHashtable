package hashtable;

public interface AbstractFastSet<V> {
	boolean findOrPut(V v);

	V[] getData();

	void cleanup();
}
