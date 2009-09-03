 /*
 * Copyright 2003-2009 the original author or authors.
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
package org.codehaus.groovy.eclipse.test.actions;

import org.codehaus.groovy.eclipse.editor.actions.RenameToGroovyAction;
import org.codehaus.groovy.eclipse.editor.actions.RenameToJavaAction;
import org.codehaus.groovy.eclipse.test.EclipseTestCase;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;

/**
 * Tests the commands RenameToGroovy and RenameToJava
 * @author Andrew Eisenberg
 * @created Aug 26, 2009
 *
 */
public class ConvertToJavaOrGroovyActionTest extends EclipseTestCase {
    public void testRenameToGroovy() throws Exception {
        IType type = testProject.createJavaTypeAndPackage("foo", "Bar.java", "class Bar { }");
        IResource file = type.getCompilationUnit().getResource();
        assertTrue(file.getName() + " should exist", file.exists());
        StructuredSelection ss = new StructuredSelection(file);
        IActionDelegate action = new RenameToGroovyAction();
        action.selectionChanged(null, ss);
        action.run(null);
        file.refreshLocal(IResource.DEPTH_INFINITE, null);
        assertFalse(file.getName() + " should not exist", file.exists());
        
        file = file.getParent().getFile(new Path("Bar.groovy"));
        assertTrue(file.getName() + " should exist", file.exists());
    }
    public void testRenameToJava() throws Exception {
        IResource file = testProject.createGroovyTypeAndPackage("foo", "Bar.groovy", "class Bar { }");
        assertTrue(file.getName() + " should exist", file.exists());
        StructuredSelection ss = new StructuredSelection(file);
        IActionDelegate action = new RenameToJavaAction();
        action.selectionChanged(null, ss);
        action.run(null);
        file.refreshLocal(IResource.DEPTH_INFINITE, null);
        assertFalse(file.getName() + " should not exist", file.exists());
        
        file = file.getParent().getFile(new Path("Bar.java"));
        assertTrue(file.getName() + " should exist", file.exists());
    }
    public void testRenameToGroovyAndBack() throws Exception {
        IType type = testProject.createJavaTypeAndPackage("foo", "Bar.java", "class Bar { }");
        IResource file = type.getCompilationUnit().getResource();
        assertTrue(file.getName() + " should exist", file.exists());
        StructuredSelection ss = new StructuredSelection(file);
        IActionDelegate action = new RenameToGroovyAction();
        action.selectionChanged(null, ss);
        action.run(null);
        file.refreshLocal(IResource.DEPTH_INFINITE, null);
        assertFalse(file.getName() + " should not exist", file.exists());
        
        file = file.getParent().getFile(new Path("Bar.groovy"));
        assertTrue(file.getName() + " should exist", file.exists());

        // now back again
        ss = new StructuredSelection(file);
        action = new RenameToJavaAction();
        action.selectionChanged(null, ss);
        action.run(null);
        file.refreshLocal(IResource.DEPTH_INFINITE, null);
        assertFalse(file.getName() + " should not exist", file.exists());
        
        file = file.getParent().getFile(new Path("Bar.java"));
        assertTrue(file.getName() + " should exist", file.exists());

    }
    public void testRenameToJavaAndBack() throws Exception {
        IResource file = testProject.createGroovyTypeAndPackage("foo", "Bar.roovy", "class Bar { }");
        assertTrue(file.getName() + " should exist", file.exists());
        StructuredSelection ss = new StructuredSelection(file);
        IActionDelegate action = new RenameToJavaAction();
        action.selectionChanged(null, ss);
        action.run(null);
        file.refreshLocal(IResource.DEPTH_INFINITE, null);
        assertFalse(file.getName() + " should not exist", file.exists());
        
        file = file.getParent().getFile(new Path("Bar.java"));
        assertTrue(file.getName() + " should exist", file.exists());

        // now back again
        ss = new StructuredSelection(file);
        action = new RenameToGroovyAction();
        action.selectionChanged(null, ss);
        action.run(null);
        file.refreshLocal(IResource.DEPTH_INFINITE, null);
        assertFalse(file.getName() + " should not exist", file.exists());
        
        file = file.getParent().getFile(new Path("Bar.groovy"));
        assertTrue(file.getName() + " should exist", file.exists());

    }

}