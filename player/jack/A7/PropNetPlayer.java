package jack.A7;

import org.ggp.base.apps.player.Player;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import base.GGPlayer;
import jack.A4.MCTSNode;
import jack.A4.MCTSSimulator;

/*
 * This experimental player uses both a Monte Carlo Simulator and an Alpha Beta searcher.
 * On start, a Monte Carlo simulator is spun up, and we let it run for all of the start time.
 *
 * On play, we notify the simulator that, at a time in the near future, we will need the best move
 * from a certain state (details about thread communication are below).  We then spin up an alpha beta
 * searcher.  When we hit the timeout, we check to see if the alpha beta searcher has complete its search.
 * If it has, we use the move it suggests.  Otherwise, we use the move the simulator suggests. If something
 * goes wrong, we fallback on a legal player.
 */

public class PropNetPlayer extends GGPlayer {

	// We give 5 seconds of padding just to be safe
	private static final long PADDING = 3000;
	// We use the same simulator throughout, just updating the root node when necessary.
	private MCTSSimulator simulator;

	public static void main(String[] args) {
		Player.initialize(new PropNetPlayer().getName());
	}

	/*
	 *  Start up a simulator whose root node is the initial state of the game. We then
	 *  sleep so that our simulator can get as much work done as possible.
	 */
	@Override
	public void start(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		MCTSNode.reset();
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		simulator = new MCTSSimulator(machine, state, role);
		simulator.start();
		long sleepTime = timeout - System.currentTimeMillis() - PADDING;
		try { Thread.sleep(sleepTime); } catch (InterruptedException e) { }
	}


	/*
	 * First, we create a thread that runs alpha-beta search and start it. We then give the simulator the
	 * new state (which it will update to its root node) and the timeout.  We then wait for the simulator
	 * to return its best move.  After we get this, we check to see if the searcher completed.
	 *
	 * The assumption here is that if we can search the whole game tree, we should.  Otherwise, we fall back
	 * on the MCTS move.
	 *
	 * We finally have a fallback if something goes wrong in the threads, which is a simple legal player.
	 *
	 * We'll go into the specifics later, but the idea here is that we can tell the simulator when it needs
	 * to find us a best move by.  We then wait for this move, waking up as soon as it becomes available
	 * or 2 seconds after it's supposed to return (in which case we can assume there's an error). We can
	 * check to see if our Alpha Beta searcher has sent us a message saying that it's completed. All of
	 * this communication uses BlockingQueues of length one, which allows nice communication between threads
	 * as long as certain contracts are followed.
	 *
	 * [IMPT] : The implementation relies on a very specific multithreaded ordering.  Changing them even a
	 * small amount can cause the entire thing to fall apart.  It's not super difficult to understand, but
	 * definitely be careful and make sure you understand exactly the implementation before making changes.
	 *
	 * There's probably a better way to multithread this, so feel free
	 */
	@Override
	public Move play(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		simulator.setStateAndTimeout(state, timeout - PADDING);
		Move bestMove = simulator.getBestMove();
		if (bestMove != null) {
			System.out.println("Using MCTS");
		} else {
			System.out.println("Using Random Move");
			bestMove = machine.findLegalx(role, state);
		}
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
		return new CachedStateMachine(new PropNetStateMachine());
	}

	@Override
	public String getName() {
		return "experimental_propnet_player";
	}
}
