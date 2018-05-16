package jack.A5;

import java.util.List;
import java.util.Map;

import org.ggp.base.apps.player.Player;
import org.ggp.base.util.gdl.grammar.Gdl;
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
import jack.A4.MCTSNode;

public class OptimizePlayerJack extends GGPlayer {

	private static final long PADDING = 5000;
	private long paddedTimeout;
	private MCTSNode rootNode;

	public static void main(String[] args) {
		Player.initialize(new OptimizePlayerJack().getName());
	}

	@Override
	public void start(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		List<Gdl> rules = getMatch().getGame().getRules();
		System.out.println(rules);
		rules = SubgoalReorderer.optimize(rules);
		System.out.println(rules);
		rules = RedundantSubgoalRemover.optimize(rules);
		System.out.println(rules);
		rules = RedundantRuleRemover.optimize(rules);
		System.out.println(rules);
		StateMachine machine = getInitialStateMachine();
		machine.initialize(rules);
		switchStateMachine(machine);
		MachineState state = getCurrentState();
		Role role = getRole();
		rootNode = new MCTSNode(machine, state);
		paddedTimeout = timeout - PADDING;
		findBestMove(machine, state, role);
	}

	@Override
	public Move play(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		paddedTimeout = timeout - PADDING;
		Move bestMove = findBestMove(machine, state, role);
		System.out.println("Move is: " + bestMove + "\n");
		return bestMove;
	}

	private Move findBestMove(StateMachine machine, MachineState state, Role role)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		rootNode = rootNode.findOrCreateNodeByState(state);
		rootNode.setRoot();
		while (System.currentTimeMillis() < paddedTimeout) {
			MCTSNode selection = rootNode.select();
			Map<Role, Double> rewards = selection.simulate();
			selection.backpropagate(rewards);
		}
		return rootNode.findBestChild(role);
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
		return "_optimize_player_jack";
	}

}
