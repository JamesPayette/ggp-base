package jack.A7;

import java.util.Collection;
import java.util.HashSet;

import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;

public class PropNetMachineState extends MachineState {

	private PropNetMarking baseMarking;
	private Collection<Proposition> bases;

	public PropNetMachineState() {
		baseMarking = null;
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


    @Override
    public MachineState clone() {
        return new PropNetMachineState(new HashSet<Proposition>(bases));
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
