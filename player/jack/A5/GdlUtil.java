package jack.A5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;

public class GdlUtil {

	// ------------- Find Variables -----------------------//

	public static Set<GdlVariable> findVars(Collection<GdlLiteral> literals) {
		Set<GdlVariable> vars = new HashSet<GdlVariable>();
		for (GdlLiteral literal: literals) {
			vars.addAll(findVars(literal));
		}
		return vars;
	}

	public static Set<GdlVariable> findVars(GdlLiteral literal) {
		if (literal instanceof GdlSentence) {
			return findVars((GdlSentence) literal);
		} else if (literal instanceof GdlNot) {
			return findVars((GdlNot) literal);
		} else if (literal instanceof GdlDistinct) {
			return findVars((GdlDistinct) literal);
		} else if (literal instanceof GdlOr) {
			return findVars((GdlOr) literal);
		} else {
			System.out.println("Unable to cast GdlLiteral");
			return new HashSet<GdlVariable>();
		}
	}

	public static Set<GdlVariable> findVars(GdlSentence sentence) {
		if (sentence instanceof GdlProposition) {
			return new HashSet<GdlVariable>();
		} else if (sentence instanceof GdlRelation) {
			return findVars((GdlRelation) sentence);
		} else {
			System.out.println("Unable to cast GdlSentence");
			return new HashSet<GdlVariable>();
		}
	}

	public static Set<GdlVariable> findVars(GdlRelation relation) {
		Set<GdlVariable> vars = new HashSet<GdlVariable>();
		for (GdlTerm term: relation.getBody()) {
			vars.addAll(findVars(term));
		}
		return vars;
	}

	public static Set<GdlVariable> findVars(GdlTerm term) {
		Set<GdlVariable> vars = new HashSet<GdlVariable>();
		if (term instanceof GdlConstant) {
			return vars;
		} else if (term instanceof GdlVariable) {
			vars.add((GdlVariable) term);
		} else if (term instanceof GdlFunction) {
			vars.addAll(findVars((GdlFunction) term));
		} else {
			System.out.println("Unable to cast GdlTerm");
		}
		return vars;
	}

	public static Set<GdlVariable> findVars(GdlFunction function) {
		Set<GdlVariable> vars = new HashSet<GdlVariable>();
		for (GdlTerm term: function.getBody()) {
			vars.addAll(findVars(term));
		}
		return vars;
	}

	public static Set<GdlVariable> findVars(GdlNot not) {
		return findVars(not.getBody());
	}

	public static Set<GdlVariable> findVars(GdlDistinct distinct) {
		Set<GdlVariable> vars = new HashSet<GdlVariable>();
		vars.addAll(findVars(distinct.getArg1()));
		vars.addAll(findVars(distinct.getArg2()));
		return vars;
	}

	public static Set<GdlVariable> findVars(GdlOr or) {
		Set<GdlVariable> vars = new HashSet<GdlVariable>();
		for (GdlLiteral literal: or.getDisjuncts()) {
			vars.addAll(findVars(literal));
		}
		return vars;
	}

	// ------------- Find Constants -----------------------//

	public static Set<GdlConstant> findConsts(Collection<GdlLiteral> literals) {
		Set<GdlConstant> consts = new HashSet<GdlConstant>();
		for (GdlLiteral literal: literals) {
			consts.addAll(findConsts(literal));
		}
		return consts;
	}

	public static Set<GdlConstant> findConsts(GdlLiteral literal) {
		if (literal instanceof GdlSentence) {
			return findConsts((GdlSentence) literal);
		} else if (literal instanceof GdlNot) {
			return findConsts((GdlNot) literal);
		} else if (literal instanceof GdlDistinct) {
			return findConsts((GdlDistinct) literal);
		} else if (literal instanceof GdlOr) {
			return findConsts((GdlOr) literal);
		} else {
			System.out.println("Unable to cast GdlLiteral");
			return new HashSet<GdlConstant>();
		}
	}

	public static Set<GdlConstant> findConsts(GdlSentence sentence) {
		if (sentence instanceof GdlProposition) {
			return findConsts((GdlProposition) sentence);
		} else if (sentence instanceof GdlRelation) {
			return findConsts((GdlRelation) sentence);
		} else {
			System.out.println("Unable to cast GdlSentence");
			return new HashSet<GdlConstant>();
		}
	}

	public static Set<GdlConstant> findConsts(GdlProposition proposition) {
		Set<GdlConstant> consts = new HashSet<GdlConstant>();
		for (GdlTerm term: proposition.getBody()) {
			consts.addAll(findConsts(term));
		}
		return consts;
	}

	public static Set<GdlConstant> findConsts(GdlRelation relation) {
		Set<GdlConstant> consts = new HashSet<GdlConstant>();
		for (GdlTerm term: relation.getBody()) {
			consts.addAll(findConsts(term));
		}
		return consts;
	}

	public static Set<GdlConstant> findConsts(GdlTerm term) {
		Set<GdlConstant> consts = new HashSet<GdlConstant>();
		if (term instanceof GdlConstant) {
			consts.add((GdlConstant) term);
		} else if (term instanceof GdlVariable) {
			return consts;
		} else if (term instanceof GdlFunction) {
			consts.addAll(findConsts((GdlFunction) term));
		} else {
			System.out.println("Unable to cast GdlTerm");
		}
		return consts;
	}

	public static Set<GdlConstant> findConsts(GdlFunction function) {
		Set<GdlConstant> consts = new HashSet<GdlConstant>();
		for (GdlTerm term: function.getBody()) {
			consts.addAll(findConsts(term));
		}
		return consts;
	}

	public static Set<GdlConstant> findConsts(GdlNot not) {
		return findConsts(not.getBody());
	}

	public static Set<GdlConstant> findConsts(GdlDistinct distinct) {
		Set<GdlConstant> consts = new HashSet<GdlConstant>();
		consts.addAll(findConsts(distinct.getArg1()));
		consts.addAll(findConsts(distinct.getArg2()));
		return consts;
	}

	public static Set<GdlConstant> findConsts(GdlOr or) {
		Set<GdlConstant> consts = new HashSet<GdlConstant>();
		for (GdlLiteral literal: or.getDisjuncts()) {
			consts.addAll(findConsts(literal));
		}
		return consts;
	}

	// ------------- Apply Mapping -----------------------//

	public static Set<Gdl> applyMapping(GdlMapping mapping, Collection<GdlLiteral> literals) {
		Set<Gdl> mapped = new HashSet<Gdl>();
		for (GdlLiteral literal : literals) {
			mapped.add(applyMapping(mapping, literal));
		}
		return mapped;
	}

	public static Gdl applyMapping(GdlMapping mapping, GdlLiteral literal) {
		if (literal instanceof GdlSentence) {
			return applyMapping(mapping, (GdlSentence) literal);
		} else if (literal instanceof GdlNot) {
			return applyMapping(mapping, (GdlNot) literal);
		} else if (literal instanceof GdlDistinct) {
			return applyMapping(mapping, (GdlDistinct) literal);
		} else if (literal instanceof GdlOr) {
			return applyMapping(mapping, (GdlOr) literal);
		} else {
			System.out.println("Unable to cast GdlLiteral");
			return literal;
		}
	}

	public static Gdl applyMapping(GdlMapping mapping, GdlSentence sentence) {
		if (sentence instanceof GdlProposition) {
			return sentence;
		} else if (sentence instanceof GdlRelation) {
			return applyMapping(mapping, (GdlRelation) sentence);
		} else {
			System.out.println("Unable to cast GdlSentence");
			return sentence;
		}
	}

	public static Gdl applyMapping(GdlMapping mapping, GdlRelation relation) {
		List<GdlTerm> newBody = new ArrayList<GdlTerm>();
		for (GdlTerm term: relation.getBody()) {
			newBody.add((GdlTerm) applyMapping(mapping, term));
		}
		return GdlPool.getRelation(relation.getName(), newBody);
	}

	public static Gdl applyMapping(GdlMapping mapping, GdlTerm term) {
		if (term instanceof GdlConstant) {
			return term;
		} else if (term instanceof GdlVariable) {
			return mapping.get((GdlVariable) term);
		} else if (term instanceof GdlFunction) {
			return applyMapping(mapping, (GdlFunction) term);
		} else {
			System.out.println("Unable to cast GdlTerm");
			return term;
		}
	}

	public static Gdl applyMapping(GdlMapping mapping, GdlFunction function) {
		List<GdlTerm> newBody = new ArrayList<GdlTerm>();
		for (GdlTerm term: function.getBody()) {
			newBody.add((GdlTerm) applyMapping(mapping, term));
		}
		return GdlPool.getFunction(function.getName(), newBody);
	}

	public static Gdl applyMapping(GdlMapping mapping, GdlNot not) {
		return GdlPool.getNot((GdlLiteral) applyMapping(mapping, not.getBody()));
	}

	public static Gdl applyMapping(GdlMapping mapping, GdlDistinct distinct) {
		GdlTerm arg1 = (GdlTerm) applyMapping(mapping, distinct.getArg1());
		GdlTerm arg2 = (GdlTerm) applyMapping(mapping, distinct.getArg2());
		return GdlPool.getDistinct(arg1, arg2);
	}

	public static Gdl applyMapping(GdlMapping mapping, GdlOr or) {
		List<GdlLiteral> newDisjuncts = new ArrayList<GdlLiteral>();
		for (GdlLiteral literal: or.getDisjuncts()) {
			newDisjuncts.add((GdlLiteral) applyMapping(mapping, literal));
		}
		return GdlPool.getOr(newDisjuncts);
	}

	public static boolean proveable(Gdl test, Set<Gdl> againsts) {
		if (againsts.contains(test)) return true;
		for(Gdl against : againsts) {
			if (proveable((GdlLiteral) test, (GdlLiteral) against)) return true;
		}
		return false;
	}

	public static boolean proveable(Gdl test, Gdl against) {
		if (test instanceof GdlLiteral && against instanceof GdlLiteral) {
			return proveable((GdlLiteral) test, (GdlLiteral) against);
		}
		return false;
	}

	public static boolean proveable(GdlLiteral test, GdlLiteral against) {
		if (test instanceof GdlDistinct && against instanceof GdlDistinct) {
			return proveable((GdlDistinct) test, (GdlDistinct) against);
		} else if (test instanceof GdlNot && against instanceof GdlNot) {
			return proveable((GdlNot) test, (GdlNot) against);
		} else if (test instanceof GdlOr && against instanceof GdlOr) {
			return proveable((GdlOr) test, (GdlOr) against);
		} else if (test instanceof GdlSentence && against instanceof GdlSentence) {
			return proveable((GdlSentence) test, (GdlSentence) against);
		}
		return false;
	}

	public static boolean proveable(GdlDistinct test, GdlDistinct against) {
		if(!proveable(test.getArg1(), against.getArg1())) return false;
		if(!proveable(test.getArg2(), against.getArg2())) return false;
		return true;
	}

	public static boolean proveable(GdlNot test, GdlNot against) {
		return proveable(test.getBody(), against.getBody());
	}

	public static boolean proveable(GdlOr test, GdlOr against) {
		int testLen = test.arity();
		int againstLen = test.arity();
		if (testLen != againstLen) return false;
		for(int i = 0; i < testLen; i++) {
			if(!proveable(test.get(i), against.get(i))) return false;
		}
		return true;
	}

	public static boolean proveable(GdlSentence test, GdlSentence against) {
		int testLen = test.arity();
		int againstLen = against.arity();
		if (testLen != againstLen) return false;
		for(int i = 0; i < testLen; i++) {
			if(!proveable(test.get(i), against.get(i))) return false;
		}
		return true;
	}

	public static boolean proveable(GdlTerm test, GdlTerm against) {
		if(test instanceof GdlVariable) return true;
		if(test instanceof GdlConstant && against instanceof GdlConstant) return test == against;
		if(test instanceof GdlFunction && against instanceof GdlFunction) return proveable((GdlFunction) test, (GdlFunction) against);
		return false;
	}

	public static boolean proveable(GdlFunction test, GdlFunction against) {
		int testLen = test.arity();
		int againstLen = against.arity();
		if (testLen != againstLen) return false;
		for(int i = 0; i < testLen; i++) {
			if(!proveable(test.get(i), against.get(i))) return false;
		}
		return true;
	}

}
