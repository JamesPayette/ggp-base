package jack.A5;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;

public class GdlMapping {
	private int counter = 0;

	private Map<GdlVariable, GdlConstant> mapping;
	private Set<GdlConstant> consts;

	public GdlMapping(Set<GdlConstant> consts, Set<GdlVariable> vars) {
		this.consts = consts;
		this.mapping = new HashMap<GdlVariable, GdlConstant>();
		for(GdlVariable key : vars) {
			GdlConstant value = getNewConstant();
			consts.add(value);
			mapping.put(key, value);
		}
	}

	private GdlConstant getNewConstant() {
		while(true) {
			GdlConstant next = GdlPool.getConstant("x_" + counter++);
			if(!consts.contains(next)) return next;
		}
	}

	public GdlTerm get(GdlVariable var) {
		if (mapping.containsKey(var)) {
			return mapping.get(var);
		}
		return var;
	}

}
