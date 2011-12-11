package org.rascalmpl.eclipse.ambidexter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.rascalmpl.eclipse.nature.ProjectEvaluatorFactory;
import org.rascalmpl.interpreter.Evaluator;
import org.rascalmpl.interpreter.TypeReifier;
import org.rascalmpl.interpreter.env.Environment;
import org.rascalmpl.interpreter.env.ModuleEnvironment;
import org.rascalmpl.interpreter.types.RascalTypeFactory;

public abstract class AbstractAmbidexterAction extends Action implements SelectionListener  {
	protected String sentence;
	protected IConstructor nonterminal;
	protected String project;
	protected String module;

	public AbstractAmbidexterAction() {
		super();
	}

	public AbstractAmbidexterAction(String text) {
		super(text);
	}
	
	@Override
	abstract public void run();

	public AbstractAmbidexterAction(String text, ImageDescriptor image) {
		super(text, image);
	}

	public AbstractAmbidexterAction(String text, int style) {
		super(text, style);
	}

	@Override
	public void widgetSelected(SelectionEvent event) {
		Object selection = event.getSource();
	    
	    if (selection != null && selection instanceof Table) {
	    	Table table = (Table) selection;
	    	for (TableItem item : table.getSelection()) {
	    	    this.sentence = (String) item.getData("sentence");
	    		this.nonterminal = (IConstructor) item.getData("nonterminal");
	    		this.project = (String) item.getData("project");
	    		this.module = (String) item.getData("module");
	    	}
	    }
		
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		widgetSelected(e);
	}

	protected Evaluator getEvaluator() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(this.project);
		
		if (project == null) {
			throw new IllegalArgumentException(this.project);
		}
		
		return ProjectEvaluatorFactory.getInstance().getEvaluator(project);
	}
	
	protected IConstructor parse() {
		Evaluator eval = getEvaluator();
		
		ModuleEnvironment env = eval.getHeap().getModule(module);
		
		if (env == null) {
			throw new IllegalArgumentException(module);
		}
		
		Environment old = eval.getCurrentEnvt();
		try {
			eval.setCurrentEnvt(env);
			eval.pushEnv();
			Type nt = RascalTypeFactory.getInstance().nonTerminalType(nonterminal);
			IValue reified = nt.accept(new TypeReifier(eval, eval.getValueFactory())).getValue();
			return (IConstructor) eval.call("parse", reified, eval.getValueFactory().string(sentence));
		}
		finally {
			eval.unwind(old);
		}
	}
}