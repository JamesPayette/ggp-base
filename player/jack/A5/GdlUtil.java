package jack.A5;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.prover.aima.substituter.Substituter;
import org.ggp.base.util.prover.aima.substitution.Substitution;

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

	// ------------- Apply Substitution -----------------------//

	public static Set<GdlLiteral> applySubstitution(Set<GdlLiteral> literals, Substitution theta) {
		Set<GdlLiteral> literalsSub = new HashSet<GdlLiteral>();
		for(GdlLiteral literal : literals) {
			literalsSub.add(Substituter.substitute(literal, theta));
		}
		return literalsSub;
	}

	public static GdlLiteral applySubstitution(GdlLiteral literal, Substitution theta) {
		return Substituter.substitute(literal, theta);
	}

}
