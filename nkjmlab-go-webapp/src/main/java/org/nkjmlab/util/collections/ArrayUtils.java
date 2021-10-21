package org.nkjmlab.util.collections;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArrayUtils {

  public static void main(String[] args) {
    split(3, 1, 2, 3, 4).forEach(a -> System.out.println(Arrays.toString(a)));

    System.out.println(Arrays.toString(addAll(new int[] {1, 2, 3}, new int[] {4, 5, 6})));
  }

  public static <T> List<T[]> split(int size, @SuppressWarnings("unchecked") T... objects) {

    int slotNum = Math.floorDiv(objects.length, size);
    List<T[]> result = new ArrayList<>(slotNum + 1);

    for (int i = 0; i < slotNum; i++) {
      result.add(Arrays.copyOfRange(objects, size * i, size * (i + 1)));
    }
    if (size * slotNum != objects.length) {
      result.add(Arrays.copyOfRange(objects, size * slotNum, objects.length));
    }
    return result;
  }

  public static <T> T[] add(T[] array, T elem) {
    final int arrayLength = Array.getLength(array);
    @SuppressWarnings("unchecked")
    final T[] newArray =
        (T[]) Array.newInstance(array.getClass().getComponentType(), arrayLength + 1);
    System.arraycopy(array, 0, newArray, 0, arrayLength);
    newArray[arrayLength] = elem;
    return newArray;
  }


  public static <T> T[] addAll(T[] array, @SuppressWarnings("unchecked") T... elems) {
    final int arrayLength = Array.getLength(array);
    final int elemsLength = Array.getLength(elems);
    @SuppressWarnings("unchecked")
    final T[] newArray =
        (T[]) Array.newInstance(array.getClass().getComponentType(), arrayLength + elemsLength);
    System.arraycopy(array, 0, newArray, 0, arrayLength);
    System.arraycopy(elems, 0, newArray, arrayLength, elemsLength);
    return newArray;
  }

  public static int[] addAll(int[] array, int... elems) {
    final int arrayLength = Array.getLength(array);
    final int elemsLength = Array.getLength(elems);
    final int[] newArray =
        (int[]) Array.newInstance(array.getClass().getComponentType(), arrayLength + elemsLength);
    System.arraycopy(array, 0, newArray, 0, arrayLength);
    System.arraycopy(elems, 0, newArray, arrayLength, elemsLength);
    return newArray;
  }


}
