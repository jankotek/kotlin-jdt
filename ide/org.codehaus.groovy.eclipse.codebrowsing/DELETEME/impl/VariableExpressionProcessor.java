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
package org.codehaus.groovy.eclipse.codebrowsing.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.eclipse.GroovyPlugin;
import org.codehaus.groovy.eclipse.codebrowsing.ASTNodeFinder;
import org.codehaus.groovy.eclipse.codebrowsing.ASTSearchResult;
import org.codehaus.groovy.eclipse.codebrowsing.IDeclarationSearchInfo;
import org.codehaus.groovy.eclipse.codebrowsing.IDeclarationSearchProcessor;
import org.codehaus.groovy.eclipse.codebrowsing.SourceCodeFinder;
import org.codehaus.groovy.eclipse.core.GroovyCore;
import org.codehaus.groovy.eclipse.core.model.GroovyProjectFacade;
import org.codehaus.groovy.eclipse.core.model.IDocumentFacade;
import org.codehaus.groovy.eclipse.core.util.ASTUtils;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.LocalVariable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * @author emp
 */
public class VariableExpressionProcessor implements IDeclarationSearchProcessor {
	public IJavaElement[] getProposals(IDeclarationSearchInfo info) {
		VariableExpression expr = (VariableExpression) info.getASTNode();

		Variable var = expr.getAccessedVariable();

		if (expr.getName().equals(info.getIdentifier())) {
			// Clicked on the name.
			if (var instanceof FieldNode) {
				return processFieldNode(info, (FieldNode) var);
			} else if (var instanceof VariableExpression) {
				return processVariableExpression(info, (VariableExpression) var);
			} else if (var instanceof Parameter) {
				return processParameter(info, expr, (Parameter) var);
			} else {
			    // look for field in super class
	            ClassNode superClass = info.getClassNode();
	            while (superClass != null) {
	                FieldNode field = superClass.getDeclaredField(var.getName());
	                if (field != null) {
	                    return processFieldNode(info, field);
	                }
	                superClass = superClass.getSuperClass();
	            }
			}
		} else if (var.getType().getName().endsWith(info.getIdentifier())) {
			// Clicked on the type.
			IJavaElement sourceCode = SourceCodeFinder.find(var.getType(), info.getEditorFacade().getFile());

			if (sourceCode != null) {
				return new IJavaElement[] { 
				        sourceCode 
				 };
			}

			return NONE;
		}

		return NONE;
	}

	private IJavaElement[] processFieldNode(
			IDeclarationSearchInfo info, FieldNode fieldNode) {
	    GroovyProjectFacade facade = info.getEditorFacade().getProjectFacade();
	    IType type = facade.groovyClassToJavaType(fieldNode.getDeclaringClass());
	    if (type != null) {
	        IField field = type.getField(fieldNode.getName());
	        if (field != null) {
	            return new IJavaElement[] { 
	                    field };
	        }
	    }
	    
	    
	    return NONE;
	}

	private IJavaElement[] processVariableExpression(
			IDeclarationSearchInfo info, VariableExpression expr) {
	    IDocumentFacade facade = info.getEditorFacade();
		IRegion highlight = ASTUtils.getRegion(facade, expr, info
				.getIdentifier().length());
		if (highlight != null) {
			return new IJavaElement[] { 
							createLocalVariableSourceCode(
							        expr.getName(), expr.getType().getName(), 
							        highlight, facade) };
		}
		return NONE;
	}

	private IJavaElement[] processParameter(
			IDeclarationSearchInfo info, VariableExpression expr,
			Parameter param) {
		ASTSearchResult result = ASTNodeFinder.findSurroundingClosure(info
				.getModuleNode(), expr);

		if (result == null) {
			result = ASTNodeFinder.findSurroundingMethod(info.getModuleNode(),
					expr);
		}

		if (result != null) {
			return processClosureOrMethodExpression(info, param, result);
		}
		return NONE;
	}

	private IJavaElement[] processClosureOrMethodExpression(
			IDeclarationSearchInfo info, Parameter param, ASTSearchResult result) {
		ASTNode expr = result.getASTNode();
		IDocumentFacade facade = info.getEditorFacade();
		try {
			int offset0 = facade.getOffset(expr.getLineNumber() - 1, expr
					.getColumnNumber() - 1);
			int offset1 = facade.getOffset(expr.getLastLineNumber() - 1, expr
					.getLastColumnNumber() - 1);
			String text = facade.getText(offset0, offset1 - offset0 + 1);

			int identOffset = findIdentifierOffset(text, param
					.getName());
			if (identOffset != -1) {
				Region region = new Region(offset0 + identOffset, param
						.getName().length());
				return new IJavaElement[] { 
						createLocalVariableSourceCode(param.getName(), param.getType().getName(), region, facade) };
			}
		} catch (BadLocationException e) {
			GroovyPlugin.getDefault().logException("Should not happen", e);
		}
		return NONE;
	}

    private IJavaElement createLocalVariableSourceCode(String name, String typeName,
            IRegion region, IDocumentFacade facade) {
        int start = region.getOffset();
        int end = region.getOffset() + region.getLength()-1;
        ICompilationUnit unit = JavaCore.createCompilationUnitFrom(facade.getFile());
        JavaElement parent;
        try {
            parent = (JavaElement) unit.getElementAt(region.getOffset());
        } catch (JavaModelException e) {
            GroovyCore.logException("", e);
            parent = (JavaElement) unit;
        }
        LocalVariable var = new LocalVariable(
                parent, 
                name, 
                start,  
                end, 
                start, 
                end, 
                Signature.createTypeSignature(typeName, false), // XXX This is not a long term solutino
                new Annotation[0]);
        return var;
    }
    
    /**
     * Given some text, find the offset to the first match of some identifier in
     * the text.
     * 
     * @param text
     * @param identifier
     * @return The offset, or -1 if there is no match.
     */
    private int findIdentifierOffset(String text, String identifier) {
        String notIdent = "[^a-zA-Z0-9_]";

        Pattern pattern = Pattern.compile("(^|" + notIdent + ")(" + identifier + ")("
                                + notIdent + "|$)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.start(2);
        }
        
        return -1;
    }

}