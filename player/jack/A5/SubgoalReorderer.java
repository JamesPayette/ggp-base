package jack.A5;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlVariable;

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
		Set<GdlVariable> boundVars = new HashSet<GdlVariable>();
		while(!body.isEmpty()) {
			GdlLiteral next = findNextLiteral(body, boundVars);
			body.remove(next);
			newBody.add(next);
		}
		GdlRule newGdl = GdlPool.getRule(rule.getHead(), newBody);
		return newGdl;
	}

	private static GdlLiteral findNextLiteral(List<GdlLiteral> literals, Set<GdlVariable> boundVars) {
		GdlLiteral bestLiteral = null;
		Set<GdlVariable> bestVars = null;
		for (GdlLiteral literal : literals) {
			Set<GdlVariable> vars = GdlUtil.findVars(literal);
			vars.removeAll(boundVars);
			if (bestLiteral == null || vars.size() <= bestVars.size()) {
				bestLiteral = literal;
				bestVars = vars;
			}
			if (bestVars.size() == 0) break;
		}
		boundVars.addAll(bestVars);
		return bestLiteral;
	}

}
