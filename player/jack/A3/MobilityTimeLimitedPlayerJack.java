package jack.A3;

import java.util.List;

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

public class MobilityTimeLimitedPlayerJack extends GGPlayer {

	private static final long PADDING = 3000;
	private long paddedTimeout;
	private boolean timedOut;
	private boolean treeExplored;

	public static void main(String[] args) {
		Player.initialize(new MobilityTimeLimitedPlayerJack().getName());
	}

	@Override
	public void start(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		//NOOP
	}

	@Override
	public Move play(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		paddedTimeout = timeout - PADDING;
		timedOut = false;
		Move globalBestMove = null;
		int depth = machine.getRoles().size();
		int incrementer = depth;
		while(true) {
			treeExplored = true;
			Move bestMove = findBestMove(machine, state, role, depth);
			if (!timedOut || globalBestMove == null) globalBestMove = bestMove;
			if (treeExplored && !timedOut) System.out.println("Tree Explored!");
			if (timedOut || treeExplored) break;
			depth += incrementer;
		}
		System.out.println("Explored to depth: " + depth);
		System.out.println("Move is: " + globalBestMove + "\n");
		return globalBestMove;
	}

	private Move findBestMove(StateMachine machine, MachineState state, Role role, int depth)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		List<Move> legals = machine.getLegalMoves(state, role);
		if (legals.size() == 1) {
			return legals.get(0);
		}
		Move maxMove = null;
		int maxScore = 0;
		for (Move move : legals) {
			int score = min(machine, state, role, move, 0, 100, depth - 1);
			if (maxMove == null || score > maxScore) {
				maxMove = move;
				maxScore = score;
			}
			if (maxScore == 100) break;
		}
		System.out.println("Max Score: " + maxScore);
		return maxMove;
	}

	private int min(StateMachine machine, MachineState state, Role role, Move move, int alpha, int beta, int remainingDepth)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
		for (List<Move> jointMove : jointMoves) {
			MachineState nextState = machine.getNextState(state, jointMove);
			int score = max(machine, nextState, role, alpha, beta, remainingDepth);
			beta = Math.min(score, beta);
			if (beta <= alpha) return alpha;
		}
		return beta;
	}

	private int max(StateMachine machine, MachineState state, Role role, int alpha, int beta, int remainingDepth)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (machine.findTerminalp(state)) {
			return machine.findReward(role, state);
		}
		if (remainingDepth <= 0) {
			treeExplored = false;
			return heuristic(machine, state, role);
		}
		if (isTimedOut()) {
			treeExplored = false;
			return 0;
		}
		List<Move> legals = machine.getLegalMoves(state, role);
		for (Move move : legals) {
			int score = min(machine, state, role, move, alpha, beta, remainingDepth - 1);
			alpha = Math.max(score, alpha);
			if (alpha >= beta) return beta;
		}
		return alpha;
	}

	private int heuristic(StateMachine machine, MachineState state, Role role)
			throws MoveDefinitionException {
		float legal = machine.getLegalMoves(state, role).size();
		float feasible = machine.findActions(role).size();
		return (int) ((legal / feasible) * 100);
	}

	private boolean isTimedOut() {
		if (timedOut) return true;
		if (System.currentTimeMillis() >= paddedTimeout) {
			timedOut = true;
		}
		return timedOut;
	}

	@Override
	public void abort() {
		// NOOP
	}

	@Override
	public void stop() {
		// NOOP
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}


	@Override
	public String getName() {
		return "_time_limited_player_with_mobility_jack";
	}

}
