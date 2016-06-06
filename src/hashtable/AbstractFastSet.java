package hashtable;

public interface AbstractFastSet {
	boolean findOrPut(Vector v);

	Vector[] getData();

	void cleanup();
}
