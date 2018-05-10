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

public class KeepAliveTimeLimitedPlayerJack extends GGPlayer {

	private static final long PADDING = 3000;
	private long paddedTimeout;
	private boolean timedOut;
	private boolean treeExplored;

	public static void main(String[] args) {
		Player.initialize(new KeepAliveTimeLimitedPlayerJack().getName());
	}

	private class KAInt {
		public final int score;
		public final boolean terminal;

		public KAInt(int score, boolean terminal) {
			this.score = score;
			this.terminal = terminal;
		}
	}

	@Override
	public void start(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// NOOP
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
		KAInt maxScore = null;
		for (Move move : legals) {
			KAInt score = min(machine, state, role, move, new KAInt(0, true), new KAInt(100, false), depth - 1);
			if (shouldOverwrite(maxMove, maxScore, score)) {
				maxMove = move;
				maxScore = score;
			}
			if (maxScore.score == 100) break;
		}
		System.out.println("Max score: " + maxScore.score);
		return maxMove;
	}

	private boolean shouldOverwrite(Move maxMove, KAInt maxScore, KAInt score) {
		return maxMove == null || score.score > maxScore.score || (score.score == maxScore.score && !score.terminal);
	}


	private KAInt min(StateMachine machine, MachineState state, Role role, Move move, KAInt alpha, KAInt beta, int remainingDepth)
				throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
		for (List<Move> jointMove : jointMoves) {
			MachineState nextState = machine.getNextState(state, jointMove);
			KAInt score = max(machine, nextState, role, alpha, beta, remainingDepth);
			if (score.score < beta.score || (score.score == beta.score && !beta.terminal)) {
				beta = score;
			}
			if(beta.score < alpha.score || (beta.score == alpha.score && beta.terminal)) return alpha;
		}
		return beta;
	}

	private KAInt max(StateMachine machine, MachineState state, Role role, KAInt alpha, KAInt beta, int remainingDepth)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (machine.findTerminalp(state)) {
			return new KAInt(machine.findReward(role, state), true);
		}
		if (remainingDepth <= 0) {
			treeExplored = false;
			return new KAInt(heuristic(machine, state, role), false);
		}
		if (isTimedOut()) {
			treeExplored = false;
			return new KAInt(0, false);
		}
		List<Move> legals = machine.getLegalMoves(state, role);
		for (Move move : legals) {
			KAInt score = min(machine, state, role, move, alpha, beta, remainingDepth - 1);
			if (score.score > alpha.score || (score.score == alpha.score && alpha.terminal)) {
				alpha = score;
			}
			if (alpha.score > beta.score || (alpha.score == beta.score && !alpha.terminal)) return beta;
		}
		int currentReward = machine.findReward(role, state);
		if (currentReward > alpha.score || (currentReward == alpha.score && alpha.terminal)) {
			alpha = new KAInt(currentReward, false);
		}
		if (alpha.score > beta.score || (alpha.score == beta.score && !alpha.terminal)) return beta;
		return alpha;
	}

	private int heuristic(StateMachine machine, MachineState state, Role role)
			throws GoalDefinitionException {
		return Math.max(1, machine.findReward(role, state));
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
		return "_time_limited_player_with_keep_alive_jack";
	}

}
