/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.LineLocation;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.tools.LineToProbesMap;

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.BindingNodes;
import org.jruby.truffle.nodes.core.ProcNodes;
import org.jruby.truffle.nodes.methods.DeclarationContext;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.RubyLanguage;
import org.jruby.truffle.runtime.layouts.Layouts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttachmentsManager {

    private final RubyContext context;
    private final Instrumenter instrumenter;

    public AttachmentsManager(RubyContext context, Instrumenter instrumenter) {
        this.context = context;
        this.instrumenter = instrumenter;
    }

    public synchronized EventBinding<?> attach(String file, int line, final DynamicObject block) {
        assert RubyGuards.isRubyProc(block);

        final Source source = context.getSourceCache().getBestSourceFuzzily(file);
        SourceSectionFilter filter = SourceSectionFilter.newBuilder().sourceIs(source).lineIs(line).tagIs(InstrumentationTag.STATEMENT).build();
        return instrumenter.attachFactory(filter, new EventNodeFactory() {
            public EventNode create(EventContext eventContext) {
                return new AttachmentEventNode(context, block);
            }
        });

        // with the new API you are not notified if a statement is not actually installed
        // because wrappers and installing is lazy. Is that a problem?

        // throw new RuntimeException("couldn't find a statement!");
    }

    private static class AttachmentEventNode extends EventNode {

        private final RubyContext context;
        private final DynamicObject block;
        @Child private DirectCallNode callNode;

        public AttachmentEventNode(RubyContext context, DynamicObject block) {
            this.context = context;
            this.block = block;
            this.callNode = Truffle.getRuntime().createDirectCallNode(Layouts.PROC.getCallTargetForType(block));

            // (chumer): do we still want to clone and inline always? don't think so
            if (callNode.isCallTargetCloningAllowed()) {
                callNode.cloneCallTarget();
            }
            if (callNode.isInlinable()) {
                callNode.forceInlining();
            }
        }

        @Override
        public void onEnter(VirtualFrame frame) {
            callNode.call(frame, ProcNodes.packArguments(block, new Object[] {  BindingNodes.createBinding(context, frame.materialize())}));
        }
    }

}
