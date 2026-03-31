/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2026, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.set.aphrodite.expr;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractPropertyExpressionResolver implements ExpressionResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{\\{\\s*(.*?)\\s*\\}\\}");

    protected String resolve(Function<String, String> funcResolver, String input) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);

        // 2. The Loop: Find every occurrence of the placeholder
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = funcResolver.apply(key);
            String replacementText = (replacement != null) ? replacement : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacementText));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

}
