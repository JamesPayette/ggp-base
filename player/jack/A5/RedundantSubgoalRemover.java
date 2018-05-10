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
import org.ggp.base.util.gdl.grammar.GdlVariable;

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

		for (GdlLiteral candidate: body) {
			Set<GdlLiteral> other = new HashSet<GdlLiteral>(body);
			other.remove(candidate);
			Set<GdlVariable> vars = GdlUtil.findVars(other);
			Set<GdlConstant> consts = GdlUtil.findConsts(other);
			vars.addAll(headVars);
			consts.addAll(headConsts);
			GdlMapping mapping = new GdlMapping(consts, vars);
			Set<Gdl> mappedOther = GdlUtil.applyMapping(mapping, other);
			Gdl mappedCandidate = GdlUtil.applyMapping(mapping, candidate);
			if (!GdlUtil.proveable(mappedCandidate, mappedOther)) newBody.add(candidate);
		}
		return GdlPool.getRule(rule.getHead(), newBody);
	}

}
