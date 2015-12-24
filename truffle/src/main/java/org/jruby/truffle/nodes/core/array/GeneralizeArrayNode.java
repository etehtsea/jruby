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

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.layouts.Layouts;

@NodeChildren({
        @NodeChild(value = "array", type = RubyNode.class),
        @NodeChild(value = "requiredCapacity", type = RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class GeneralizeArrayNode extends RubyNode {

    public GeneralizeArrayNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeGeneralize(DynamicObject array, int requiredCapacity);

    @Specialization(
            guards = { "isRubyArray(array)", "isNullArray(array)" }
    )
    public DynamicObject generalizeNull(DynamicObject array, int requiredCapacity) {
        Object store = new Object[requiredCapacity];
        Layouts.ARRAY.setStore(array, store);
        return array;
    }

    @Specialization(
            guards = { "isRubyArray(array)", "isIntArray(array)" }
    )
    public DynamicObject generalizeInt(DynamicObject array, int requiredCapacity) {
        final int[] store = (int[]) Layouts.ARRAY.getStore(array);
        Layouts.ARRAY.setStore(array, ArrayUtils.boxExtra(store, ArrayUtils.capacity(store.length, requiredCapacity) - store.length));
        return array;
    }

    @Specialization(
            guards = { "isRubyArray(array)", "isLongArray(array)" }
    )
    public DynamicObject generalizeLong(DynamicObject array, int requiredCapacity) {
        final long[] store = (long[]) Layouts.ARRAY.getStore(array);
        Layouts.ARRAY.setStore(array, ArrayUtils.boxExtra(store, ArrayUtils.capacity(store.length, requiredCapacity) - store.length));
        return array;
    }

    @Specialization(
            guards = { "isRubyArray(array)", "isDoubleArray(array)" }
    )
    public DynamicObject generalizeDouble(DynamicObject array, int requiredCapacity) {
        final double[] store = (double[]) Layouts.ARRAY.getStore(array);
        Layouts.ARRAY.setStore(array, ArrayUtils.boxExtra(store, ArrayUtils.capacity(store.length, requiredCapacity) - store.length));
        return array;
    }

}
