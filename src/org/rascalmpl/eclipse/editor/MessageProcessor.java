package org.rascalmpl.eclipse.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.imp.parser.IMessageHandler;
import org.eclipse.imp.parser.IModelListener;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.values.uptr.TreeAdapter;

public class MessageProcessor {

	public void process(final IConstructor parseTree, final IParseController parseController, final IProgressMonitor monitor) {
			IPath path = parseController.getPath();
			IMessageHandler handler = ((IMessageHandlerProvider) parseController).getMessageHandler();
			
			monitor.beginTask("Marking errors and warnings in " + path, 1);
			if (parseTree != null) {
				processMarkers(parseTree, handler); 
				handler.endMessages();
			}
	}
	
	private void processMarkers(IConstructor tree, IMessageHandler handler) {
		if (TreeAdapter.isAppl(tree) && !TreeAdapter.isLexical(tree)) {
			IValue anno = tree.getAnnotation("message");
			if (anno != null && anno.getType().isAbstractDataType() && anno.getType().getName().equals("Message")) {
				IConstructor marker = (IConstructor) anno;
				ISourceLocation loc = TreeAdapter.getLocation(tree);
				processMarker(marker, loc, handler);
			}
			
			anno = tree.getAnnotation("messages");
			if (anno != null && anno.getType().isSetType()) {
				Type elemType = anno.getType().getElementType();
				
				if (elemType.isAbstractDataType() && elemType.getName().equals("Message")) {
					
					for (IValue message : ((ISet) anno)) {
						IConstructor marker = (IConstructor) message;
						ISourceLocation loc = (ISourceLocation) marker.get(1);
						ISourceLocation treeLoc = (ISourceLocation) tree.getAnnotation("loc");
						if (loc == null) {
							loc = treeLoc;
						}
						if (loc.getURI().getPath().equals(treeLoc.getURI().getPath()))
							processMarker(marker, loc, handler);
					}
				}
				
				// we do not recurse if we found messages
				return;
			}
			
			for (IValue child : TreeAdapter.getArgs(tree)) {
				processMarkers((IConstructor) child, handler);
			}
		}
		else if (TreeAdapter.isAmb(tree)) {
			for (IValue alt : TreeAdapter.getAlternatives(tree)) {
				processMarkers((IConstructor) alt, handler);
			}
		}
	}

	private void processMarker(IConstructor marker, ISourceLocation loc, IMessageHandler handler)  {
		int severity = IStatus.INFO;

		if (marker.getName().equals("error")) {
			severity = IStatus.ERROR;
		}
		else if (marker.getName().equals("warning")) {
			severity = IStatus.WARNING;
		}

		String msg = ((IString) marker.get(0)).getValue();
		Map<String,Object> attrs = new HashMap<String,Object>();
		attrs.put(IMarker.SEVERITY, severity);
		attrs.put(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);

		handler.handleSimpleMessage(msg, loc.getOffset(), loc.getOffset() + loc.getLength(), loc.getBeginColumn(), loc.getEndColumn(), loc.getBeginLine(), loc.getEndLine(), attrs);
	}

	public int compareTo(IModelListener o) {
		return 0;
	}
}