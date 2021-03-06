package john;
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

public class MinimaxPlayerJohn extends GGPlayer {
	public static void main(String[] args) {
		Player.initialize(new MinimaxPlayerJohn().getName());
	}

	@Override
	public void start(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

	}

	public int maxscore(StateMachine machine, MachineState state, Role role)
	throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (machine.findTerminalp(state)) {
			return machine.findReward(role, state);
		}

		List<Move> legals = findLegals(role, state, machine);
		int score = 0;
		for (int i = 0; i<legals.size(); i++) {
			int result = minscore(machine, state, legals.get(i), role);
			if (result > score) {
				score = result;
			}
		}

		return score;
	}

	public int minscore(StateMachine machine, MachineState state, Move move, Role role)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
		int score = 100;
		for (List<Move> jointMove : jointMoves) {

			MachineState nextState = machine.getNextState(state, jointMove);
			int result = maxscore(machine, nextState, role);
			if (result < score) {score = result;}
		}
		return score;
	}

	@Override
	public Move play(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();


		List<Move> legals = findLegals(role, state, machine);
		Move move = legals.get(0);
		int score = 0;

		for (int i = 0; i<legals.size(); i++) {

			int result = minscore(machine, state, legals.get(i), role);
			if (result > score) {score = result; move = legals.get(i);}
		}

		System.out.print("Move is: " + move);
		return move;
	}

	@Override
	public void abort() {

	}

	@Override
	public void stop() {

	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public String getName() {
		return "_minimax_player_john";
	}
}

