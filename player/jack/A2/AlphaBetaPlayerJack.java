package jack.A2;

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

public class AlphaBetaPlayerJack extends GGPlayer {

	public static void main(String[] args) {
		Player.initialize(new AlphaBetaPlayerJack().getName());
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
		Move bestMove = findBestMove(machine, state, role);
		System.out.println("Move is: " + bestMove);
		return bestMove;
	}

	private Move findBestMove(StateMachine machine, MachineState state, Role role)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		List<Move> legals = machine.getLegalMoves(state, role);
		Move maxMove = null;
		int maxScore = 0;
		for (Move move : legals) {
			int score = min(machine, state, role, move, 0, 100);
			if (maxMove == null || score > maxScore) {
				maxMove = move;
				maxScore = score;
			}
			if (maxScore == 100) break;
		}
		return maxMove;
	}

	private int min(StateMachine machine, MachineState state, Role role, Move move, int alpha, int beta)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
		for (List<Move> jointMove : jointMoves) {
			MachineState nextState = machine.getNextState(state, jointMove);
			int score = max(machine, nextState, role, alpha, beta);
			beta = Math.min(score, beta);
			if (beta <= alpha) return alpha;
		}
		return beta;
	}

	private int max(StateMachine machine, MachineState state, Role role, int alpha, int beta)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (machine.findTerminalp(state)) {
			return machine.findReward(role, state);
		}
		List<Move> legals = machine.getLegalMoves(state, role);
		for (Move move : legals) {
			int score = min(machine, state, role, move, alpha, beta);
			alpha = Math.max(score, alpha);
			if (alpha >= beta) return beta;
		}
		return alpha;
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
		return "_alpha_beta_player_jack";
	}

}
