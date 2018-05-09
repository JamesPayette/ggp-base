package jack.A5;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;

public class SubgoalReorderer {

	public static List<Gdl> optimize(List<Gdl> rules) {
		List<Gdl> newRules = new ArrayList<Gdl>();
		for (Gdl gdl : rules) {
			if (gdl instanceof GdlRule) {
				GdlRule rule = (GdlRule) gdl;
				rule = reorderSubgoals(rule);
				newRules.add(rule);
			} else {
				newRules.add(gdl);
			}
		}
		return newRules;
	}

	private static GdlRule reorderSubgoals(GdlRule rule) {
		List<GdlLiteral> newBody = new ArrayList<GdlLiteral>();
		List<GdlLiteral> body = new ArrayList<GdlLiteral>(rule.getBody());
		while(!body.isEmpty()) {
			GdlLiteral next = findNextLiteral(body, newBody);
			body.remove(next);
			newBody.add(next);
		}
		GdlRule newGdl = GdlPool.getRule(rule.getHead(), newBody);
		return newGdl;
	}

	private static GdlLiteral findNextLiteral(List<GdlLiteral> oldLiterals, List<GdlLiteral> newLiterals) {
		for (GdlLiteral literal : oldLiterals) {
			int unboundVars = findUnboundVars(literal, newLiterals);
			if (unboundVars == 0) return literal;
		}
		return oldLiterals.get(0);
	}

	private static int findUnboundVars(GdlLiteral literal, List<GdlLiteral> newLiterals) {
		Set<String> bound = findVars(newLiterals);
		Set<String> ruleVars = findVars(literal);
		ruleVars.removeAll(bound);
		return ruleVars.size();
	}

	private static Set<String> findVars(List<GdlLiteral> newLiterals) {
		Set<String> bound = new HashSet<String>();
		for (GdlLiteral literal : newLiterals) {
			bound.addAll(findVars(literal));
		}
		return bound;
	}

	private static Set<String> findVars(GdlLiteral literal) {
		Set<String> bound = new HashSet<String>();
		String literalString = literal.toString();
		while(true) {
			int startIndex = literalString.indexOf('?');
			if (startIndex == -1) break;
			int endIndex = literalString.indexOf(' ', startIndex);
			bound.add(literalString.substring(startIndex, endIndex));
			literalString = literalString.substring(endIndex);
		}
		return bound;
	}

}
