package com.coalminesoftware.bezier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CollectionUtils {
    private CollectionUtils() { }

    /**
     * @return A modifiable list of the given size, filled with null values.
     */
    public static <T> List<T> createFilledList(int count) {
        return createFilledList(count, null);
    }

    /**
     * @return A modifiable list of the given size, filled with the given value.
     */
    public static <T> List<T> createFilledList(int count, T value) {
        return new ArrayList<>(Collections.nCopies(count, value));
    }
}
