package jack.A5;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.prover.aima.substitution.Substitution;
import org.ggp.base.util.prover.aima.unifier.Unifier;

public class RedundantSubgoalRemover {

	public static List<Gdl> optimize(List<Gdl> rules) {
		List<Gdl> newRules = new ArrayList<Gdl>();
		for (Gdl gdl : rules) {
			if (gdl instanceof GdlRule) {
				GdlRule rule = (GdlRule) gdl;
				rule = removeRedundantSubgoals(rule);
				newRules.add(rule);
			} else {
				newRules.add(gdl);
			}
		}
		return newRules;
	}

	private static GdlRule removeRedundantSubgoals(GdlRule rule) {
		List<GdlLiteral> newBody = new ArrayList<GdlLiteral>();
		List<GdlLiteral> body = new ArrayList<GdlLiteral>(rule.getBody());

		Set<GdlVariable> headVars = GdlUtil.findVars(rule.getHead());
		Set<GdlConstant> headConsts = GdlUtil.findConsts(rule.getHead());

		for (GdlLiteral candidate : body) {
			Set<GdlLiteral> other = new HashSet<GdlLiteral>(body);
			other.remove(candidate);
			Set<GdlVariable> vars = GdlUtil.findVars(other);
			Set<GdlConstant> consts = GdlUtil.findConsts(other);
			vars.addAll(headVars);
			consts.addAll(headConsts);
			Substitution theta = new Substitution();
			fillSubstitution(theta, vars, consts);
			Set<GdlLiteral> otherSub = GdlUtil.applySubstitution(other, theta);
			GdlLiteral candidateSub = GdlUtil.applySubstitution(candidate, theta);
			if (!proveable(candidateSub, otherSub))
				newBody.add(candidate);
		}
		return GdlPool.getRule(rule.getHead(), newBody);
	}

	private static void fillSubstitution(Substitution sub, Set<GdlVariable> vars, Set<GdlConstant> consts) {
		int counter = 0;
		for (GdlVariable var : vars) {
			while (true) {
				GdlConstant next = GdlPool.getConstant("x_" + counter++);
				if (!consts.contains(next)) {
					sub.put(var, next);
					consts.add(next);
					break;
				}
			}
		}
	}

	private static boolean proveable(GdlLiteral candidate, Set<GdlLiteral> others) {
		if (candidate instanceof GdlSentence)
			return proveable((GdlSentence) candidate, others);
		return others.contains(candidate);
	}

	private static boolean proveable(GdlSentence candidate, Set<GdlLiteral> others) {
		for (GdlLiteral other : others) {
			if (other instanceof GdlSentence) {
				if (Unifier.unify(candidate, (GdlSentence) other) != null)
					return true;
			}
		}
		return false;
	}

}
