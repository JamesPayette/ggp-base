package jack.A4;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

public class ExperimentalPlayer extends GGPlayer {

	private static final long PADDING = 5000;
	private MCTSSimulator simulator;

	public static void main(String[] args) {
		Player.initialize(new MCTSPlayerJack().getName());
	}

	@Override
	public void start(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		simulator = new MCTSSimulator(machine, state);
		simulator.start();
		long sleepTime = timeout - System.currentTimeMillis() - PADDING;
		try { Thread.sleep(sleepTime); } catch (InterruptedException e) { }
	}

	@Override
	public Move play(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		MachineState state = getCurrentState();
		Role role = getRole();
		long sleepTime = timeout - System.currentTimeMillis() - PADDING;
		try { Thread.sleep(sleepTime); } catch (InterruptedException e) { }
		Move bestMove = findBestMove(state, role);
		System.out.println("Move is: " + bestMove + "\n");
		return bestMove;
	}

	private Move findBestMove(MachineState state, Role role)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		simulator.lock();
		MCTSNode rootNode = simulator.getRootNode().findNodeByState(state);
		simulator.setRootNode(rootNode);
		Move bestMove = rootNode.findBestChild(role);
		simulator.unlock();
		return bestMove;
	}


	@Override
	public void abort() {
		simulator.stopPlaying();
	}

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


	private class MCTSSimulator extends Thread {
		private MCTSNode rootNode;
		private Lock updateLock;
		private StateMachine machine;
		private MachineState state;
		private boolean playing = true;

		public MCTSSimulator(StateMachine machine, MachineState state) {
			this.machine = machine;
			this.state = state;
			updateLock = new ReentrantLock();
		}

		@Override
		public void run() {
			try {
				rootNode = new MCTSNode(machine, state, null);
				while(playing) {
					lock();
					MCTSNode selection = rootNode.select();
					Map<Role, Integer> rewards = selection.simulate();
					selection.backpropagate(rewards);
					unlock();
				}
			} catch (MoveDefinitionException e) {
				e.printStackTrace();
			} catch (TransitionDefinitionException e) {
				e.printStackTrace();
			} catch (GoalDefinitionException e) {
				e.printStackTrace();
			}
		}

		public void lock() {
			updateLock.lock();
		}

		public void unlock() {
			updateLock.unlock();
		}

		public MCTSNode getRootNode() {
			return rootNode;
		}

		public void setRootNode(MCTSNode rootNode) {
			rootNode.setRoot();
			this.rootNode = rootNode;
		}

		public void stopPlaying() {
			playing = false;
		}
	}

}
