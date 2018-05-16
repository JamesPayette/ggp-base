package jack.A7;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;

public class TopologicalSorter {
	private Set<Proposition> baseProps;
	private Set<Proposition> excludedProps;
	private List<Component> unMarked;
	private Set<Component> tempMarked;
	private Set<Component> permMarked;
	private List<Proposition> order;


	public TopologicalSorter(PropNet net) {
		excludedProps = new HashSet<Proposition>();
		excludedProps.addAll(net.getBasePropositions().values());
		excludedProps.addAll(net.getInputPropositions().values());
		excludedProps.add(net.getInitProposition());
		baseProps = new HashSet<Proposition>(net.getBasePropositions().values());
		unMarked = new LinkedList<Component>(net.getComponents());
		tempMarked = new HashSet<Component>();
		permMarked = new HashSet<Component>();
		order = new LinkedList<Proposition>();
	}

	public List<Proposition> sort() {
		while(!unMarked.isEmpty()) {
			Component c = unMarked.get(0);
			visit(c);
		}
		return order;
	}

	private void visit(Component c) {
		if(permMarked(c)) return;
		if(tempMarked(c)) {
			throw new RuntimeException("Not A DAG...");
		}
		tempMark(c);
		for(Component cPrime : c.getOutputs()) {
			if(shouldPropagate(cPrime)) visit(cPrime);
		}
		permMark(c);
		if (propositionToAdd(c)) order.add(0, (Proposition) c);
	}

	private void tempMark(Component c) {
		unMarked.remove(c);
		tempMarked.add(c);
	}

	private void permMark(Component c) {
		tempMarked.remove(c);
		permMarked.add(c);
	}

	private boolean tempMarked(Component c) {
		return tempMarked.contains(c);
	}

	private boolean permMarked(Component c) {
		return permMarked.contains(c);
	}

	private boolean propositionToAdd(Component c) {
		return ((c instanceof Proposition) && !excludedProps.contains((Proposition) c));
	}

	private boolean shouldPropagate(Component c) {
		return !(c instanceof Transition);
	}

}
