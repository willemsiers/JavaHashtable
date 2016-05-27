package hashtable;

import java.lang.reflect.Field;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.util.*;

import static jdk.nashorn.internal.runtime.regexp.joni.constants.StackPopLevel.FREE;
import static other.Logger.logV;

class SubHashMap<K, V> extends HashMap<K, V> {

	public SubHashMap(int cap, int lf) {
		super(cap,lf);
	}

	public static void main(String[] args) throws Exception {
		final int STATESPACE_SIZE = 8 * 1024;
		final int FREE_FACTOR = 4;

		Vector[] statespace = new Vector[STATESPACE_SIZE];
		for (int i = 0; i < STATESPACE_SIZE; i++) {
			statespace[i] = new Vector();
			statespace[i].value = "" + i;
		}

		SubHashMap<Vector, Vector> map = new SubHashMap<Vector, Vector>(STATESPACE_SIZE*FREE_FACTOR, 1);

		for (int i = 0; i < STATESPACE_SIZE; i++) {
			map.put(statespace[i],statespace[i]); // if false, data is put
		}

		map.dumpBuckets();
	}

	public void dumpBuckets() throws Exception {

		Field f = HashMap.class.getDeclaredField("table");
		f.setAccessible(true);

		Map.Entry<K, V>[] table = (Map.Entry<K, V>[]) f.get(this);

		Class<?> hashMapEntryClass = null;
		for (Class<?> c : HashMap.class.getDeclaredClasses()) {
			System.out.println(c.getCanonicalName());
			try {
				if (c.getDeclaredField("next") != null) {
					hashMapEntryClass = c;
				}
			}catch(Exception e){

			}
		}

		Field nextField = hashMapEntryClass.getDeclaredField("next");
		nextField.setAccessible(true);

		int colissions = 0;

		for (int i = 0; i < table.length; i++) {

			System.out.print("Bucket " + i + ": ");
			Map.Entry<K, V> entry = table[i];
			int contains = 0;

			while (entry != null) {
				contains ++;
				System.out.print(entry.getKey() + " ");
				entry = (Map.Entry<K, V>) nextField.get(entry);
			}

			System.out.println();
			if(contains > 1){
				colissions+=contains;
			}
		}
		System.out.println("colissions: "+colissions);
	}
}