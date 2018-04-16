package john;
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
public class LegalPlayerJohn extends GGPlayer {
	/**
	 * All we have to do here is call the Player's initialize method with
	 * our name as the argument so that the Player GUI knows which name to
	 * have selected at startup. This is all you need in the main method
	 * of your own player.
	 */
	public static void main(String[] args) {
		Player.initialize(new LegalPlayerJohn().getName());
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void start(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	}


	@Override
	public Move play(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();

		List<Move> legals = findLegals(role, state, machine);
		Move move = legals.get(0);
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
		return "john_legal_player";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}

}
