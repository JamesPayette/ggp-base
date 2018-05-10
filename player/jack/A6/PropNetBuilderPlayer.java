package jack.A6;

import org.ggp.base.apps.player.Player;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
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
import jack.A4.AlphaBetaSearcher;
import jack.A4.MCTSSimulator;

public class PropNetBuilderPlayer extends GGPlayer {

	// We give 5 seconds of padding just to be safe
	private static final long PADDING = 3000;
	// We use the same simulator throughout, just updating the root node when necessary.
	private MCTSSimulator simulator;
	private PropNet propNet;

	public static void main(String[] args) {
		Player.initialize(new PropNetBuilderPlayer().getName());
	}

	@Override
	public void start(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();

		try { propNet = OptimizingPropNetFactory.create(getMatch().getGame().getRules());
		} catch (InterruptedException e) { e.printStackTrace(); }

		if (propNet != null) System.out.println("PropNet created. Size:" + propNet.getSize());

		simulator = new MCTSSimulator(machine, state, role);
		simulator.start();
		long sleepTime = timeout - System.currentTimeMillis() - PADDING;
		try { Thread.sleep(sleepTime); } catch (InterruptedException e) { }
	}

	@Override
	public Move play(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		AlphaBetaSearcher searcher = new AlphaBetaSearcher(machine, state, role);
		searcher.start();
		simulator.setStateAndTimeout(state, timeout - PADDING);
		Move bestMove;
		Move bestMCTSMove = simulator.getBestMove();
		Move bestABMove = searcher.getBestMove();
		if (bestABMove != null) {
			System.out.println("Using Alpha Beta");
			bestMove = bestABMove;
		} else if (bestMCTSMove != null) {
			System.out.println("Using MCTS");
			bestMove = bestMCTSMove;
		} else {
			System.out.println("Using Random Move");
			bestMove = machine.findLegalx(role, state);
		}
		searcher.stopPlaying();
		System.out.println("Move is: " + bestMove + "\n");
		return bestMove;
	}

	/*
	 * We need to let our simulator know that it can return
	 */
	@Override
	public void abort() {
		simulator.stopPlaying();
	}

	/*
	 * We need to let our simulator know that it can return
	 */
	@Override
	public void stop() {
		simulator.stopPlaying();
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public String getName() {
		return "_propnet_builder_player";
	}
}
