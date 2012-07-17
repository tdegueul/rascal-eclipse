/*******************************************************************************
 * Copyright (c) 2009-2012 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Tijs van der Storm - Tijs.van.der.Storm@cwi.nl
 *   * Emilie Balland - (CWI)
 *   * Arnold Lankamp - Arnold.Lankamp@cwi.nl
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
*******************************************************************************/
package org.rascalmpl.eclipse.console;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IHyperlink;
import org.rascalmpl.eclipse.Activator;
import org.rascalmpl.eclipse.console.internal.IInterpreterConsole;
import org.rascalmpl.eclipse.console.internal.InteractiveInterpreterConsole;
import org.rascalmpl.eclipse.console.internal.StdAndErrorViewPart;
import org.rascalmpl.interpreter.AbstractInterpreterEventTrigger;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.IInterpreterEventListener;
import org.rascalmpl.interpreter.debug.DebugHandler;
import org.rascalmpl.interpreter.debug.DebuggableEvaluator;
import org.rascalmpl.interpreter.debug.IDebugHandler;
import org.rascalmpl.interpreter.env.GlobalEnvironment;
import org.rascalmpl.interpreter.env.ModuleEnvironment;
import org.rascalmpl.interpreter.utils.ReadEvalPrintDialogMessages;
import org.rascalmpl.values.ValueFactoryFactory;

import static org.rascalmpl.interpreter.AbstractInterpreterEventTrigger.*;

public class ConsoleFactory{
	public final static String INTERACTIVE_CONSOLE_ID = InteractiveInterpreterConsole.class.getName();
	private final static String SHELL_MODULE = "$shell$";

	private final static IValueFactory vf = ValueFactoryFactory.getValueFactory();
	private final static IConsoleManager fConsoleManager = ConsolePlugin.getDefault().getConsoleManager();

	private static PrintWriter getErrorWriter() {
		try {
			OutputStream err = StdAndErrorViewPart.getStdErr(); 
			return new PrintWriter(new OutputStreamWriter(err, "UTF16"),true);
		} catch (UnsupportedEncodingException e) {
			Activator.getInstance().logException("could not get stderr writer", e);
			return new PrintWriter(System.err);
		}
	}
	
	private static PrintWriter getStandardWriter() {
		try {
			OutputStream out = StdAndErrorViewPart.getStdOut(); 
			return new PrintWriter(new OutputStreamWriter(out, "UTF16"));
		} catch (UnsupportedEncodingException e) {
			Activator.getInstance().logException("could not get stdout writer", e);
			return new PrintWriter(System.out);
		}
	}
	
	public ConsoleFactory(){
		super();
	}

	private static class InstanceKeeper{
		private final static ConsoleFactory instance = new ConsoleFactory();
	}

	public static ConsoleFactory getInstance(){
		return InstanceKeeper.instance;
	}
	
	public IRascalConsole openRunConsole(){
		Activator.getInstance().checkRascalRuntimePreconditions();
		GlobalEnvironment heap = new GlobalEnvironment();
		IRascalConsole console = new InteractiveRascalConsole(new ModuleEnvironment(SHELL_MODULE, heap), heap);
		fConsoleManager.addConsoles(new IConsole[]{console});
		fConsoleManager.showConsoleView(console);
		return console;
	}
	
	public IRascalConsole openRunConsole(IProject project){
		Activator.getInstance().checkRascalRuntimePreconditions(project);
		GlobalEnvironment heap = new GlobalEnvironment();
		IRascalConsole console = new InteractiveRascalConsole(project, new ModuleEnvironment(SHELL_MODULE, heap), heap);
		fConsoleManager.addConsoles(new IConsole[]{console});
		fConsoleManager.showConsoleView(console);
		return console;
	}
	
	public IRascalConsole openDebuggableConsole(){
		GlobalEnvironment heap = new GlobalEnvironment();
		IRascalConsole console = new InteractiveRascalConsole(new ModuleEnvironment(SHELL_MODULE, heap), heap, true);
		fConsoleManager.addConsoles(new IConsole[]{console});
		fConsoleManager.showConsoleView(console);
		return console;
	}
	
	public IRascalConsole openDebuggableConsole(IProject project){
		GlobalEnvironment heap = new GlobalEnvironment();
		IRascalConsole console = new InteractiveRascalConsole(project, new ModuleEnvironment(SHELL_MODULE, heap), heap, true);
		fConsoleManager.addConsoles(new IConsole[]{console});
		fConsoleManager.showConsoleView(console);
		return console;
	}

	public interface IRascalConsole extends IInterpreterConsole {
		void activate(); // Eclipse thing.
		RascalScriptInterpreter getRascalInterpreter();
		IDocument getDocument();
		IDebugHandler getDebugHandler();
		AbstractInterpreterEventTrigger getEventTrigger();
	    void addHyperlink(IHyperlink hyperlink, int offset, int length) throws BadLocationException;
	}

	private class InteractiveRascalConsole extends InteractiveInterpreterConsole implements IRascalConsole{	
		
		private final AbstractInterpreterEventTrigger eventTrigger = newInterpreterEventTrigger(this, new CopyOnWriteArrayList<IInterpreterEventListener>());
		
		private final DebugHandler debugHandler;
		
		// TODO: unify all four constructors		
		public InteractiveRascalConsole(ModuleEnvironment shell, GlobalEnvironment heap){
			super(new RascalScriptInterpreter(), "Rascal", ReadEvalPrintDialogMessages.PROMPT, ReadEvalPrintDialogMessages.CONTINUE_PROMPT);
			
			this.debugHandler = null;
			
			getRascalInterpreter().setEventTrigger(eventTrigger);
			
			Evaluator evaluator = new Evaluator(vf, getErrorWriter(), getStandardWriter(), shell, heap);
			evaluator.setEventTrigger(eventTrigger);
			
			getInterpreter().initialize(evaluator);
			initializeConsole();
		}
		
		/* 
		 * console associated to a given Eclipse project 
		 * used to initialize the path with modules accessible 
		 * from the selected project and all its referenced projects
		 * */
		// TODO: unify all four constructors		
		public InteractiveRascalConsole(IProject project, ModuleEnvironment shell, GlobalEnvironment heap){
			super(new RascalScriptInterpreter(project), "Rascal ["+project.getName()+"]", ReadEvalPrintDialogMessages.PROMPT, ReadEvalPrintDialogMessages.CONTINUE_PROMPT);
			
			this.debugHandler = null;
			
			getRascalInterpreter().setEventTrigger(eventTrigger);
			
			Evaluator evaluator = new Evaluator(vf, getErrorWriter(), getStandardWriter(), shell, heap);
			evaluator.setEventTrigger(eventTrigger);
			
			getInterpreter().initialize(evaluator);
			initializeConsole();
		}

		// TODO: unify all four constructors
		public InteractiveRascalConsole(ModuleEnvironment shell, GlobalEnvironment heap, boolean debugDummy){
			super(new RascalScriptInterpreter(), "Rascal [DEBUG]", ReadEvalPrintDialogMessages.PROMPT, ReadEvalPrintDialogMessages.CONTINUE_PROMPT);

			this.debugHandler = new DebugHandler();
			this.debugHandler.setEventTrigger(eventTrigger);
			// add termination action to debugging handler
			this.debugHandler.setTerminateAction(new Runnable() {
				@Override
				public void run() {
					terminate();
				}
			});			

			getRascalInterpreter().setEventTrigger(eventTrigger);
			
			DebuggableEvaluator evaluator = new DebuggableEvaluator(vf, getErrorWriter(), getStandardWriter(), shell, heap);
			evaluator.setEventTrigger(eventTrigger);
			evaluator.addSuspendTriggerListener(debugHandler);
						
			getInterpreter().initialize(evaluator);
			initializeConsole();
		}
		
		// TODO: unify all four constructors
		public InteractiveRascalConsole(IProject project, ModuleEnvironment shell, GlobalEnvironment heap, boolean debugDummy){
			super(new RascalScriptInterpreter(project), "Rascal [DEBUG, "+project.getName()+"]", ReadEvalPrintDialogMessages.PROMPT, ReadEvalPrintDialogMessages.CONTINUE_PROMPT);
			
			this.debugHandler = new DebugHandler();
			this.debugHandler.setEventTrigger(eventTrigger);
			// add termination action to debugging handler
			this.debugHandler.setTerminateAction(new Runnable() {
				@Override
				public void run() {
					terminate();
				}
			});			

			getRascalInterpreter().setEventTrigger(eventTrigger);			
			
			DebuggableEvaluator evaluator = new DebuggableEvaluator(vf, getErrorWriter(), getStandardWriter(), shell, heap);
			evaluator.setEventTrigger(eventTrigger);
			evaluator.addSuspendTriggerListener(debugHandler);
			
			getInterpreter().initialize(evaluator);
			initializeConsole();
		}
		
		@Override
		public RascalScriptInterpreter getRascalInterpreter(){
			return (RascalScriptInterpreter) getInterpreter();
		}

		@Override
		public IDebugHandler getDebugHandler() {
			return debugHandler;
		}
		
		@Override
		public AbstractInterpreterEventTrigger getEventTrigger() {
			return eventTrigger;
		}
		
	}

}
