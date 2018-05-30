package jack.A4;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/*
 * This is the MCTS simulator. It is an MCTS implementation that runs in the background while the
 * game is going on.  It uses three queues to communicate with the main thread, making the thread-safety
 * relatively difficult.  In the case of the simulator, the timeout is local. Whenever the current time
 * becomes greater than the timeout, we calculate the best move of the current root node and place it in
 * the moveQueue.  We then reset the timeout to be maxLong.  In this way, we allow the main thread to tell
 * us when to report a best move but don't have to worry about calculating or communication when the main
 * thread is not ready.
 *
 * This thread runs until the main thread tells it to stop. On each iteration, it selects a node,
 * simulates from this node, and backpropagates the results.  Most of the logic for this can be found
 * in the MCTSNode class.  We can think of this loop as the logic that continues to update the MCTS tree.
 *
 * The interesting logic happens when the main thread receives a play message.  When that happens, the
 * main thread places a timeout and a new state into the timeoutQueue and stateQueue. At the start of
 * each iteration, the simulator checks these queues to see if these queues are not empty.  If they are
 * not, the simulator grabs the state and resets the root to be the node for this state.  It also sets
 * the local timeout from maxLong to the provided timeout.  It then continues to simulate until the
 * timeout is up.  At this point, it calculates the best move, places it in the move queue (so that the
 * main thread can grab it) and reset the timeout to maxLong.
 */
public class MCTSSimulator extends Thread {
	private MCTSNode rootNode;
	private StateMachine machine;
	private MachineState state;
	private BlockingQueue<MachineState> stateQueue;
	private BlockingQueue<Long> timeoutQueue;
	private BlockingQueue<Move> moveQueue;
	private long timeout;
	private Role role;
	private boolean playing = true;

	public MCTSSimulator(StateMachine machine, MachineState state, Role role) {
		this.machine = machine;
		this.state = state;
		this.role = role;
		stateQueue = new ArrayBlockingQueue<MachineState>(1);
		timeoutQueue = new ArrayBlockingQueue<Long>(1);
		moveQueue = new ArrayBlockingQueue<Move>(1);
		timeout = Long.MAX_VALUE;
	}

	@Override
	public void run() {
		try {
			rootNode = new MCTSNode(machine, state);
			while(playing) {
				if (!stateQueue.isEmpty()) setRoot();
				if (!timeoutQueue.isEmpty()) timeout = timeoutQueue.take();
				if (System.currentTimeMillis() >= timeout) {
					System.out.println("MCTS timeout. Placing best move in queue.");
					timeout = Long.MAX_VALUE;
					moveQueue.clear();
					moveQueue.add(rootNode.findBestChild(role));
				}
				MCTSNode selection = rootNode.select();
				Map<Role, Double> rewards = selection.simulate();
				selection.backpropagate(rewards);
			}
		} catch (MoveDefinitionException e) {
			e.printStackTrace();
		} catch (TransitionDefinitionException e) {
			e.printStackTrace();
		} catch (GoalDefinitionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
	 * This method is exposed publicly and SHOULD NOT be called internally by the simulator.
	 * This method tells the simulator the state for the new rootNode and the timeout for this turn.
	 *
	 * Contracts:
	 * - Not called internally (not called on the simulator thread)
	 * - Only called from a single other thread
	 * - Only called a single time per play
	 */
	public void setStateAndTimeout(MachineState state, long timeout) {
		stateQueue.clear();
		stateQueue.add(state);
		timeoutQueue.clear();
		timeoutQueue.add(timeout);
	}

	/*
	 * This method is exposed publicly and SHOULD NOT be called internally by the simulator.
	 * This method waits for the moveQueue to be full, takes out the move and returns it.
	 *
	 * Contracts:
	 * - Not called internally (not called on the simulator thread)
	 * - Only called from a single other thread
	 * - Only called a single time per play
	 */
	public Move getBestMove() {
		Move bestMove = null;
		try {
			bestMove = moveQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return bestMove;
	}

	public Move getBestMove(long timeout) {
		Move bestMove = null;
		try {
			bestMove = moveQueue.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return bestMove;
	}

	/*
	 * Tells the simulator to return
	 */
	public void stopPlaying() {
		playing = false;
	}

	/*
	 * This method should only be called if the stateQueue is not empty, and only once then
	 * It gets the new state of the game, and finds or creates the node associated with that state.
	 * then sets this node to the rootNode and makes it a rootNode.
	 */
	private void setRoot()
			throws InterruptedException, MoveDefinitionException {
		rootNode = rootNode.findOrCreateNodeByState(stateQueue.take());
		rootNode.setRoot();
	}
}
