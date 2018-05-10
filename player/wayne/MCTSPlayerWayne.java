package wayne;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.apps.player.Player;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

import base.GGPlayer;

public class MCTSPlayerWayne extends GGPlayer {

	final static long timeBuffer = 3000;

	/**
	 * All we have to do here is call the Player's initialize method with
	 * our name as the argument so that the Player GUI knows which name to
	 * have selected at startup. This is all you need in the main method
	 * of your own player.
	 */
	public static void main(String[] args) {
		Player.initialize(new MCTSPlayerWayne().getName());
	}

	/**
	 * Currently, we can get along just fine by using the Prover State Machine.
	 * We will implement a more optimized PropNet State Machine later. The Cached
	 * State Machine is a wrapper that reduces the number of calls to the Prover
	 * State Machine by returning results of method calls that have been made previously.
	 * (e.g. getNextState calls or getLegalMoves for the same combination of parameters)
	 */
	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	/**
	 * If we wanted to use the metagame (or start) clock to compute something
	 * about the game (or explore the game tree), we could do so here. Since
	 * this is just a legal player, there is no need for such computation.
	 */
	@Override
	public void start(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

	}

	/**
	 * Where your player selects the move they want to play. In-line comments
	 * explain each line of code. Your goal essentially boils down to returning the best
	 * move possible.
	 *
	 * The current state for the player is updated between moves automatically for you.
	 *
	 * The value of the timeout variable is the UNIX time by which you need to submit your move.
	 * You can determine how much time your player has left (in milliseconds) by using the following line of code:
	 * long timeLeft = timeout - System.currentTimeMillis();
	 *
	 * Make sure to submit your move before this time runs out. It's also a good
	 * idea to leave a couple seconds (2-4) as buffer for network lag/spikes and
	 * so that you don't overrun your time thus timing out (which plays
	 * a random move for you and counts as an error -- two very bad things).
	 */
	@Override
	public Move play(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		//Gets our state machine (the same one as returned in getInitialStateMachine)
		//This State Machine simulates the game we are currently playing.
		StateMachine machine = getStateMachine();

		//Gets the current state we're in (e.g. move 2 of a game of tic tac toe where X just played in the center)
		MachineState state = getCurrentState();

		//Gets our role (e.g. X or O in a game of tic tac toe)
		Role role = getRole();

		//Gets all legal moves for our player in the current state
		List<Move> legalMoves = findLegals(role, state, machine);

		Move chosenMove = legalMoves.get(0);
		int nodesExplored = 0;
		if (legalMoves.size() > 1) {
			Node rootNode = new Node(role, state, machine, null, null);
			expand(rootNode);
			while (timeout - System.currentTimeMillis() > timeBuffer) {
				Node selectedNode = select(rootNode);
				expand(selectedNode);
				double score = simulate(selectedNode, timeout);
				backpropagate(selectedNode, score);
			}

			double maxScore = 0.0;
			for (int i = 0; i < rootNode.playerLegalMoves.size(); i++) {
				double score = 1.0 * rootNode.playerUtilities[i] / rootNode.playerVisits[i];
				if (score > maxScore) {
					maxScore = score;
					chosenMove = rootNode.playerLegalMoves.get(i);
				}
			}

			for (int visit : rootNode.playerVisits) {
				// Sum over visit counts of children nodes to find own visit count.
				nodesExplored += visit;
			}
		}

		//Logging what decisions your player is making as well as other statistics
		//is a great way to debug your player and benchmark it against other players.
		System.out.println("Explored " + nodesExplored + " nodes.");
		System.out.println("I am playing: " + chosenMove);
		return chosenMove;
	}

	private Node select(Node node) throws MoveDefinitionException {
		// Return any children which have not been visited yet.
		if (node.machine.isTerminal(node.state)) {
			return node;
		}

		for (int i = 0; i < node.children.size(); i++) {
			if (node.jointVisits[i] == 0) {
				return node.children.get(i);
			}
		}

		// If all children have been visited already, then pick own move to maximize
		// selection function and pick opponent move which minimizes selection function.
		// First, select our own move.
		double score = 0;
		Move playerMove = node.playerLegalMoves.get(0);

		int parentVisits = 0;
		for (int visit : node.playerVisits) {
			// Sum over visit counts of children nodes to find own visit count.
			parentVisits += visit;
		}

		for (int i = 0; i < node.playerLegalMoves.size(); i++) {
			double utility = node.playerUtilities[i];
			int visits = node.playerVisits[i];
			double newScore = 1.0 * utility / visits + Math.sqrt(2 * Math.log(parentVisits) / visits);
			if (newScore > score) {
				score = newScore;
				playerMove = node.playerLegalMoves.get(i);
			}
		}

		// Next, find the legal joint moves and chose the one which minimizes the selection function.
		score = 100;
		List<List<Move>> jointMoves = node.machine.getLegalJointMoves(node.state, node.role, playerMove);
		List<Move> jointMove = jointMoves.get(0);
		parentVisits = node.playerVisits[node.playerLegalMoves.indexOf(playerMove)];
		int index = node.jointLegalMoves.indexOf(jointMove);

		for (List<Move> move : jointMoves) {
			index = node.jointLegalMoves.indexOf(move);
			double utility = node.jointUtilities[index];
			int visits = node.jointVisits[index];
			double newScore = 1.0 * utility / visits + Math.sqrt(2 * Math.log(parentVisits) / visits);
			if (newScore < score) {
				score = newScore;
				jointMove = move;
			}
		}
		return select(node.children.get(index));
	}

	private void expand(Node node) throws TransitionDefinitionException, MoveDefinitionException {
		// On expansion, need to populate node's children, as well as initialize utility, visit, and move arrays
		// Don't bother expanding terminal states.
		if (node.children == null && !node.machine.isTerminal(node.state)) {
			node.children = new ArrayList<Node>();
			for (List<Move> jointMove : node.jointLegalMoves) {
				MachineState nextState = node.machine.getNextState(node.state, jointMove);
				node.children.add(new Node(node.role, nextState, node.machine, node, jointMove));
			}
		}
	}

	private double simulate(Node node, long timeout) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		Random gen = new Random();
		int count = 50;
		int reward = 0;
		for (int i = 0; i < count; i++) {
			MachineState current = node.state;
			while (!node.machine.findTerminalp(current)) {
				List<List<Move>> moves = node.machine.getLegalJointMoves(current);
				current = node.machine.getNextState(current, moves.get(gen.nextInt(moves.size())));
			}
			reward += node.machine.findReward(node.role, current);
		}
		return 1.0 * reward / count;
	}

	private void backpropagate(Node node, double score) {
		// This is really just a wrapper to pass information about the child to the parent
		parentBackprop(node.parent, node, score);
	}

	private void parentBackprop(Node node, Node child, double score) {
		// Figure out the indices to update visit counts and utilities.
		int roleIndex = node.machine.getRoleIndices().get(node.role);
		int playerIndex = node.playerLegalMoves.indexOf(child.originMove.get(roleIndex));
		int jointIndex = node.jointLegalMoves.indexOf(child.originMove);
		node.playerUtilities[playerIndex] += score;
		node.playerVisits[playerIndex] += 1;
		node.jointUtilities[jointIndex] += score;
		node.jointVisits[jointIndex] += 1;

		if (node.parent != null) {
			parentBackprop(node.parent, node, score);
		}
	}

	/**
	 * Can be used for cleanup at the end of a game, if it is needed.
	 */
	@Override
	public void stop() {

	}

	/**
	 * Can be used for cleanup in the event a game is aborted while
	 * still in progress, if it is needed.
	 */
	@Override
	public void abort() {

	}

	/**
	 * Returns the name of the player.
	 */
	@Override
	public String getName() {
		return "_mcts_player_wayne";
	}

	private class Node {
		public Role role;
		public Node parent;
		public StateMachine machine;
		public MachineState state;
		public List<Node> children;
		public List<Move> originMove;

		public double playerUtilities[];
		public double jointUtilities[];
		public int playerVisits[];
		public int jointVisits[];
		public List<Move> playerLegalMoves;
		public List<List<Move>> jointLegalMoves;

		public Node(Role role, MachineState state, StateMachine machine, Node parent, List<Move> move) throws MoveDefinitionException {
			this.role = role;
			this.state = state;
			this.machine = machine;
			this.parent = parent;
			this.originMove = move;

			if (!machine.isTerminal(state)) {
				this.playerLegalMoves = machine.getLegalMoves(state, role);
				this.jointLegalMoves = machine.getLegalJointMoves(state);
				this.playerUtilities =  new double[this.playerLegalMoves.size()];
				this.playerVisits =  new int[this.playerLegalMoves.size()];
				this.jointUtilities =  new double[this.jointLegalMoves.size()];
				this.jointVisits =  new int[this.jointLegalMoves.size()];
			}
		}
	}

}
