package jack;

import java.util.List;
import java.util.Map;

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

public class CompulsiveDeliberationJack extends GGPlayer {

	public static void main(String[] args) {
		Player.initialize(new CompulsiveDeliberationJack().getName());
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
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		List<Move> legals = machine.findLegals(role, state);
		Map<Move, List<MachineState>> nextStateMap = machine.getNextStates(state, role);
		Move bestMove = null;
		int bestScore = 0;
		for (Move move : legals) {
			for (MachineState nextState : nextStateMap.get(move)) {
				int score = bestScore(machine, nextState, role);
				if (bestMove == null || score >= bestScore) {
					bestMove = move;
					bestScore = score;
				}
				if (bestScore == 100) break;
			}
		}
		return bestMove;
	}

	private int bestScore(StateMachine machine, MachineState state, Role role)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (machine.findTerminalp(state)) {
			return machine.findReward(role, state);
		}
		int bestScore = 0;
		for (MachineState nextState : machine.getNextStates(state)) {
			int score = bestScore(machine, nextState, role);
			if (score > bestScore) {
				bestScore = score;
			}
			if (bestScore == 100) break;
		}
		return bestScore;
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
		return "jack_compulsive_deliberation_player";
	}

}
