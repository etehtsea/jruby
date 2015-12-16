/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.format.parser;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;

import org.jruby.truffle.format.nodes.PackNode;
import org.jruby.truffle.format.nodes.PackRootNode;
import org.jruby.truffle.format.nodes.SourceNode;
import org.jruby.truffle.format.nodes.control.SequenceNode;
import org.jruby.truffle.format.nodes.format.FormatFloatNodeGen;
import org.jruby.truffle.format.nodes.format.FormatIntegerNodeGen;
import org.jruby.truffle.format.nodes.read.LiteralBytesNode;
import org.jruby.truffle.format.nodes.read.LiteralIntegerNode;
import org.jruby.truffle.format.nodes.read.ReadHashValueNodeGen;
import org.jruby.truffle.format.nodes.read.ReadIntegerNodeGen;
import org.jruby.truffle.format.nodes.read.ReadStringNodeGen;
import org.jruby.truffle.format.nodes.read.ReadValueNodeGen;
import org.jruby.truffle.format.nodes.type.ToDoubleWithCoercionNodeGen;
import org.jruby.truffle.format.nodes.type.ToIntegerNodeGen;
import org.jruby.truffle.format.nodes.type.ToStringNodeGen;
import org.jruby.truffle.format.nodes.write.WriteByteNode;
import org.jruby.truffle.format.nodes.write.WriteBytesNodeGen;
import org.jruby.truffle.format.nodes.write.WritePaddedBytesNodeGen;
import org.jruby.truffle.format.runtime.PackEncoding;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a format expression into a tree of Truffle nodes.
 */
public class FormatParser {

    private final RubyContext context;

    private PackEncoding encoding = PackEncoding.DEFAULT;

    public FormatParser(RubyContext context) {
        this.context = context;
    }

    public CallTarget parse(ByteList format) {
        final FormatTokenizer tokenizer = new FormatTokenizer(context, format);
        final PackNode body = parse(tokenizer);
        return Truffle.getRuntime().createCallTarget(new PackRootNode(PackCompiler.describe(format.toString()), encoding, body));
    }

    public PackNode parse(FormatTokenizer tokenizer) {
        final List<PackNode> sequenceChildren = new ArrayList<>();

        while (true) {
            Object token = tokenizer.next();

            if (token == null) {
                break;
            }

            final PackNode node;

            if (token instanceof ByteList) {
                final ByteList byteList = (ByteList) token;

                if (byteList.length() == 1) {
                    node = new WriteByteNode(context, (byte) byteList.get(0));
                } else {
                    node = WriteBytesNodeGen.create(context, new LiteralBytesNode(context, byteList));
                }
            } else if (token instanceof FormatDirective) {
                final FormatDirective directive = (FormatDirective) token;

                final PackNode valueNode;

                if (directive.getKey() == null) {
                    valueNode = ReadValueNodeGen.create(context, new SourceNode());
                } else {
                    valueNode = ReadHashValueNodeGen.create(context, directive.getKey(), new SourceNode());
                }

                switch (directive.getType()) {
                    case '%':
                        node = new WriteByteNode(context, (byte) '%');
                        break;
                    case '{':
                        node = WriteBytesNodeGen.create(context,
                                ToStringNodeGen.create(context, true, "to_s", false, new ByteList(),
                                        valueNode));
                        break;
                    case 's':
                        if (directive.getKey() == null) {
                            if (directive.getSpacePadding() == FormatDirective.DEFAULT) {
                                node = WriteBytesNodeGen.create(context, ReadStringNodeGen.create(
                                        context, true, "to_s", false, new ByteList(), new SourceNode()));
                            } else {
                                node = WritePaddedBytesNodeGen.create(context, directive.getSpacePadding(), directive.getLeftJustified(),
                                        ReadStringNodeGen.create(context, true, "to_s", false, new ByteList(), new SourceNode()));
                            }
                        } else {
                            if (directive.getSpacePadding() == FormatDirective.DEFAULT) {
                                node = WriteBytesNodeGen.create(context, ToStringNodeGen.create(
                                        context, true, "to_s", false, new ByteList(), valueNode));
                            } else {
                                node = WritePaddedBytesNodeGen.create(context, directive.getSpacePadding(), directive.getLeftJustified(),
                                        ToStringNodeGen.create(context, true, "to_s", false, new ByteList(), valueNode));
                            }
                        }
                        break;
                    case 'd':
                    case 'i':
                    case 'u':
                    case 'x':
                    case 'X':
                        final PackNode spacePadding;
                        if (directive.getSpacePadding() == FormatDirective.PADDING_FROM_ARGUMENT) {
                            spacePadding = ReadIntegerNodeGen.create(context, new SourceNode());
                        } else {
                            spacePadding = new LiteralIntegerNode(context, directive.getSpacePadding());
                        }

                        final PackNode zeroPadding;

                        /*
                         * Precision and zero padding both set zero padding -
                         * but precision has priority and explicit zero padding
                         * is actually ignored if it's set.
                         */

                        if (directive.getZeroPadding() == FormatDirective.PADDING_FROM_ARGUMENT) {
                            zeroPadding = ReadIntegerNodeGen.create(context, new SourceNode());
                        } else if (directive.getPrecision() != FormatDirective.DEFAULT) {
                            zeroPadding = new LiteralIntegerNode(context, directive.getPrecision());
                        } else {
                            zeroPadding = new LiteralIntegerNode(context, directive.getZeroPadding());
                        }

                        final char format;

                        switch (directive.getType()) {
                            case 'd':
                            case 'i':
                            case 'u':
                                format = 'd';
                                break;
                            case 'x':
                            case 'X':
                                format = directive.getType();
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }

                        node = WriteBytesNodeGen.create(context,
                                FormatIntegerNodeGen.create(context, format,
                                        spacePadding,
                                        zeroPadding,
                                        ToIntegerNodeGen.create(context, valueNode)));
                        break;
                    case 'f':
                    case 'g':
                    case 'G':
                    case 'e':
                    case 'E':
                        node = WriteBytesNodeGen.create(context,
                                FormatFloatNodeGen.create(context, directive.getSpacePadding(),
                                        directive.getZeroPadding(), directive.getPrecision(),
                                        directive.getType(),
                                        ToDoubleWithCoercionNodeGen.create(context,
                                                valueNode)));
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            } else {
                throw new UnsupportedOperationException();
            }

            sequenceChildren.add(node);
        }

        return new SequenceNode(context, sequenceChildren.toArray(new PackNode[sequenceChildren.size()]));
    }

}
