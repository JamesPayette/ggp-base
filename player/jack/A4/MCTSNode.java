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

/*
 * This class does the heavy lifting for Monte Carlo Tree Search.  It masks much of the complexity of
 * the MCTS process, but is admittedly complex. The main methods here are select() (which houses expansion),
 * simulate(), backpropagate(), and findBestChild().  The implementation makes the assumption that every player
 * will make the that he (based on his own current statistics) believes will be his best move.
 *
 * TODO: Check if the rampant use of doubles is necessary/helps.  Didn't want to worry about int overflow
 * and the loss of precision in the low level int divisions, but unclear.
 */

public class MCTSNode {

	// A constant, the number of depth charges to use if there are no running statistics
	private static final int COUNT = 4;

	// A class variable, maps machine states to MCTSNodes. We use this to "unify" parts of the tree
	// if two paths lead to the same state.  It also makes lookup for changing the root node easier
	// TODO: Verify that the tree is acyclic.  If not, this could cause infinite lopps
	private static Map<MachineState, MCTSNode> stateMap = new HashMap<MachineState, MCTSNode>();
	// These class variables keep the running statistics for depth charges.  They allow us to make
	// informed decisions on how many depth charges to run on each iteration.  This is necessary, as some
	// games have much costlier depth charges than others.
	private static int numDepthCharges = 0;
	private static double runningAverage = 0.0;

	// game-wide state machine, helpful to have a reference in the node
	private StateMachine machine;
	// state for which this node corresponds.  Under the acyclic assumption, there is a direct mapping from
	// state to node
	private MachineState state;
	// A node can have multiples parents if multiple states lead to the same place
	// TODO: Check if this even happens (if length is ever greater than 1)
	// An empty list here implies that this is the root node of the working MCTS tree
	private List<MCTSNode> parents;
	// A mapping from each role to its list of possible moves for this state
	private Map<Role, List<Move>> legalMap;
	// A mapping from each role to the list of total utilities for this state.  The indices into this list
	// correspond with the indices of the moves in the legal map.
	private Map<Role, List<Double>> utilityMap;
	// A mapping from each role to the list of counts for this state.  The indices into this list
	// correspond with the indices of the moves in the legal map.
	private Map<Role, List<Double>> countMap;
	// List of children nodes.  It is a map from legal joint moves to the node that results from taking
	// this joint move in the current state
	private Map<List<Move>, MCTSNode> children;

	/*
	 * This method initializes the node.
	 * Important to note is that if this is a terminal state, we set the legal moves to be an
	 * empty list.  This allows the MCTS selection process to stop when we want it to.
	 * All utilities and counts are initialized to 0.
	 */
	public MCTSNode(StateMachine machine, MachineState state) throws MoveDefinitionException {
		this.machine = machine;
		this.state = state;
		parents = new ArrayList<MCTSNode>();
		legalMap = new HashMap<Role, List<Move>>();
		utilityMap = new HashMap<Role, List<Double>>();
		countMap = new HashMap<Role, List<Double>>();
		for (Role r : machine.getRoles()) {
			List<Move> legals;
			if (machine.findTerminalp(state)) {
				legals = new ArrayList<Move>();
			} else {
				legals = machine.findLegals(r, state);
			}
			legalMap.put(r, legals);
			utilityMap.put(r, new ArrayList<Double>(Collections.nCopies(legals.size(), 0.0)));
			countMap.put(r, new ArrayList<Double>(Collections.nCopies(legals.size(), 0.0)));
		}
		children = new HashMap<List<Move>, MCTSNode>();
		stateMap.put(state, this);
	}

	public static void reset() {
		stateMap = new HashMap<MachineState, MCTSNode>();
		numDepthCharges = 0;
		runningAverage = 0.0;
	}

	/*
	 * This method runs the "select" method, which expands the tree if necessary, and then returns
	 * the node from which we'd like to make our depth charges.  Note that we return a node if it's
	 * a terminal state.  This will happen when we get to the end of the tree, and thus the tree stops
	 * but we continue to update statistics.
	 *
	 * We return "this" if the state is terminal.
	 * We then check to see if we've expanded all of the child nodes of this node, which are
	 * all of the legal joint moves.  If any of them are not expanded, we expand them and return them.
	 * If all child nodes are expanded, we select the best move for each role, and use this list as the
	 * "best" joint move from this state.  We get the node corresponding to this joint move from the children
	 * map, and recursively return the select call on this node.
	 */
	public MCTSNode select() throws TransitionDefinitionException, MoveDefinitionException {
		if (machine.findTerminalp(state)) {
			return this;
		}
		for (List<Move> jointMove : machine.getLegalJointMoves(state)) {
			if (!children.containsKey(jointMove)) {
				MachineState nextState = machine.findNext(jointMove, state);
				MCTSNode child = createChild(nextState, jointMove);
				return child;
			}
		}
		List<Move> selectedMove = new ArrayList<Move>();
		for (Role r : machine.getRoles()) {
			selectedMove.add(selectMoveForRole(r));
		}
		return children.get(selectedMove).select();
	}

	/*
	 * This is the method that "expands" the tree.  We first check to see if there already exists a node
	 * corresponding to the state.  If so, we simply get this node from the global map.  Otherwise, we
	 * create a new node with this state.  We then do some housekeeping, adding "this" as a parent to the
	 * node, and adding this newly created node to the children map of "this" with the given jointMove as
	 * the key.
	 * The found/newly created child is returned.
	 */
	private MCTSNode createChild(MachineState nextState, List<Move> jointMove) throws MoveDefinitionException {
		MCTSNode child = findOrCreateNodeByState(nextState);
		child.addParent(this);
		children.put(jointMove, child);
		return child;
	}

	/*
	 * This method selects the best move for a role in this state.  It essentially just fetches the correct
	 * lists and masses them on to a more general method.
	 */
	private Move selectMoveForRole(Role role) {
		List<Move> legals = legalMap.get(role);
		List<Double> utilities = utilityMap.get(role);
		List<Double> counts = countMap.get(role);
		return selectMove(legals, utilities, counts);
	}

	/*
	 * This method selects the best move from a list of legal moves and the utilites and counts
	 * collected thus far.  This uses a select function, which is a combination of exploration
	 * and exploitation.
	 *
	 * TODO: Check the assumption that every player is going to explore/exploit. Is this correct?
	 */
	private Move selectMove(List<Move> legals, List<Double> utilities, List<Double> counts) {
		Move bestMove = null;
		double bestScore = 0.0;
		for (int i = 0; i < legals.size(); i++) {
			double score = selectFn(utilities, counts, i);
			if (bestMove == null || score > bestScore) {
				bestMove = legals.get(i);
				bestScore = score;
			}
		}
		return bestMove;
	}

	/*
	 * This is the chosen select function for our Monte Carlo Tree Search player.  It's the classic MCB
	 * formula, and seems to work pretty well.
	 *
	 * TODO: Check if this is tuneable, and see if this helps.
	 */
	private double selectFn(List<Double> utilities, List<Double> counts, int index) {
		double parentVisits = getSum(counts);
		double exploitation = utilities.get(index) / counts.get(index);
		double exploration = Math.sqrt(2 * Math.log(parentVisits) / counts.get(index));
		return exploitation + exploration;
	}

	/*
	 * Private helper method to find the sum of numbers in a list.  This is how we find the number
	 * of parent visits.
	 */
	private double getSum(List<Double> counts) {
		double sum = 0;
		for (Double i : counts)
			sum += i;
		return sum;
	}

	/*
	 * This method runs the simulate method.  It returns a map from role to double, which maps role to the
	 * average utility found in the depth charges in this simulation.
	 *
	 * We have a helper method that returns the number of depth charges we should use for this iteration.
	 * For each of the inner iterations, we run a depth charge, record its length, update the global depth
	 * charge stats, and then update the stats for each role.
	 *
	 * The final loop simply divides the utility counts by the count, so that we get an average.
	 */
	public Map<Role, Double> simulate()
			throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		Map<Role, Double> rewards = new HashMap<Role, Double>();
		int count = getCount();
		for (int i = 0; i < count; i++) {
			long start = System.currentTimeMillis();
			MachineState finalState = findRandomFinalState();
			long end = System.currentTimeMillis();
			updateDepthChargeStats(end - start);
			for (Role r : machine.findRoles()) {
				rewards.put(r, rewards.getOrDefault(r, 0.0) + machine.getGoal(finalState, r));
			}
		}
		for (Role r : machine.findRoles()) {
			rewards.put(r, (rewards.get(r) / count));
		}
		return rewards;
	}

	/*
	 * This method is backpropagation.  It's a bit confusing honestly, but the idea is that for each node,
	 * we want to update is parent nodes(s)' statistics based on what we found.  We run this recursively up
	 * the tree until we get to the root.
	 */
	public void backpropagate(Map<Role, Double> rewards) {
		for (MCTSNode parent : parents) {
			List<Move> jointMove = parent.getJointMove(this);
			List<Role> roles = machine.getRoles();
			for (int i = 0; i < roles.size(); i++) {
				Role role = roles.get(i);
				Move move = jointMove.get(i);
				double reward = rewards.get(role);
				parent.updateUtility(role, move, reward);
				parent.updateCount(role, move);
			}
			parent.backpropagate(rewards);
		}
	}

	/*
	 * This method updates the depth charge stats.  We simple increment the number of depth charges,
	 * and update the running average
	 */
	private void updateDepthChargeStats(long newTime) {
		numDepthCharges++;
		runningAverage += (((double) Math.max(newTime, 10) - runningAverage) / numDepthCharges);
	}

	/*
	 * This method calculates the number of depth charges we should make on an iteration. It uses the
	 * running statistics to decide how many to run.  We default to four, but otherwise we do as many as
	 * we think will complete in half a second. We need to do at least one, otherwise the whole process
	 * is pointless.
	 *
	 * This method helps to make sure we don't accidentally time out in very deep games.  It allows us to
	 * still make a number of depth charges in small games, but not too many in big ones.
	 */
	private int getCount() {
		if (numDepthCharges == 0) {
			return COUNT;
		} else {
			return Math.max(1, (int) (500 / runningAverage));
		}
	}

	/*
	 * This method is essentially a reverse lookup in the map from jointMove to node.
	 * We use it in backprop.  It essentially loops through the keys in the map, and then checks
	 * to see if the value associated with the key is the node we're looking for.  If so, we return this
	 * key. See backpropagation() to see why we need this.
	 *
	 * TODO: Handle errors better than returning null, which probably crashes the program.
	 */
	private List<Move> getJointMove(MCTSNode node) {
		for (List<Move> jointMove : children.keySet()) {
			if (children.get(jointMove).equals(node))
				return jointMove;
		}
		System.out.println("Joint Move not found for a node");
		return null;
	}

	/*
	 * This method looks up a state in the globabl statemap and returns it if it exists.
	 * Otherwise, it creates a new node and returns that.
	 */
	public MCTSNode findOrCreateNodeByState(MachineState state)
			throws MoveDefinitionException {
		MCTSNode node;
		if (stateMap.containsKey(state)) {
			node = stateMap.get(state);
		} else {
			node = new MCTSNode(machine, state);
		}
		return node;
	}

	/*
	 * This method sets this node to be the root.  It does this by simply setting the parents
	 * array to an empty list.  This will cause the backpropagation to return, ending the recursion
	 */
	public void setRoot() {
		parents = new ArrayList<MCTSNode>();
	}

	/*
	 * This method adds a parent to the parents list of this node.  It does so safely, only adding if
	 * the parent is not already in the list.
	 */
	public void addParent(MCTSNode parent) {
		if (!parents.contains(parent)) {
			parents.add(parent);
		}
	}

	/*
	 * This method updates utility statistics for a given role and given move with a given award.
	 */
	private void updateUtility(Role role, Move move, double reward) {
		int moveIndex = getMoveIndex(legalMap.get(role), move);
		List<Double> utilityList = utilityMap.get(role);
		utilityList.set(moveIndex, utilityList.get(moveIndex) + reward);
	}

	/*
	 * This method returns the index of the given move into the legals list. This is used in updating
	 * utility and counts, all in backprop.
	 *
	 * TODO: Handle the error state better
	 */
	private int getMoveIndex(List<Move> legals, Move move) {
		for (int i = 0; i < legals.size(); i++) {
			if (legals.get(i).equals(move))
				return i;
		}
		System.out.println("Move not found in legal moves, returning 0");
		return 0;
	}

	/*
	 * This method updates count statistics for a given role and given move (incrementing by 1).
	 */
	private void updateCount(Role role, Move move) {
		int moveIndex = getMoveIndex(legalMap.get(role), move);
		List<Double> countList = countMap.get(role);
		countList.set(moveIndex, countList.get(moveIndex) + 1);
	}

	/*
	 * This move find the best move for a given role.  We use this as a way of calculating the optimal
	 * move for a role.  We use the move that has the highest average utility.  Although it looks relatively
	 * complex, it's actually pretty straightforward how this is implemented.
	 */
	public Move findBestChild(Role role) {
		List<Move> legals = legalMap.get(role);
		List<Double> utilites = utilityMap.get(role);
		List<Double> counts = countMap.get(role);
		int bestIndex = 0;
		double bestScore = 0.0;
		for (int i = 0; i < legals.size(); i++) {
			if (counts.get(i) > 0) {
				double score = utilites.get(i) / counts.get(i);
				System.out.println("MCTS finds possible move " + legals.get(i) + " with score: " + score);
				if (score > bestScore) {
					bestScore = score;
					bestIndex = i;
				}
			}
		}
		System.out.println("So far, we've done " + numDepthCharges + " depth charges.");
		return legals.get(bestIndex);
	}

	/*
	 * This method completes a depth charge, simulating random joint moves until a final state is found.
	 */
	private MachineState findRandomFinalState() throws TransitionDefinitionException, MoveDefinitionException {
		MachineState randState = state;
		while (!machine.findTerminalp(randState)) {
			randState = machine.getNextState(randState, machine.getRandomJointMove(randState));
		}
		return randState;
	}

}
