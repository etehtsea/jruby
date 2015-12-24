/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.coerce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;

/**
 * Casts an object to a Ruby Proc object.
 */
@NodeChild("child")
public abstract class ToProcNode extends RubyNode {

    public ToProcNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    @Specialization(guards = "isNil(nil)")
    public DynamicObject doNil(Object nil) {
        return nil();
    }

    @Specialization(guards = "isRubyProc(proc)")
    public DynamicObject doRubyProc(DynamicObject proc) {
        return proc;
    }

    @Specialization(guards = "!isRubyProc(object)")
    public DynamicObject doObject(VirtualFrame frame, Object object,
            @Cached("createCallNode()") CallDispatchHeadNode toProc) {
        final Object coerced;
        try {
            coerced = toProc.call(frame, object, "to_proc", null);
        } catch (RaiseException e) {
            if (Layouts.BASIC_OBJECT.getLogicalClass(e.getRubyException()) == getContext().getCoreLibrary().getNoMethodErrorClass()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeErrorNoImplicitConversion(object, "Proc", this));
            } else {
                throw e;
            }
        }

        if (RubyGuards.isRubyProc(coerced)) {
            return (DynamicObject) coerced;
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeErrorBadCoercion(object, "Proc", "to_proc", coerced, this));
        }
    }

    protected CallDispatchHeadNode createCallNode() {
        return DispatchHeadNodeFactory.createMethodCall(getContext());
    }

}
