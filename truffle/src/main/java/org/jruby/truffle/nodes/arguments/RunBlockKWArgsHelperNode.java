/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.arguments;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.locals.ReadFrameSlotNode;
import org.jruby.truffle.nodes.locals.ReadFrameSlotNodeGen;
import org.jruby.truffle.nodes.locals.WriteFrameSlotNode;
import org.jruby.truffle.nodes.locals.WriteFrameSlotNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;

public class RunBlockKWArgsHelperNode extends RubyNode {

    @Child private ReadFrameSlotNode readArrayNode;
    @Child private WriteFrameSlotNode writeArrayNode;

    private final Object kwrestName;

    public RunBlockKWArgsHelperNode(RubyContext context, SourceSection sourceSection, FrameSlot arrayFrameSlot, Object kwrestName) {
        super(context, sourceSection);
        readArrayNode = ReadFrameSlotNodeGen.create(arrayFrameSlot);
        writeArrayNode = WriteFrameSlotNodeGen.create(arrayFrameSlot);
        this.kwrestName = kwrestName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.bailout("blocks with kwargs are not optimized yet");

        final Object array = readArrayNode.executeRead(frame);

        final Object remainingArray = ruby(frame, "Truffle::Primitive.load_arguments_from_array_kw_helper(array, kwrest_name, binding)", "array", array, "kwrest_name", kwrestName, "binding", Layouts.BINDING.createBinding(getContext().getCoreLibrary().getBindingFactory(), frame.materialize()));

        writeArrayNode.executeWrite(frame, remainingArray);

        return nil();
    }

}
