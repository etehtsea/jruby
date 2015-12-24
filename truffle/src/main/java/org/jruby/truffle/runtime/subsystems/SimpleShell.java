/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Activation;
import org.jruby.truffle.runtime.backtrace.BacktraceFormatter;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.translator.TranslatorDriver.ParserContext;

import java.util.Arrays;
import java.util.StringTokenizer;

public class SimpleShell {

    private int currentFrameIndex;
    private MaterializedFrame currentFrame;

    private final RubyContext context;

    public SimpleShell(RubyContext context) {
        this.context = context;
    }

    public void run(MaterializedFrame frame, Node currentNode) {
        currentFrameIndex = 0;
        currentFrame = frame;

        while (true) {
            final String shellLine = System.console().readLine("> ");

            final StringTokenizer tokenizer = new StringTokenizer(shellLine);

            if (!tokenizer.hasMoreElements()) {
                continue;
            }

            switch (tokenizer.nextToken()) {
                case "backtrace":
                    backtrace(currentNode);
                    break;

                case "continue":
                    return;

                case "exit":
                    // We're in the debugger, not normal Ruby, so just hard exit here
                    System.exit(0);
                    break;

                case "frame":
                    currentFrameIndex = Integer.parseInt(tokenizer.nextToken());
                    currentFrame = RubyCallStack.getBacktrace(currentNode).getActivations().get(currentFrameIndex).getMaterializedFrame();
                    break;

                default:
                    try {
                        final Object result = context.parseAndExecute(
                                Source.fromText(shellLine, "shell"),
                                UTF8Encoding.INSTANCE,
                                ParserContext.EVAL,
                                RubyArguments.getSelf(currentFrame.getArguments()),
                                currentFrame,
                                false,
                                RubyArguments.getDeclarationContext(currentFrame.getArguments()),
                                currentNode);

                        String inspected;

                        try {
                            inspected = context.send(result, "inspect", null).toString();
                        } catch (Exception e) {
                            inspected = String.format("(error inspecting %s@%x %s)", result.getClass().getSimpleName(), result.hashCode(), e.toString());
                        }

                        System.console().writer().println(inspected);
                    } catch (RaiseException e) {
                        final Object rubyException = e.getRubyException();

                        BacktraceFormatter.createDefaultFormatter(context).printBacktrace((DynamicObject) rubyException, Layouts.EXCEPTION.getBacktrace((DynamicObject) rubyException), System.console().writer());
                    }
            }
        }
    }

    private void backtrace(Node currentNode) {
        int n = 0;

        for (Activation activation : RubyCallStack.getBacktrace(currentNode).getActivations()) {
            if (n == currentFrameIndex) {
                System.console().writer().print("  ▶");
            } else {
                System.console().writer().printf("%3d", n);
            }

            System.console().writer().println(BacktraceFormatter.createDefaultFormatter(context).formatLine(Arrays.asList(activation), 0));
            n++;
        }
    }

}
