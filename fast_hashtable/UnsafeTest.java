package fast_hashtable;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeTest {

   long a = 0;
   long b = 0;

   public UnsafeTest(int size) {
      final int LONG_SIZE_BYTES = 8;
      Unsafe unsafe = null;
      try {
         Field f = Unsafe.class.getDeclaredField("theUnsafe");
         f.setAccessible(true);
         unsafe = (Unsafe)f.get(null);
      } catch (NoSuchFieldException e) {
         e.printStackTrace();
      } catch (IllegalAccessException e) {
         e.printStackTrace();
      }

      if(unsafe != null)
      {
         long aOffset = 0;
         try {
            aOffset = unsafe.objectFieldOffset(UnsafeTest.class.getDeclaredField("b"));
         } catch (NoSuchFieldException e) {
            e.printStackTrace();
         }
         long arrayBase = unsafe.allocateMemory(size * LONG_SIZE_BYTES);
         unsafe.setMemory(arrayBase, size*LONG_SIZE_BYTES, (byte) 0);
//         boolean success = unsafe.compareAndSwapLong(this, aOffset, 0, 4);
         for (int offset = 0; offset < size; offset++) {
            //unsafe.putAddress(arrayBase + offset*LONG_SIZE_BYTES, (long) Math.pow(offset, 2));
         }

         for (int offset = 0; offset < size; offset++) {
            long value = unsafe.getAddress(arrayBase + offset*LONG_SIZE_BYTES);
            System.out.printf("offset %d, value %d\n", offset, value);
         }

         Unsafe finalUnsafe = unsafe;
         final Object nullObj = (Object)null; //memory base
         for (int i = 0; i<4; i++) {
            new Thread(new Runnable() {
               @Override
               public void run() {
                  for (int i = 0; i < 10; i++) {
                     int pos = i % size;
                     boolean success = finalUnsafe.compareAndSwapLong(nullObj, arrayBase+(pos * LONG_SIZE_BYTES), pos, 0);
                     System.out.printf("pos=%d, success %b\n", pos, success);
                  }
               }
            }).start();
         }

         System.out.printf("aoffset"+aOffset);
         System.out.printf("a, b "+ this.a +" , "+b);
         unsafe.freeMemory( arrayBase);
      }else{
         throw new NullPointerException();
      }
   }

   public static void main(String[] args) {
      UnsafeTest ut = new UnsafeTest(8);
   }
}
