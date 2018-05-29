package jack.A7;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class PropNetMachineState extends MachineState {

	private PropNetMarking baseMarking;
	private Collection<Proposition> bases;
	private boolean isMarked;
	private boolean isTerminal;
	private Map<Role, List<Move>> legalMoves;
	private Map<Role, Integer> goalValues;

	public PropNetMachineState() {
		baseMarking = null;
		isMarked = false;
		isTerminal = false;
		legalMoves = null;
		goalValues = null;
	}

	public PropNetMachineState(Collection<Proposition> bases) {
		this.bases = bases;
		baseMarking = new PropNetMarking(bases);
	}

	public PropNetMarking getBaseMarking() {
		return baseMarking;
	}

	public boolean get(Proposition p) {
		return baseMarking.get(p);
	}

	public boolean isMarked() {
		return isMarked;
	}

	public void setMarked(boolean marked) {
		isMarked = marked;
	}

	public boolean isTerminal() {
		return isTerminal;
	}

	public void setTerminal(boolean terminal) {
		isTerminal = terminal;
	}

	public List<Move> getLegalMoves(Role role) throws MoveDefinitionException {
		if (!legalMoves.containsKey(role)) throw new MoveDefinitionException(this, role);
		return legalMoves.get(role);
	}

	public void setLegalMoves(Map<Role, List<Move>> moves) {
		legalMoves = moves;
	}

	public int getGoalValue(Role role) throws GoalDefinitionException {
		if (!goalValues.containsKey(role)) throw new GoalDefinitionException(this, role);
		return goalValues.get(role);
	}

	public void setGoalValues(Map<Role, Integer> values) {
		goalValues = values;
	}

    @Override
    public MachineState clone() {
        PropNetMachineState state = new PropNetMachineState(new HashSet<Proposition>(bases));
        state.setMarked(isMarked);
        state.setTerminal(isTerminal);
        state.setLegalMoves(legalMoves);
        state.setGoalValues(goalValues);
        return state;
    }

    /* Utility methods */
    @Override
    public int hashCode()
    {
        return getBaseMarking().hashCode();
    }

    @Override
    public String toString()
    {
        if(baseMarking == null)
            return "(PropNetMachineState with null baseMarking)";
        else
            return baseMarking.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if ((o != null) && (o instanceof PropNetMachineState))
        {
            PropNetMachineState state = (PropNetMachineState) o;
            return state.getBaseMarking().equals(getBaseMarking());
        }
        return false;
    }

}
