package john;
import java.util.Arrays;
import java.util.List;

import org.ggp.base.apps.player.Player;
import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
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

public class CompDelibPlayerJohn extends GGPlayer {
	public static void main(String[] args) {
		Player.initialize(new CompDelibPlayerJohn().getName());
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
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
			List<Move> nextMove = Arrays.asList(legals.get(i));
			MachineState nextState = machine.getNextState(state, nextMove);
			int result = maxscore(machine, nextState, role);
			if (result > score) {
				score = result;
			}
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
			List<Move> nextMove = Arrays.asList(legals.get(i));
			int result = maxscore(machine, machine.getNextState(state, nextMove), role);
			if (result == 100 ) {return legals.get(i);}
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
	public String getName() {
		return "john_comp_delib_player";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}

}
