package jack.A4;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/*
 * The Alpha-Beta searcher is basically a thread implementation of the Alpha Beta Player.
 * On startup, it tries to do a complete alpha-beta search of the tree.
 *
 * If it completes while it's still playing, it puts the best move it found into the moveQueue.
 * The move is then retrieved from the moveQueue by the main thread for use in the play() method.
 */

public class AlphaBetaSearcher extends Thread {
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
