package jack.A7;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;

public class DiffPropPropNetStateMachine extends StateMachine {
	/** The underlying proposition network */
	private PropNet propNet;
	/** The topological ordering of the propositions */
	private List<Proposition> ordering;
	/** The player roles */
	private List<Role> roles;
	/**
	 * Initializes the PropNetStateMachine. You should compute the topological
	 * ordering here. Additionally you may compute the initial state here, at your
	 * discretion.
	 */

	@Override
	public synchronized void initialize(List<Gdl> description) {
		try {
			propNet = OptimizingPropNetFactory.create(description);
			ordering = getOrdering();
			roles = propNet.getRoles();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Computes if the state is terminal. Should return the value of the terminal
	 * proposition for the state.
	 */
	@Override
	public synchronized boolean isTerminal(MachineState state) {
		Queue<Component> toMark = new LinkedList<Component>();
		clearActions(toMark);
		markBases(state, toMark);
		propagate(toMark);
		return propNet.getTerminalProposition().getValue();
	}

	/**
	 * Computes the goal for a role in the current state. Should return the value of
	 * the goal proposition that is true for that role. If there is not exactly one
	 * goal proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined.
	 */
	@Override
	public synchronized int getGoal(MachineState state, Role role) throws GoalDefinitionException {
		Queue<Component> toMark = new LinkedList<Component>();
		clearActions(toMark);
		markBases(state, toMark);
		propagate(toMark);
		Set<Proposition> gps = propNet.getGoalPropositions().get(role);
		Proposition goal = null;
		for (Proposition gp : gps) {
			if (gp.getValue()) {
				if (goal != null) throw new GoalDefinitionException(state, role);
				goal = gp;
			}
		}
		if (goal == null) throw new GoalDefinitionException(state, role);
		return getGoalValue(goal);
	}

	/**
	 * Returns the initial state. The initial state can be computed by only setting
	 * the truth value of the INIT proposition to true, and then computing the
	 * resulting state.
	 */
	@Override
	public synchronized MachineState getInitialState() {
		Queue<Component> toMark = new LinkedList<Component>();
		resetMachine(toMark);
		markInitProp(true, toMark);
		propagate(toMark);
		toMark = new LinkedList<Component>();
		MachineState nextState = getStateFromBase(toMark);
		markInitProp(false, toMark);
		propagate(toMark);
		return nextState;
	}

	/**
	 * Computes all possible actions for role.
	 */
	@Override
	public synchronized List<Move> findActions(Role role) throws MoveDefinitionException {
		List<Move> legals = new ArrayList<Move>();
		Set<Proposition> legalProps = propNet.getLegalPropositions().get(role);
		for (Proposition lp : legalProps) {
			Proposition ip = propNet.getLegalInputMap().get(lp);
			GdlSentence sentence = ip.getName();
			legals.add(new Move(sentence.get(1)));
		}
		if (legals.size() == 0) throw new MoveDefinitionException(getInitialState(), role);
		return legals;
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public synchronized List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException {
		Queue<Component> toMark = new LinkedList<Component>();
		clearActions(toMark);
		markBases(state, toMark);
		propagate(toMark);
		List<Move> legals = new ArrayList<Move>();
		Set<Proposition> legalProps = propNet.getLegalPropositions().get(role);
		for (Proposition lp : legalProps) {
			if (lp.getValue()) {
				Proposition ip = propNet.getLegalInputMap().get(lp);
				GdlSentence sentence = ip.getName();
				legals.add(new Move(sentence.get(1)));
			}
		}
		if (legals.size() == 0) throw new MoveDefinitionException(state, role);
		return legals;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public synchronized MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException {
		Queue<Component> toMark = new LinkedList<Component>();
		markActions(moves, toMark);
		markBases(state, toMark);
		propagate(toMark);
		toMark = new LinkedList<Component>();
		MachineState next = getStateFromBase(toMark);
		propagate(toMark);
		if (next == null) throw new TransitionDefinitionException(state, moves);
		return next;
	}

	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	private MachineState getStateFromBase(Queue<Component> toMark) {
		Collection<Proposition> bases = new HashSet<Proposition>();
		for(Proposition p : propNet.getBasePropositions().values()) {
			boolean newValue = p.getSingleInput().getValue();
			boolean oldValue = p.getValue();
			bases.add(p);
			if(oldValue != newValue) {
				p.setValue(newValue);
				addComponentsToQueue(p, toMark);
			}
		}
		return new PropNetMachineState(bases);
	}

	private void markBases(MachineState state, Queue<Component> toMark) {
		PropNetMachineState pState = (PropNetMachineState) state;
		for (Proposition p : propNet.getBasePropositions().values()) {
			boolean newValue = pState.get(p);
			boolean oldValue = p.getValue();
			if(oldValue != newValue) {
				p.setValue(newValue);
				addComponentsToQueue(p, toMark);
			}
		}
	}

	private void resetMachine(Queue<Component> toMark) {
		for(Proposition p : propNet.getPropositions()) {
			boolean newValue = false;
			boolean oldValue = p.getValue();
			if(oldValue != newValue) {
				p.setValue(newValue);
				addComponentsToQueue(p, toMark);
			}
		}
	}

	private void markActions(List<Move> moves, Queue<Component> toMark) {
		Set<Proposition> inputs = getInputPropositions(moves);
		for (Proposition p : propNet.getInputPropositions().values()) {
			boolean newValue = inputs.contains(p);
			boolean oldValue = p.getValue();
			if(oldValue != newValue) {
				p.setValue(newValue);
				addComponentsToQueue(p, toMark);
			}
		}
	}

	private void clearActions(Queue<Component> toMark) {
		for (Proposition p : propNet.getInputPropositions().values()) {
			boolean newValue = false;
			boolean oldValue = p.getValue();
			if(oldValue != newValue) {
				p.setValue(newValue);
				addComponentsToQueue(p, toMark);
			}
		}
	}

	private void markInitProp(boolean newValue, Queue<Component> toMark) {
		boolean oldValue = propNet.getInitProposition().getValue();
		if (newValue != oldValue) {
			propNet.getInitProposition().setValue(newValue);
			addComponentsToQueue(propNet.getInitProposition(), toMark);
		}
	}

	private List<Proposition> getOrdering() {
		List<Proposition> order = new TopologicalSorter(propNet).sort();
		return order;
	}

	private int getGoalValue(Proposition goalProposition) {
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	private Set<Proposition> getInputPropositions(List<Move> moves) {
		Set<Proposition> inputProps = new HashSet<Proposition>();
		Map<Role, Integer> roleIndices = getRoleIndices();
		for (int i = 0; i < roles.size(); i++) {
			int index = roleIndices.get(roles.get(i));
			GdlSentence key = ProverQueryBuilder.toDoes(roles.get(i), moves.get(index));
			Proposition inputProp = propNet.getInputPropositions().get(key);
			inputProps.add(inputProp);
		}
		return inputProps;
	}

	private void addComponentsToQueue(Component c, Queue<Component> toMark) {
		for(Component cPrime : c.getOutputs()) {
			toMark.add(cPrime);
		}
	}

	// TODO: Somehow enforce the topological ordering.
	// We need to say: Whenever we get to a node, if there's a node in the ordering
	// in front of it in the queue, the move it to the back of the queue.
	// Any naive approach to this is going to be super slow though...
	private void propagate(Queue<Component> toMark) {
		if (toMark.isEmpty()) return;
		Component c = toMark.remove();
		if (c instanceof Proposition) {
			Proposition p = (Proposition) c;
			boolean newValue = p.getSingleInput().getValue();
			boolean oldValue = p.getValue();
			if(newValue != oldValue) {
				p.setValue(newValue);
				addComponentsToQueue(p, toMark);
			}
		} else {
			addComponentsToQueue(c, toMark);
		}
		propagate(toMark);
	}

}