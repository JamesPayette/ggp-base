package jack.A7;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
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

public class PropNetStateMachine extends StateMachine {
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
			roles = propNet.getRoles();
			ordering = getOrdering();
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
		PropNetMachineState propNetState = (PropNetMachineState)state;
		markState(propNetState);
		return propNetState.isTerminal();
	}

	/**
	 * Computes the goal for a role in the current state. Should return the value of
	 * the goal proposition that is true for that role. If there is not exactly one
	 * goal proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined.
	 */
	@Override
	public synchronized int getGoal(MachineState state, Role role) throws GoalDefinitionException {
		PropNetMachineState propNetState = (PropNetMachineState)state;
		markState(propNetState);
		return propNetState.getGoalValue(role);
	}

	/**
	 * Returns the initial state. The initial state can be computed by only setting
	 * the truth value of the INIT proposition to true, and then computing the
	 * resulting state.
	 */
	@Override
	public synchronized MachineState getInitialState() {
		resetMachine();
		propNet.getInitProposition().setValue(true);
		markPropNet();
		return getStateFromBase();
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
		PropNetMachineState propNetState = (PropNetMachineState)state;
		markState(propNetState);
		return propNetState.getLegalMoves(role);
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public synchronized MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException {
		resetMachine();
		markActions(moves);
		markBases(state);
		markPropNet();
		MachineState next = getStateFromBase();
		if (next == null) throw new TransitionDefinitionException(state, moves);
		return next;
	}

	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	private List<Proposition> getOrdering() {
		List<Proposition> order = new TopologicalSorter(propNet).sort();
		return order;
	}

	private int getGoalValue(Proposition goalProposition) {
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	private MachineState getStateFromBase() {
		Collection<Proposition> bases = new HashSet<Proposition>();
		for(Proposition p : propNet.getBasePropositions().values()) {
			p.setValue(p.getSingleInput().getValue());
			bases.add(p);
		}
		return new PropNetMachineState(bases);
	}

	private void markState(PropNetMachineState state) {
		if (state.isMarked()) return;

		resetMachine();
		markBases(state);
		markPropNet();

		Map<Role, List<Move>> legalMoves = new HashMap<Role, List<Move>>();
		Map<Role, Integer> goalValues = new HashMap<Role, Integer>();
		for (Role role : roles) {
			// Get legal moves for each role
			List<Move> legals = new ArrayList<Move>();
			Set<Proposition> legalProps = propNet.getLegalPropositions().get(role);
			for (Proposition lp : legalProps) {
				if (lp.getValue()) {
					Proposition ip = propNet.getLegalInputMap().get(lp);
					GdlSentence sentence = ip.getName();
					legals.add(new Move(sentence.get(1)));
				}
			}
			if (legals.size() > 0) {
				legalMoves.put(role, legals);
			}

			// Get goal values for each role
			Set<Proposition> gps = propNet.getGoalPropositions().get(role);
			Proposition goal = null;
			boolean error = false;
			for (Proposition gp : gps) {
				if (gp.getValue()) {
					if (goal != null) {
						error = true;
						break;
					}
					goal = gp;
				}
			}
			if (goal != null && !error) {
				goalValues.put(role, getGoalValue(goal));
			}
		}
		state.setLegalMoves(legalMoves);
		state.setGoalValues(goalValues);

		// Get terminal value
		state.setTerminal(propNet.getTerminalProposition().getValue());

		state.setMarked(true);
	}

	private void markBases(MachineState state) {
		PropNetMachineState pState = (PropNetMachineState) state;
		for (Proposition p : propNet.getBasePropositions().values()) {
			p.setValue(pState.get(p));
		}
	}

	private void resetMachine() {
		for(Proposition p : propNet.getPropositions()) {
			p.setValue(false);
		}
	}

	private void markActions(List<Move> moves) {
		Set<Proposition> inputs = getInputPropositions(moves);
		for (Proposition p : propNet.getInputPropositions().values()) {
			p.setValue(inputs.contains(p));
		}
	}

	private void markPropNet() {
		for(Proposition p : ordering) {
			p.setValue(p.getSingleInput().getValue());
		}
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

}
