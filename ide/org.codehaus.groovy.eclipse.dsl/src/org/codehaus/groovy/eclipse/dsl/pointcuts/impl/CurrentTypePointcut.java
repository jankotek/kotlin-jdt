/*
 * Copyright 2003-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.eclipse.dsl.pointcuts.impl;

import org.codehaus.groovy.eclipse.dsl.pointcuts.AbstractPointcut;
import org.codehaus.groovy.eclipse.dsl.pointcuts.BindingSet;
import org.codehaus.groovy.eclipse.dsl.pointcuts.GroovyDSLDContext;
import org.codehaus.groovy.eclipse.dsl.pointcuts.IPointcut;

/**
 * Tests that the type being analyzed matches.  The match can
 * either be a string match (ie - the type name),
 * or it can pass the current type to a containing pointcut
 * @author andrew
 * @created Feb 10, 2011
 */
public class CurrentTypePointcut extends AbstractPointcut {

    public CurrentTypePointcut(String containerIdentifier) {
        super(containerIdentifier);
    }

    public BindingSet matches(GroovyDSLDContext pattern) {
        Object firstArgument = getFirstArgument();
        if (firstArgument instanceof String) {
            if (pattern.matchesType((String) firstArgument)) {
                return new BindingSet().addDefaultBinding(pattern.getCurrentType());
            } else {
                return null;
            }
        } else {
            pattern.setOuterPointcutBinding(pattern.getCurrentType());
            BindingSet matches = ((IPointcut) firstArgument).matches(pattern);
            if (matches != null) {
                matches.addDefaultBinding(pattern.getCurrentType());
            }
            return matches;
        }
    }

    /**
     * expecting one arg that is either a string or a pointcut
     */
    @Override
    public String verify() {
        String oneStringOrOnePointcutArg = oneStringOrOnePointcutArg();
        if (oneStringOrOnePointcutArg == null) {
            return super.verify();
        }
        return oneStringOrOnePointcutArg;
    }
}