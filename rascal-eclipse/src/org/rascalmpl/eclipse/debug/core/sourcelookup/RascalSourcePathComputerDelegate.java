/*******************************************************************************
 * Copyright (c) 2009-2012 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Emilie Balland - (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
*******************************************************************************/
package org.rascalmpl.eclipse.debug.core.sourcelookup;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.rascalmpl.eclipse.IRascalResources;
import org.rascalmpl.eclipse.nature.ProjectEvaluatorFactory;
import org.rascalmpl.interpreter.Evaluator;
import io.usethesource.vallang.ISourceLocation;

public class RascalSourcePathComputerDelegate implements ISourcePathComputerDelegate {

  /* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourcePathComputerDelegate#computeSourceContainers(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
	    String project = configuration.getAttribute(IRascalResources.ATTR_RASCAL_PROJECT, (String) null);
		
		if (project != null) {
			IProject associatedProject = ResourcesPlugin.getWorkspace().getRoot().getProject(project);
			Evaluator eval = ProjectEvaluatorFactory.getInstance().getEvaluator(associatedProject);
			List<ISourceLocation> path = eval.getRascalResolver().collect();
			ISourceContainer[] result = new ISourceContainer[path.size() + 1];
			
			int i = 0;
			for (ISourceLocation elem : path) {
				result[i++] = new URISourceContainer(elem);
			}
			
			result[i++] = new DummyConsoleSourceContainer();
			
			return result;
		} else {
			/* default case */
			return new ISourceContainer[]{new DummyConsoleSourceContainer()};			
		}
	}
}
