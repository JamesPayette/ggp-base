package jack.A4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSNode {
	private static final int COUNT = 30;
	private StateMachine machine;
	private MachineState state;
	private MCTSNode parent;
	private Map<Role, List<Move>> legalMap;
	private Map<Role, List<Integer>> utilityMap;
	private Map<Role, List<Integer>> countMap;
	private Map<List<Move>, MCTSNode> children;

	public MCTSNode(StateMachine machine, MachineState state, MCTSNode parent)
			throws MoveDefinitionException {
		this.machine = machine;
		this.state = state;
		this.parent = parent;
		legalMap = new HashMap<Role, List<Move>>();
		utilityMap = new HashMap<Role, List<Integer>>();
		countMap = new HashMap<Role, List<Integer>>();
		for (Role r : machine.getRoles()) {
			List<Move> legals;
			if (machine.findTerminalp(state)) {
				legals = new ArrayList<Move>();
			} else {
				legals = machine.findLegals(r, state);
			}
			legalMap.put(r, legals);
			utilityMap.put(r, new ArrayList<Integer>(Collections.nCopies(legals.size(),0)));
			countMap.put(r, new ArrayList<Integer>(Collections.nCopies(legals.size(),0)));
		}
		children = new HashMap<List<Move>, MCTSNode>();
	}

	public MCTSNode select()
			throws TransitionDefinitionException, MoveDefinitionException {
		if (machine.findTerminalp(state)) {
			return this;
		}
		for (List<Move> jointMove : machine.getLegalJointMoves(state)) {
			if (!children.containsKey(jointMove)) {
				MachineState nextState = machine.findNext(jointMove, state);
				MCTSNode child = new MCTSNode(machine, nextState, this);
				children.put(jointMove, child);
				return child;
			}
		}
		List<Move> selectedMove = new ArrayList<Move>();
		for (Role r : machine.getRoles()) {
			selectedMove.add(selectMoveForRole(r));
		}
		return children.get(selectedMove).select();
	}

	private Move selectMoveForRole(Role role) {
		List<Move> legals = legalMap.get(role);
		List<Integer> utilities = utilityMap.get(role);
		List<Integer> counts = countMap.get(role);
		return selectMove(legals, utilities, counts);
	}

	private Move selectMove(List<Move> legals, List<Integer> utilities, List<Integer> counts) {
		Move bestMove = null;
		double bestScore = 0.0;
		for (int i = 0; i < legals.size(); i ++) {
			double score = selectFn(utilities, counts, i);
			if (bestMove == null || score > bestScore) {
				bestMove = legals.get(i);
				bestScore = score;
			}
		}
		return bestMove;
	}

	private double selectFn(List<Integer> utilities, List<Integer> counts, int index) {
		int parentVisits = getSum(counts);
		double exploitation = utilities.get(index) / (double) counts.get(index);
		double exploration = Math.sqrt(2 * Math.log(parentVisits) / counts.get(index));
		return exploitation + exploration;
	}

	private int getSum(List<Integer> counts) {
		int sum = 0;
		for (Integer i : counts) sum += i;
		return sum;
	}

	public Map<Role, Integer> simulate()
			throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		Map<Role, Integer> rewards = new HashMap<Role, Integer>();
		for (int i = 0; i < COUNT; i++) {
			MachineState finalState = findRandomFinalState();
			for (Role r : machine.findRoles()) {
				rewards.put(r, rewards.getOrDefault(r, 0) + machine.getGoal(finalState, r));
			}
		}
		for (Role r : machine.findRoles()) {
			rewards.put(r, (int) (rewards.get(r) / (double) COUNT));
		}
		return rewards;
	}

	public void backpropagate(Map<Role, Integer> rewards) {
		if (parent == null) return;
		List<Move> jointMove = parent.getJointMove(this);
		List<Role> roles = machine.getRoles();
		for(int i = 0; i < roles.size(); i++) {
			Role role = roles.get(i);
			Move move = jointMove.get(i);
			int reward = rewards.get(role);
			parent.updateUtility(role, move, reward);
			parent.updateCount(role, move);
		}
		parent.backpropagate(rewards);
	}

	private List<Move> getJointMove(MCTSNode node) {
		for (List<Move> jointMove : children.keySet()) {
			if (children.get(jointMove).equals(node)) return jointMove;
		}
		return null;
	}

	public MCTSNode findNodeByState(MachineState state) {
		if (state.equals(this.state)) {
			return this;
		}
		for (MCTSNode node : children.values()) {
			MCTSNode found = node.findNodeByState(state);
			if (found != null) return found;
		}
		return null;
	}

	public void setRoot() {
		parent = null;
	}

	private void updateUtility(Role role, Move move, int reward) {
		int moveIndex = getMoveIndex(legalMap.get(role), move);
		List<Integer> utilityList = utilityMap.get(role);
		utilityList.set(moveIndex, utilityList.get(moveIndex) + reward);
	}

	private int getMoveIndex(List<Move> legals, Move move) {
		for(int i = 0; i < legals.size(); i++) {
			if(legals.get(i).equals(move)) return i;
		}
		System.out.println("Move not found in legal moves, returning 0");
		return 0;
	}

	private void updateCount(Role role, Move move) {
		int moveIndex = getMoveIndex(legalMap.get(role), move);
		List<Integer> countList = countMap.get(role);
		countList.set(moveIndex, countList.get(moveIndex) + 1);
	}

	public Move findBestChild(Role role) {
		List<Move> legals = legalMap.get(role);
		List<Integer> utilites = utilityMap.get(role);
		List<Integer> counts = countMap.get(role);
		int bestIndex = 0;
		double bestScore = 0.0;
		for (int i = 0; i < legals.size(); i ++) {
			if (counts.get(i) > 0) {
				double score = utilites.get(i) / (double) counts.get(i);
				System.out.println(legals.get(i) + " : " + score);
				if (score > bestScore) {
					bestScore = score;
					bestIndex = i;
				}
			}
		}
		return legals.get(bestIndex);
	}

	private MachineState findRandomFinalState()
			throws TransitionDefinitionException, MoveDefinitionException {
		MachineState randState = state;
		while(!machine.findTerminalp(randState)) {
			randState = machine.getNextState(randState, machine.getRandomJointMove(randState));
	    }
		return randState;
	}

}
