package jack.A7;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ggp.base.util.propnet.architecture.components.Proposition;

public class PropNetMarking {
	private Map<Proposition, Boolean> marking;

	public PropNetMarking(Collection<Proposition> bases) {
		marking = new HashMap<Proposition, Boolean>();
		for(Proposition base : bases) {
			marking.put(base, base.getValue());
		}
	}

	public boolean get(Proposition p) {
		Boolean b = marking.get(p);
		if (b == null) {
			System.out.print("Proposition not found while restoring a state");
			return false;
		}
		return b;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((marking == null) ? 0 : marking.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PropNetMarking other = (PropNetMarking) obj;
		if (marking == null) {
			if (other.marking != null)
				return false;
		} else if (!marking.equals(other.marking))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String retVal = "";
		for (Map.Entry<Proposition, Boolean> entry : marking.entrySet()) {
			retVal += "|" + entry.getKey().toString() + ":" + entry.getValue().toString();
		}
		return retVal;
	}

}
