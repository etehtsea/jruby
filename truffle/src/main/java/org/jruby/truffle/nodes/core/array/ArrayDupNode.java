/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNode;
import org.jruby.truffle.nodes.objects.AllocateObjectNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.layouts.Layouts;

/**
 * Dup an array, without using any method lookup. This isn't a call - it's an operation on a core class.
 */
@NodeChild(value = "array", type = RubyNode.class)
@ImportStatic(ArrayGuards.class)
public abstract class ArrayDupNode extends RubyNode {

    @Child private AllocateObjectNode allocateNode;

    public ArrayDupNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        allocateNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
    }

    public abstract DynamicObject executeDup(VirtualFrame frame, DynamicObject array);

    @Specialization(guards = {"isRubyArray(from)", "isNullArray(from)"})
    public DynamicObject dupNull(DynamicObject from) {
        return allocateNode.allocate(getContext().getCoreLibrary().getArrayClass(), null, 0);
    }

    @Specialization(guards = {"isRubyArray(from)", "isIntArray(from)"})
    public DynamicObject dupIntegerFixnum(DynamicObject from) {
        final int[] store = (int[]) Layouts.ARRAY.getStore(from);
        return allocateNode.allocate(
                getContext().getCoreLibrary().getArrayClass(),
                store.clone(),
                Layouts.ARRAY.getSize(from));
    }

    @Specialization(guards = {"isRubyArray(from)", "isLongArray(from)"})
    public DynamicObject dupLongFixnum(DynamicObject from) {
        final long[] store = (long[]) Layouts.ARRAY.getStore(from);
        return allocateNode.allocate(
                getContext().getCoreLibrary().getArrayClass(),
                store.clone(),
                Layouts.ARRAY.getSize(from));
    }

    @Specialization(guards = {"isRubyArray(from)", "isDoubleArray(from)"})
    public DynamicObject dupFloat(DynamicObject from) {
        final double[] store = (double[]) Layouts.ARRAY.getStore(from);
        return allocateNode.allocate(
                getContext().getCoreLibrary().getArrayClass(),
                store.clone(),
                Layouts.ARRAY.getSize(from));
    }

    @Specialization(guards = {"isRubyArray(from)", "isObjectArray(from)"})
    public DynamicObject dupObject(DynamicObject from) {
        final Object[] store = (Object[]) Layouts.ARRAY.getStore(from);
        return allocateNode.allocate(
                getContext().getCoreLibrary().getArrayClass(),
                ArrayUtils.copy(store),
                Layouts.ARRAY.getSize(from));
    }

}
