package jack.A5;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.prover.aima.substitution.Substitution;
import org.ggp.base.util.prover.aima.unifier.Unifier;

public class RedundantRuleRemover {

	public static List<Gdl> optimize(List<Gdl> rules) {
		List<Gdl> newRules = new ArrayList<Gdl>();
		List<Gdl> remainingRules = new ArrayList<Gdl>(rules);
		for (Gdl gdl : rules) {
			remainingRules.remove(gdl);
			if (gdl instanceof GdlRule) {
				GdlRule rule = (GdlRule) gdl;
				if (!subsumed(rule, newRules) && !subsumed(rule, remainingRules)) newRules.add(rule);
			} else {
				newRules.add(gdl);
			}
		}
		return newRules;
	}

	private static boolean subsumed(GdlRule rule, List<Gdl> others) {
		for (Gdl other : others) {
			if (other instanceof GdlRule && subsumed(rule, (GdlRule) other)) return true;
		}
		return false;
	}

	private static boolean subsumed(GdlRule rule, GdlRule other) {
		GdlSentence ruleHead = rule.getHead();
		GdlSentence otherHead = other.getHead();
		Substitution theta = Unifier.unify(ruleHead, otherHead);
		if (theta == null || rule.arity() != other.arity()) return false;
		List<GdlLiteral> ruleBody = rule.getBody();
		List<GdlLiteral> otherBody = other.getBody();
		Set<GdlLiteral> ruleBodySet = new HashSet<GdlLiteral>(ruleBody);
		Set<GdlLiteral> otherBodySet = new HashSet<GdlLiteral>(otherBody);
		ruleBodySet = GdlUtil.applySubstitution(ruleBodySet, theta);
		otherBodySet = GdlUtil.applySubstitution(otherBodySet, theta);
		for (GdlLiteral literal : ruleBodySet) {
			if (!findMapping(literal, otherBodySet, theta)) return false;
			ruleBodySet = GdlUtil.applySubstitution(ruleBodySet, theta);
			otherBodySet = GdlUtil.applySubstitution(otherBodySet, theta);
		}
		return true;
	}

	private static boolean findMapping(GdlLiteral literal, Set<GdlLiteral> others, Substitution theta) {
		if (literal instanceof GdlSentence) findMapping((GdlSentence) literal, others, theta);
		return others.contains(literal);
	}

	private static boolean findMapping(GdlSentence literal, Set<GdlLiteral> others, Substitution theta) {
		for (GdlLiteral other: others) {
			if (other instanceof GdlSentence) {
				Substitution newTheta = Unifier.unify(literal, (GdlSentence) other);
				if (newTheta == null) continue;
				theta.compose(newTheta);
				return true;
			}
		}
		return false;
	}
}
