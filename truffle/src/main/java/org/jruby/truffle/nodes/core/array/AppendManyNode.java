/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayMirror;
import org.jruby.truffle.runtime.array.ArrayReflector;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.layouts.Layouts;

import java.util.Arrays;

@NodeChildren({
        @NodeChild("array"),
        @NodeChild("otherSize"),
        @NodeChild("other"),
})
@ImportStatic(ArrayGuards.class)
public abstract class AppendManyNode extends RubyNode {

    public AppendManyNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract DynamicObject executeAppendMany(DynamicObject array, int otherSize, Object other);

    // Append into an empty array

    // TODO CS 12-May-15 differentiate between null and empty but possibly having enough space

    @Specialization(guards = {"isRubyArray(array)", "isEmptyArray(array)"})
    public DynamicObject appendManyEmpty(DynamicObject array, int otherSize, int[] other) {
        Layouts.ARRAY.setStore(array, Arrays.copyOf(other, otherSize));
        Layouts.ARRAY.setSize(array, otherSize);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isEmptyArray(array)"})
    public DynamicObject appendManyEmpty(DynamicObject array, int otherSize, long[] other) {
        Layouts.ARRAY.setStore(array, Arrays.copyOf(other, otherSize));
        Layouts.ARRAY.setSize(array, otherSize);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isEmptyArray(array)"})
    public DynamicObject appendManyEmpty(DynamicObject array, int otherSize, double[] other) {
        Layouts.ARRAY.setStore(array, Arrays.copyOf(other, otherSize));
        Layouts.ARRAY.setSize(array, otherSize);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isEmptyArray(array)"})
    public DynamicObject appendManyEmpty(DynamicObject array, int otherSize, Object[] other) {
        Layouts.ARRAY.setStore(array, Arrays.copyOf(other, otherSize));
        Layouts.ARRAY.setSize(array, otherSize);
        return array;
    }

    // Append of the correct type

    @Specialization(guards = {"isRubyArray(array)", "isIntArray(array)"})
    public DynamicObject appendManySameType(DynamicObject array, int otherSize, int[] other,
                                   @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isLongArray(array)"})
    public DynamicObject appendManySameType(DynamicObject array, int otherSize, long[] other,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isDoubleArray(array)"})
    public DynamicObject appendManySameType(DynamicObject array, int otherSize, double[] other,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isObjectArray(array)"})
    public DynamicObject appendManySameType(DynamicObject array, int otherSize, Object[] other,
                                  @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayReflector.reflect((Object[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    public void appendManySameTypeGeneric(DynamicObject array, ArrayMirror storeMirror,
                                          int otherSize, ArrayMirror otherStoreMirror,
                                          ConditionProfile extendProfile) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + otherSize;

        final ArrayMirror newStoreMirror;

        if (extendProfile.profile(newSize > storeMirror.getLength())) {
            newStoreMirror = storeMirror.copyArrayAndMirror(ArrayUtils.capacity(storeMirror.getLength(), newSize));
        } else {
            newStoreMirror = storeMirror;
        }

        otherStoreMirror.copyTo(newStoreMirror, 0, oldSize, otherSize);
        Layouts.ARRAY.setStore(array, newStoreMirror.getArray());
        Layouts.ARRAY.setSize(array, newSize);
    }

    // Append something else into an Object[]

    @Specialization(guards = {"isRubyArray(array)", "isObjectArray(array)"})
    public DynamicObject appendManyBoxIntoObject(DynamicObject array, int otherSize, int[] other,
                                        @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManyBoxIntoObjectGeneric(array, otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isObjectArray(array)"})
    public DynamicObject appendManyBoxIntoObject(DynamicObject array, int otherSize, long[] other,
                                        @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManyBoxIntoObjectGeneric(array, otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isObjectArray(array)"})
    public DynamicObject appendManyBoxIntoObject(DynamicObject array, int otherSize, double[] other,
                                        @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManyBoxIntoObjectGeneric(array, otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    public void appendManyBoxIntoObjectGeneric(DynamicObject array, int otherSize, ArrayMirror otherStoreMirror,
                                          ConditionProfile extendProfile) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + otherSize;

        final Object[] oldStore = (Object[]) Layouts.ARRAY.getStore(array);
        final Object[] newStore;

        if (extendProfile.profile(newSize > oldStore.length)) {
            newStore = ArrayUtils.grow(oldStore, ArrayUtils.capacity(oldStore.length, newSize));
        } else {
            newStore = oldStore;
        }

        otherStoreMirror.copyTo(newStore, 0, oldSize, otherSize);
        Layouts.ARRAY.setStore(array, newStore);
        Layouts.ARRAY.setSize(array, newSize);
    }

    // Append forcing a generalization from int[] to long[]

    @Specialization(guards = {"isRubyArray(array)", "isIntArray(array)"})
    public DynamicObject appendManyLongIntoInteger(DynamicObject array, int otherSize, long[] other) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + otherSize;

        final int[] oldStore = (int[]) Layouts.ARRAY.getStore(array);
        long[] newStore = ArrayUtils.longCopyOf(oldStore, ArrayUtils.capacity(oldStore.length, newSize));

        System.arraycopy(other, 0, newStore, oldSize, otherSize);

        Layouts.ARRAY.setStore(array, newStore);
        Layouts.ARRAY.setSize(array, newSize);
        return array;
    }

    // Append forcing a generalization to Object[]

    @Specialization(guards = {"isRubyArray(array)", "isIntArray(array)"})
    public DynamicObject appendManyGeneralizeIntegerDouble(DynamicObject array, int otherSize, double[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isIntArray(array)"})
    public DynamicObject appendManyGeneralizeIntegerDouble(DynamicObject array, int otherSize, Object[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isLongArray(array)"})
    public DynamicObject appendManyGeneralizeLongDouble(DynamicObject array, int otherSize, double[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isLongArray(array)"})
    public DynamicObject appendManyGeneralizeLongDouble(DynamicObject array, int otherSize, Object[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isDoubleArray(array)"})
    public DynamicObject appendManyGeneralizeDoubleInteger(DynamicObject array, int otherSize, int[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isDoubleArray(array)"})
    public DynamicObject appendManyGeneralizeDoubleLong(DynamicObject array, int otherSize, long[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isDoubleArray(array)"})
    public DynamicObject appendManyGeneralizeDoubleObject(DynamicObject array, int otherSize, Object[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    public void appendManyGeneralizeGeneric(DynamicObject array, ArrayMirror storeMirror, int otherSize, ArrayMirror otherStoreMirror) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + otherSize;
        Object[] newStore = storeMirror.getBoxedCopy(ArrayUtils.capacity(storeMirror.getLength(), newSize));
        otherStoreMirror.copyTo(newStore, 0, oldSize, otherSize);
        Layouts.ARRAY.setStore(array, newStore);
        Layouts.ARRAY.setSize(array, newSize);
    }

}
