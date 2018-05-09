package jack.A4;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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

public class ExperimentalPlayer extends GGPlayer {

	// We give 5 seconds of padding just to be safe
	private static final long PADDING_LONG = 5000;
	private static final long PADDING_SHORT = 3000;
	// We use the same simulator throughout, just updating the root node when necessary.
	private MCTSSimulator simulator;

	public static void main(String[] args) {
		Player.initialize(new ExperimentalPlayer().getName());
	}

	/*
	 *  Start up a simulator whose root node is the initial state of the game. We then
	 *  sleep so that our simulator can get as much work done as possible.
	 */
	@Override
	public void start(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		simulator = new MCTSSimulator(machine, state, role);
		simulator.start();
		long sleepTime = timeout - System.currentTimeMillis() - PADDING_SHORT;
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
		AlphaBetaSearcher searcher = new AlphaBetaSearcher(machine, state, role);
		searcher.start();
		long simulatorTimeout = timeout - PADDING_LONG;
		long simulatorFetchTimeout = timeout - System.currentTimeMillis() - PADDING_SHORT;
		simulator.setStateAndTimeout(state, simulatorTimeout);
		Move bestMove;
		Move bestMCTSMove = simulator.getBestMove(simulatorFetchTimeout);
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
		return "jack_experimental_player";
	}

	/*
	 * This is the MCTS simulator. It is an MCTS implementation that runs in the background while the
	 * game is going on.  It uses three queues to communicate with the main thread, making the thread-safety
	 * relatively difficult.  In the case of the simulator, the timeout is local. Whenever the current time
	 * becomes greater than the timeout, we calculate the best move of the current root node and place it in
	 * the moveQueue.  We then reset the timeout to be maxLong.  In this way, we allow the main thread to tell
	 * us when to report a best move but don't have to worry about calculating or communcation when the main
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
	private class MCTSSimulator extends Thread {
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
					if (System.currentTimeMillis() > timeout) {
						moveQueue.offer(rootNode.findBestChild(role));
						timeout = Long.MAX_VALUE;
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
			stateQueue.offer(state);
			timeoutQueue.offer(timeout);
		}

		/*
		 * This method is exposed publicly and SHOULD NOT be called internally by the simulator.
		 * This method waits for the moveQueue to be full for the specified amount of time, returning
		 * what is placed in the queue or null on a timeout.
		 *
		 * Contracts:
		 * - Not called internally (not called on the simulator thread)
		 * - Only called from a single other thread
		 * - Only called a single time per play
		 */
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

	/*
	 * The Alpha-Beta searcher is basically a thread implementation of the Alpha Beta Player.
	 * On startup, it tries to do a complete alpha-beta search of the tree.
	 *
	 * If it completes while it's still playing, it puts the best move it found into the moveQueue.
	 * The move is then retrieved from the moveQueue by the main thread for use in the play() method.
	 */

	private class AlphaBetaSearcher extends Thread {
		private StateMachine machine;
		private MachineState state;
		private Role role;
		private BlockingQueue<Move> moveQueue;
		private boolean playing = true;

		public AlphaBetaSearcher(StateMachine machine, MachineState state, Role role) {
			this.machine = machine;
			this.state = state;
			this.role = role;
			moveQueue = new ArrayBlockingQueue<Move>(1);
		}

		/*
		 * The run method is very similar to the play method of the alpha-beta player.
		 *
		 * The main difference is that if we complete the search, we put the move into the moveQueue.
		 * This exposes it to the thread that creates the searcher, so that the thread can safely
		 * retrieve this best move.  It's how we communicate between the searcher and the main thread.
		 */
		@Override
		public void run() {
			try {
				List<Move> legals =  machine.getLegalMoves(state, role);
				Move maxMove = null;
				int maxScore = 0;
				for (Move move : legals) {
					int score = min(machine, state, move, 0, 100);
					if (maxMove == null || score > maxScore) {
						maxMove = move;
						maxScore = score;
					}
					if (maxScore == 100) break;
				}
				if(playing) {
					System.out.println("Alpha-Beta offers a max move of score: " + maxScore);
					moveQueue.offer(maxMove);
				}
			} catch (MoveDefinitionException e) {
				e.printStackTrace();
			} catch (TransitionDefinitionException e) {
				e.printStackTrace();
			} catch (GoalDefinitionException e) {
				e.printStackTrace();
			}
		}

		/*
		 * This method is exposed publicly and SHOULD NOT be called internally by the searcher.
		 * This method return the move that the searcher wrote to the bestMove queue if one exists.
		 *
		 * This method may not seem safe, but it's important to remember that while we may see that
		 * the queue is empty while it's being filled, if we see that it's not empty we know it won't
		 * change, and there will be a move to take.
		 *
		 * Contracts:
		 * - Not called internally (not called on the searcher thread)
		 * - Only called from a single other thread
		 * - Only called a single time per searcher (relaxation: only called once for which a move is returned)
		 */
		public Move getBestMove() {
			Move bestMove = null;
			if (!moveQueue.isEmpty()) {
				try {
					bestMove = moveQueue.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return bestMove;
		}

		/*
		 * Sets the playing flag to false.  We won't write to the queue and we'll return from the search
		 */
		public void stopPlaying() {
			playing = false;
		}

		/*
		 * Min pulled from alpha beta player.  Should be pretty straightforward (I hope)
		 */
		private int min(StateMachine machine, MachineState state, Move move, int alpha, int beta)
				throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
			List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
			for (List<Move> jointMove : jointMoves) {
				MachineState nextState = machine.getNextState(state, jointMove);
				int score = max(machine, nextState, alpha, beta);
				beta = Math.min(score, beta);
				if (beta <= alpha) return alpha;
			}
			return beta;
		}

		/*
		 * Max pulled from alpha beta player.  Should be pretty straightforward (I hope)
		 *
		 * Main difference is that if we're no longer playing, return 0 as the value will never be read.
		 */
		private int max(StateMachine machine, MachineState state, int alpha, int beta)
				throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
			if (!playing) {
				return 0;
			}
			if (machine.findTerminalp(state)) {
				return machine.findReward(role, state);
			}
			List<Move> legals = machine.getLegalMoves(state, role);
			for (Move move : legals) {
				int score = min(machine, state, move, alpha, beta);
				alpha = Math.max(score, alpha);
				if (alpha >= beta) return beta;
			}
			return alpha;
		}
	}
}
