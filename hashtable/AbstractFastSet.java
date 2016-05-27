package hashtable;

public interface AbstractFastSet {
	boolean findOrPut(Vector v, boolean insertAbsent);

	Vector[] getData();

	void cleanup();
}
