//////////////////////////////////////////////////////////////////////
//
// Extended Algebraic Decision Diagrams Package
// Expressions Library 
// Defines arithmetic and comparison expressions
// using symbolic variables. 
//
// @author Scott Sanner (ssanner@gmail.com)
// @author Zahra Zamani
// @author Luis Vianna
//////////////////////////////////////////////////////////////////////

package xadd;

import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import logic.kb.fol.FOPC;
import xadd.XADD;

public abstract class ExprLib {

    public enum ArithOperation {
        UND("UND"),
        SUM("+"),
        MINUS("-"),
        PROD("*"),
        DIV("/"),
        ERROR("ERROR");

        private final String value;

        ArithOperation(String value) {
            this.value = value;
        }

        public String toString() {
            return this.value;
        }

        public static int toXADDOper(ArithOperation op) {
            switch (op) {
                case SUM:
                    return XADD.SUM;
                case MINUS:
                    return XADD.MINUS;
                case PROD:
                    return XADD.PROD;
                case DIV:
                    return XADD.DIV;
                case UND:
                    return XADD.UND;
                default:
                    return XADD.ERROR;
            }
        }

        public static ArithOperation fromXADDOper(int op) {
            switch (op) {
                case XADD.SUM:
                    return SUM;
                case XADD.MINUS:
                    return MINUS;
                case XADD.PROD:
                    return PROD;
                case XADD.DIV:
                    return DIV;
                case XADD.UND:
                    return UND;
                default:
                    return ERROR;
            }
        }
    }

    public enum CompOperation {
        UND("UND"),
        GT(">"),
        GT_EQ(">="),
        LT("<"),
        LT_EQ("<="),
        EQ("=="),
        NEQ("!=");

        private final String value;

        CompOperation(String value) {
            this.value = value;
        }

        public String toString() {
            return this.value;
        }

    }


    //Expr constants
    public final static ArithExpr ZERO = new DoubleExpr(0d);
    public final static ArithExpr ONE = new DoubleExpr(1d);
    public final static ArithExpr NEG_ONE = new DoubleExpr(-1d);
    public final static ArithExpr POS_INF = new DoubleExpr(Double.POSITIVE_INFINITY);
    public final static ArithExpr NEG_INF = new DoubleExpr(Double.NEGATIVE_INFINITY);
    public final static ArithExpr NAN = new DoubleExpr(Double.NaN);

    public final static DecimalFormat _df = new DecimalFormat("#.########");
    public final static DecimalFormat _df_unformatted = new DecimalFormat("#.########");


    @SuppressWarnings("rawtypes")
    public static abstract class Expr implements Comparable<Expr> {

        public static final Class DOUBLE_CLASS = DoubleExpr.class;
        public static final Class VAR_CLASS = VarExpr.class;
        public static final Class ARITH_CLASS = ArithExpr.class;
        public static final Class OPER_CLASS = OperExpr.class;
        public static final Class COMP_CLASS = CompExpr.class;
        public static final Class DELTA_FUN_CLASS = DeltaFunExpr.class;

        public static HashMap<Class, Integer> _class2order = new HashMap<Class, Integer>();

        static {
            _class2order.put(DOUBLE_CLASS, 0);
            _class2order.put(VAR_CLASS, 1);
            _class2order.put(ARITH_CLASS, 2);
            _class2order.put(OPER_CLASS, 3);
            _class2order.put(COMP_CLASS, 4);
            _class2order.put(DELTA_FUN_CLASS, 100); // Always last
        }

        public abstract Expr makeCanonical();

        public int compareTo(Expr o) {
            // Var, Double, Arith, Oper, Comp
            Class this_class = this.getClass();
            Class other_class = o.getClass();

            if (!this_class.equals(other_class)) {
                Integer rank_this = _class2order.get(this_class);
                Integer rank_other = _class2order.get(other_class);
                if (rank_this == null)
                    rank_this = 5;  // Put function expressions after main arithmetic expressions (0-4)
                if (rank_other == null) rank_other = 5;
                return rank_this - rank_other;
            } else {
                if (this.equals(o))
                    return 0;
                else {
                    if (this_class == OPER_CLASS && other_class == OPER_CLASS && 
                        (((OperExpr) this)._type == ArithOperation.PROD) && 
                        (((OperExpr) o)._type ==ArithOperation.PROD) ){
                        //Comparing two product terms, place smaller first and if same lenght order by var first, last by Double 
                        OperExpr thisTerm = (OperExpr) this;
                        OperExpr otherTerm = (OperExpr) o;
                        if ( thisTerm._terms.get(0) instanceof DoubleExpr &&
                             otherTerm._terms.get(0) instanceof DoubleExpr){
                            if (thisTerm._terms.size() < otherTerm._terms.size()) return -1;
                            if (thisTerm._terms.size() > otherTerm._terms.size()) return 1;
                            for(int i = thisTerm._terms.size()-1; i>0;i--){
                                if ( thisTerm._terms.get(i) instanceof VarExpr &&
                                     otherTerm._terms.get(i) instanceof VarExpr){
                                        int thisVar= ((VarExpr)thisTerm._terms.get(i)).hashCode();
                                        int oVar= ((VarExpr)otherTerm._terms.get(i)).hashCode();
                                        if (thisVar < oVar) return -1;
                                        if (thisVar > oVar) return 1; // Not -1!
                                }
                            }
                            double thisVal = ((DoubleExpr) thisTerm._terms.get(0))._dConstVal;
                            double oVal = ((DoubleExpr) otherTerm._terms.get(0))._dConstVal;
                            if (thisVal < oVal) return -1;
                            if (thisVal > oVal) return 1; // Not -1!
                        }
                    }

                    if (this.hashCode() < o.hashCode()) return -1;
                    if (this.hashCode() > o.hashCode()) return 1;
                    return 0;
                }
            }
        }
    }

    //Comparable Expressions
    public static class CompExpr extends Expr {

        public CompOperation _type = CompOperation.UND;
        public ArithExpr _lhs = null;
        public ArithExpr _rhs = null;

        public CompExpr(CompOperation type, ArithExpr lhs, ArithExpr rhs) {
            _type = type;
            _lhs = lhs;
            _rhs = rhs;
        }

        public boolean isGreater(){
            return (_type == CompOperation.GT || _type == CompOperation.GT_EQ);
        }
        public String toString(boolean format) {
            return _lhs.toString(format) + " " + _type + " "
                    + _rhs.toString(format);
        }

        public static CompOperation flipCompOper(CompOperation comp_oper) {
            switch (comp_oper) {
                case GT:
                    return CompOperation.LT_EQ;
                case GT_EQ:
                    return CompOperation.LT;
                case LT:
                    return CompOperation.GT_EQ;
                case LT_EQ:
                    return CompOperation.GT;
                case EQ:
                    return CompOperation.NEQ;
                case NEQ:
                    return CompOperation.EQ;
                default:
                    return CompOperation.UND;
            }
        }

        public static CompExpr ParseCompExpr(String s) {
            try {
                FOPC.Node res = FOPC.parse(s);
                return Convert2CompExpr((FOPC.PNode) res);
            } catch (Exception f) {
                return null;
            }
        }

        public static CompExpr Convert2CompExpr(FOPC.PNode res) {
            CompOperation type = CompOperation.UND;
            if (res._nPredID != FOPC.PNode.INVALID) {
                switch (res._nPredID) {
                    case FOPC.PNode.EQUALS: {
                        type = res._bIsNegated ? CompOperation.NEQ : CompOperation.EQ;
                    }
                    break;
                    case FOPC.PNode.LESS: {
                        type = res._bIsNegated ? CompOperation.GT_EQ : CompOperation.LT;
                    }
                    break;
                    case FOPC.PNode.LESSEQ: {
                        type = res._bIsNegated ? CompOperation.GT : CompOperation.LT_EQ;
                    }
                    break;
                }
            }
            ArithExpr lhs = ArithExpr.Convert2ArithExpr((FOPC.Term) res.getBinding(0));
            ArithExpr rhs = ArithExpr.Convert2ArithExpr((FOPC.Term) res.getBinding(1));
            if (lhs == null || rhs == null || type == CompOperation.UND)
                return null;
            else
                return new CompExpr(type, lhs, rhs);
        }

        // This ignores the difference between strict and non-strict inequality... technically
        // requires a continuous function, or if piecewise, could have errors at piece boundaries.
        public Expr makeCanonical() {

            // 1. Expressions all zero on RHS of comparisons and restrict
            // symbols:
            // a < b : b > a
            // a <= b : b >= a

            // Enforce all inequalities to be >
            CompExpr new_expr = new CompExpr(_type, _lhs, _rhs);
            switch (new_expr._type) {
                case GT_EQ:
                    new_expr._type = CompOperation.GT;
                    // Do not swap lhs and rhs -- just collapsing >= to >
                    break;
                case LT:
                case LT_EQ:
                    new_expr._type = CompOperation.GT;
                    // Swap lhs and rhs to counter the inequality switch
                    new_expr._lhs = _rhs;
                    new_expr._rhs = _lhs;
                    break;
                case EQ:
                case NEQ:
                    System.err.println("WARNING: XADDs should not use NEQ/EQ EXPR... can substitute on EQ: " + new_expr);
                    break;
            }

            //        // TREATMENT OF DIVISION -- NOT CURRENTLY BEING USED BUT LEAVING IN
            //        // SINCE CRUCIAL IF WORKING WITH POLYNOMIAL FRACTIONS IN FUTURE
            //
            //        // find first divisor on lhs
            //        // only change the decision of the expr, not the _lhs,rhs
            //        // NOTE: it does not matter which side the divisor belongs to, a
            //        // negative expr changes the sign, a positive one does not.
            //        // indicate that division has been multiplied
            //        ArithExpr div = null;
            //
            //        CompExpr temp_expr = new CompExpr(new_expr._type, new_expr._lhs,
            //                new_expr._rhs);
            //        do {
            //            div = null;
            //            if (temp_expr._lhs instanceof OperExpr)
            //                div = checkDivisor((OperExpr) temp_expr._lhs, div);
            //            if (div != null) {
            //                // CANONICAL_DIVISOR.add(div);
            //                // left side
            //                if (showDiv.size() > 0) {
            //                    for (int i = 0; i < showDiv.size(); i++)
            //                        if (showDiv.get(i) != div)
            //                            showDiv.add(div);
            //                } else
            //                    showDiv.add(div);
            //                temp_expr._lhs = removeDivFromOtherTerms(
            //                        (OperExpr) temp_expr._lhs, div);
            //                temp_expr._lhs = (ArithExpr) temp_expr._lhs.makeCanonical();
            //                // we have multiplied the lhs but not the rhs, just multiply
            //                // it
            //                if (temp_expr._rhs instanceof OperExpr)
            //                    temp_expr._rhs = removeDivFromOtherTerms((OperExpr) temp_expr._rhs, div);
            //                else if (temp_expr._rhs instanceof OperExpr)
            //                    temp_expr._rhs = (OperExpr) ArithExpr.op(temp_expr._rhs, div, PROD);
            //                temp_expr._rhs = (ArithExpr) temp_expr._rhs.makeCanonical();
            //
            //            }
            //        } while (div != null);
            //
            //        do {
            //            div = null;
            //            if (temp_expr._rhs instanceof OperExpr)
            //                div = checkDivisor((OperExpr) temp_expr._rhs, div);
            //            if (div != null) {
            //                // CANONICAL_DIVISOR.add(div);
            //                if (showDiv.size() > 0) {
            //                    for (int i = 0; i < showDiv.size(); i++)
            //                        if (showDiv.get(i) != div)
            //                            showDiv.add(div);
            //                } else
            //                    showDiv.add(div);
            //                temp_expr._rhs = removeDivFromOtherTerms(
            //                        (OperExpr) temp_expr._rhs, div);
            //                temp_expr._rhs = (ArithExpr) temp_expr._rhs.makeCanonical();
            //                // we have multiplied the lhs but not the rhs, just multiply
            //                // it
            //                if (temp_expr._lhs instanceof OperExpr)
            //                    temp_expr._lhs = removeDivFromOtherTerms(
            //                            (OperExpr) temp_expr._lhs, div);
            //                else if (temp_expr._lhs instanceof OperExpr)
            //                    temp_expr._lhs = (OperExpr) ArithExpr.op(temp_expr._lhs, div, PROD);
            //                temp_expr._lhs = (ArithExpr) temp_expr._lhs.makeCanonical();
            //
            //            }
            //        } while (div != null);
            //
            //        // System.out.println(">> CompExpr: makeCanonical: " + _lhs + " - "
            //        // + _rhs);
            //        ArithExpr new_lhs = ArithExpr.op(temp_expr._lhs, temp_expr._rhs,
            //                MINUS);
            //        new_lhs = (ArithExpr) new_lhs.makeCanonical();
            //        CompExpr current_expr = new CompExpr(temp_expr._type, new_lhs, ZERO);
            //        // System.out.println(">> CompExpr: makeCanonical: " + new_expr);
            //
            //        // divide all equation by coeff of first variable, invert type if
            //        // negative
            //        // if the prime versions appear, ignore!
            //        String contVar = null;
            //        boolean handlePrime = false;
            //        if (!(_alContinuousVars.isEmpty())) {
            //            for (int i = 0; i < _alContinuousVars.size(); i++) {
            //                contVar = _alContinuousVars.get(i);
            //                DoubleExpr doubleCoef = findVar(current_expr._lhs, contVar
            //                        + "'", false);
            //                if (doubleCoef != (DoubleExpr) ZERO) {
            //                    handlePrime = true;
            //                    break;
            //                }
            //            }
            //            // making sure that the primes are not considered
            //            if (!handlePrime) {
            //                for (int i = 0; i < _alContinuousVars.size(); i++) {
            //                    contVar = _alContinuousVars.get(i);
            //                    DoubleExpr doubleCoef = (DoubleExpr) ZERO;
            //                    // first look for x*x
            //                    if (HANDLE_NONLINEAR)
            //                        doubleCoef = findVar(current_expr._lhs, contVar,
            //                                true);
            //                    if (doubleCoef == (DoubleExpr) ZERO)
            //                        doubleCoef = findVar(current_expr._lhs, contVar,
            //                                false);
            //                    if (doubleCoef != (DoubleExpr) ZERO) {
            //                        boolean flip_comparison = false;
            //                        flip_comparison = (doubleCoef._dConstVal < 0d)
            //                                && (current_expr._type != EQ)
            //                                && (current_expr._type != NEQ);
            //
            //                        current_expr._lhs = (ArithExpr) (new OperExpr(PROD,
            //                                (ArithExpr.op(new DoubleExpr(1d),
            //                                        doubleCoef, DIV)),
            //                                        current_expr._lhs)).makeCanonical();
            //                        int comp_oper = current_expr._type;
            //                        if (flip_comparison)
            //                            switch (comp_oper) {
            //                            case CompOperation.GT:
            //                                current_expr._type = CompOperation.LT;
            //                                break;
            //                            case CompOperation.GT_EQ:
            //                                current_expr._type = CompOperation.LT_EQ;
            //                                break;
            //                            case CompOperation.LT:
            //                                current_expr._type = CompOperation.GT;
            //                                break;
            //                            case CompOperation.LT_EQ:
            //                                current_expr._type = CompOperation.GT_EQ;
            //                                break;
            //                            }
            //
            //                        // ((ExprDec) d)._expr = comp;
            //                        break;
            //                    }
            //                }
            //            }
            //        }

            ArithExpr new_lhs = ArithExpr.op(new_expr._lhs, new_expr._rhs, ArithOperation.MINUS);
            new_lhs = (ArithExpr) new_lhs.makeCanonical();
            if (XADD.NORMALIZE_DECISIONS) {
                if (new_lhs instanceof OperExpr) {
                    // Normalize uses the first coefficient, to divide all, this may lead to problem if the coefficients are later reordered 
                    new_lhs = ((OperExpr) new_lhs).normalize();
                    new_lhs = new_lhs.round();
                }
            }
            new_expr = new CompExpr(new_expr._type, new_lhs, ZERO);
            return new_expr;
        }

        //    // TREATMENT OF DIVISION -- NOT CURRENTLY BEING USED BUT LEAVING IN
        //    // SINCE CRUCIAL IF WORKING WITH POLYNOMIAL FRACTIONS IN FUTURE
        //    private OperExpr removeDivisor(OperExpr expr, ArithExpr div) {
        //        // removing the divisor term from an OperExr that occurs on the
        //        // lhs/rhs
        //        // operands can be operExr or DoubleExpr
        //        ArrayList<ArithExpr> local_terms = new ArrayList<ArithExpr>(
        //                expr._terms);
        //        local_terms.set(1, new DoubleExpr(1d));
        //        // expr._type = PROD;
        //        // expr = (OperExpr) expr._terms.get(0);
        //        return new OperExpr(expr._type, local_terms);
        //        // removing the divisor term from the expression
        //
        //    }
        //
        //    // steps to multiply all terms by the divisor
        //    private ArithExpr checkDivisor(OperExpr changing_expr, ArithExpr divisor) {
        //        if (divisor == null) {
        //            if (changing_expr._type == DIV) {
        //                divisor = (ArithExpr) changing_expr._terms.get(1);
        //                // (1)remove the divisor term from that term
        //                changing_expr = removeDivisor(changing_expr, divisor);
        //                // do not take division sign out for the next iteration
        //                return divisor;
        //
        //            }
        //
        //            else // have to go inside the expr
        //            {
        //                for (int i = 0; i < changing_expr._terms.size(); i++) {
        //                    if (changing_expr._terms.get(i) instanceof OperExpr)
        //                        divisor = checkDivisor(
        //                                (OperExpr) changing_expr._terms.get(i),
        //                                divisor);
        //                    if (divisor != null)
        //                        break;
        //                }
        //            }
        //        }
        //        return divisor;
        //    }
        //
        //    public OperExpr removeDivFromOtherTerms(OperExpr changing_expr,
        //            ArithExpr divisor) {
        //        OperExpr temp_expr = new OperExpr(changing_expr._type,
        //                changing_expr._terms);
        //        if (temp_expr._type == SUM)// we have to go in one level
        //        {
        //            ArrayList<ArithExpr> oper_list = new ArrayList<ArithExpr>();
        //            for (int i = 0; i < temp_expr._terms.size(); i++)
        //                if (temp_expr._terms.get(i) instanceof OperExpr)
        //                    oper_list.add(removeDivFromOtherTerms(
        //                            (OperExpr) temp_expr._terms.get(i), divisor));
        //                else if (temp_expr._terms.get(i) instanceof DoubleExpr)
        //                    oper_list.add(ArithExpr.op(temp_expr._terms.get(i),
        //                            divisor, PROD));
        //            for (int i = 0; i < oper_list.size(); i++)
        //                return new OperExpr(SUM, oper_list);
        //        }
        //        // found the first instance of divisor.search the rest of the expr
        //        // for the divisor
        //        // for (int j=0;j<changing_expr._terms.size();j++)
        //        // in the other statements there is either product or division (sum
        //        // of products, this level is the products level)
        //        else {
        //            if (temp_expr._type == DIV) {
        //                ArithExpr other_divisor = (ArithExpr) temp_expr._terms
        //                        .get(1);
        //                // (1)remove the divisor term from that term
        //                if (divisor.equals(other_divisor)) {
        //                    temp_expr = removeDivisor(temp_expr, divisor);
        //                    temp_expr._type = PROD;
        //                } else
        //                    temp_expr = (OperExpr) ArithExpr.op(temp_expr, divisor,
        //                            PROD);
        //            } else if (temp_expr._type == PROD) {
        //                boolean removedDiv = false;
        //                for (int k = 0; k < temp_expr._terms.size(); k++)
        //                    if (temp_expr._terms.get(k) instanceof OperExpr) {
        //                        if ((((OperExpr) temp_expr._terms.get(k))._type == DIV)
        //                                && (((OperExpr) temp_expr._terms.get(k))._terms
        //                                        .get(1).equals(divisor))) {
        //                            temp_expr._terms.set(
        //                                    k,
        //                                    removeDivisor(
        //                                            (OperExpr) temp_expr._terms
        //                                            .get(k), divisor));
        //                            ((OperExpr) temp_expr._terms.get(k))._type = PROD;
        //                            removedDiv = true;
        //                        } else if ((((OperExpr) temp_expr._terms.get(k))._type == DIV)
        //                                && (((OperExpr) temp_expr._terms.get(k))._terms
        //                                        .get(1).equals(1d)))// previously
        //                            // found divisor
        //                        {
        //                            ((OperExpr) temp_expr._terms.get(k))._type = PROD;
        //                            removedDiv = true;
        //                        }
        //
        //                    }// after if
        //                if (!removedDiv)
        //                    temp_expr = (OperExpr) (ArithExpr.op(temp_expr,
        //                            divisor, PROD));
        //            } else
        //                temp_expr = (OperExpr) (ArithExpr.op(temp_expr, divisor,
        //                        PROD));
        //            // }
        //            // changing_expr.makeCanonical();
        //        }
        //        return new OperExpr(temp_expr._type, temp_expr._terms);
        //    }

        public boolean equals(Object o) {
            if (o instanceof CompExpr) {
                CompExpr c = (CompExpr) o;
                return this._type == c._type && this._lhs.equals(c._lhs)
                        && this._rhs.equals(c._rhs);
            } else
                return false;
        }

        public int hashCode() {
            int i2 = _lhs.hashCode();
            int i3 = _rhs.hashCode();
            return (_type.hashCode()) + (i2 << 10) - (i3 << 20) + (i3 >>> 20)
                    - (i2 >>> 10);
        }

        public String toString() {
            return _lhs + " " + _type + " " + _rhs;
        }

        public CompExpr substitute(HashMap<String, ArithExpr> subst) {
            ArithExpr lhs = _lhs.substitute(subst);
            ArithExpr rhs = _rhs.substitute(subst);
            return new CompExpr(_type, lhs, rhs);
        }

        public Boolean evaluate(HashMap<String, Double> cont_assign) {

            Double dval_lhs = _lhs.evaluate(cont_assign);
            Double dval_rhs = _rhs.evaluate(cont_assign);

            if (dval_lhs == null || dval_rhs == null)
                return null;

            switch (_type) {
                case EQ:
                    return (dval_lhs == dval_rhs);
                case NEQ:
                    return (dval_lhs != dval_rhs);
                case GT:
                    return (dval_lhs > dval_rhs);
                case GT_EQ:
                    return (dval_lhs >= dval_rhs);
                case LT:
                    return (dval_lhs < dval_rhs);
                case LT_EQ:
                    return (dval_lhs <= dval_rhs);
                default:
                    return null;
            } 
        }

        public Boolean evaluateSlack(HashMap<String, Double> cont_assign, double eps) {

            Double dval_lhs = _lhs.evaluate(cont_assign);
            Double dval_rhs = _rhs.evaluate(cont_assign);

            if (dval_lhs == null || dval_rhs == null || Math.abs(dval_rhs - dval_lhs) <eps)
                return null;

            switch (_type) {
                case EQ:
                    return (dval_lhs == dval_rhs);
                case NEQ:
                    return (dval_lhs != dval_rhs);
                case GT:
                    return (dval_lhs > dval_rhs);
                case GT_EQ:
                    return (dval_lhs >= dval_rhs);
                case LT:
                    return (dval_lhs < dval_rhs);
                case LT_EQ:
                    return (dval_lhs <= dval_rhs);
                default:
                    return null;
            } 
        }

        public void collectVars(HashSet<String> vars) {
            _lhs.collectVars(vars);
            _rhs.collectVars(vars);
        }
    }

    //Arithmetic Expressions
    public abstract static class ArithExpr extends Expr {

        public abstract String toString(boolean format);

        public static ArithExpr ParseArithExpr(String s) {
            try {
                FOPC.Node res = FOPC.parse(s + " = 0");
                // if (res != null) System.out.println("==> " + res.toFOLString());
                return Convert2ArithExpr(((FOPC.PNode) res).getBinding(0));
            } catch (Exception e) {
                return null;
            }
        }

        public static ArithExpr Convert2ArithExpr(FOPC.Term t) {
            // System.out.println("Convert2ArithExpr: " + t.toFOLString());
            if (t instanceof FOPC.TVar) {
                return new VarExpr(((FOPC.TVar) t)._sName);
            } else if (t instanceof FOPC.TScalar) {
                return new DoubleExpr(((FOPC.TScalar) t)._dVal);
            } else if (t instanceof FOPC.TInteger) {
                return new DoubleExpr(((FOPC.TInteger) t)._nVal);
            } else if (t instanceof FOPC.TFunction) {
                return OperExpr.Convert2OperExpr((FOPC.TFunction) t);
            } else
                return null;
        }

        public static ArithExpr op(ArithExpr f1, ArithExpr f2, ArithOperation op) {
            if (f1 instanceof DoubleExpr && (op == ArithOperation.SUM || op == ArithOperation.PROD)) {
                // operands reordered
                return op(f2, ((DoubleExpr) f1)._dConstVal, op);
            } else if (f2 instanceof DoubleExpr) {
                // Can handle MINUS and DIV here
                return op(f1, ((DoubleExpr) f2)._dConstVal, op);
            } else if (f1 instanceof OperExpr && f2 instanceof OperExpr
                    && ((OperExpr) f1)._type == ((OperExpr) f2)._type
                    && ((OperExpr) f1)._type == op && (op == ArithOperation.SUM || op == ArithOperation.PROD)) {
                // Exploit associativity
                ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>(
                        ((OperExpr) f1)._terms);
                terms.addAll(((OperExpr) f2)._terms);
                return new OperExpr(op, terms);
            } else {
                ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>();
                terms.add(f1);
                terms.add(f2);
                return new OperExpr(op, terms);
            }

        }

        public static ArithExpr op(ArithExpr f1, double d, ArithOperation op) {
            if (f1 instanceof DoubleExpr) {
                switch (op) {
                    case SUM:
                        return new DoubleExpr(((DoubleExpr) f1)._dConstVal + d);
                    case PROD:
                        return new DoubleExpr(((DoubleExpr) f1)._dConstVal * d);
                    case MINUS:
                        return new DoubleExpr(((DoubleExpr) f1)._dConstVal - d);
                    case DIV:
                        return new DoubleExpr(((DoubleExpr) f1)._dConstVal / d);
                    default:
                        ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>();
                        terms.add(new DoubleExpr(d));
                        terms.add(f1);
                        return new OperExpr(op, terms);
                }
            } else if (f1 instanceof OperExpr && ((OperExpr) f1)._type == op
                    && (op == ArithOperation.SUM || op == ArithOperation.PROD)) {
                ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>(
                        ((OperExpr) f1)._terms);
                terms.add(new DoubleExpr(d));
                return new OperExpr(op, terms);
            } else {
                ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>();
                terms.add(f1);
                terms.add(new DoubleExpr(d));
                return new OperExpr(op, terms);
            }
        }

        public abstract ArithExpr substitute(HashMap<String, ArithExpr> subst);

        public abstract Double evaluate(HashMap<String, Double> cont_assign);

        public abstract Double evaluateRange(
                HashMap<String, Double> low_assign,
                HashMap<String, Double> high_assign, boolean use_low);

        public abstract void collectVars(HashSet<String> vars);

        public abstract ArithExpr round();

        // Assume expression is canonical, hence in sum of products form (could be a single term)
        public ArithExpr differentiateExpr(String diff_var) {
            diff_var = diff_var.intern();
            ArithExpr ret_expr = null;
            if (this instanceof OperExpr && ((OperExpr) this)._type == ArithOperation.SUM) {
                ret_expr = ((OperExpr) this).differentiateMultipleTerms(diff_var);
            } else if (this instanceof OperExpr && ((OperExpr) this)._type == ArithOperation.PROD) {
                ret_expr = ((OperExpr) this).differentiateTerm(diff_var);
            } else if ((this instanceof VarExpr) || (this instanceof DoubleExpr)) {
                OperExpr temp = new OperExpr(ArithOperation.PROD, Arrays.asList(this));
                ret_expr = temp.differentiateTerm(diff_var);
            } else {
                System.out.println("differentiateLeaf: Unsupported expression '" + this + "'");
                System.exit(1);
            }
            return ret_expr;
        }

        // Assume expression is canonical
        public ArithExpr integrateExpr(String integration_var) {
            integration_var = integration_var.intern();
            ArithExpr ret_expr = null;
            if (this instanceof OperExpr && ((OperExpr) this)._type == ArithOperation.SUM) {
                ret_expr = ((OperExpr) this).integrateMultipleTerms(integration_var);
            } else if (this instanceof OperExpr && ((OperExpr) this)._type == ArithOperation.PROD) {
                ret_expr = ((OperExpr) this).integrateTerm(integration_var);
            } else if ((this instanceof VarExpr) || (this instanceof DoubleExpr)) {
                OperExpr temp = new OperExpr(ArithOperation.PROD, Arrays.asList(this));
                ret_expr = temp.integrateTerm(integration_var);
            } else {
                System.out.println("processXADDLeaf: Unsupported expression '" + this + "'");
                System.exit(1);
            }
            return ret_expr;
        }

        // Takes ArithExpr expr1 linear in var, returns (coef,expr2) where expr1 = coef*x + expr2
        // Assume expression is canonical
        public CoefExprPair removeVarFromExpr(String remove_var) {
            remove_var = remove_var.intern();
            CoefExprPair ret = null;
            if (this instanceof OperExpr && ((OperExpr) this)._type == ArithOperation.SUM) {
                ret = ((OperExpr) this).removeVarFromExprMultipleTerms(remove_var);
            } else if (this instanceof OperExpr && ((OperExpr) this)._type == ArithOperation.PROD) {
                ret = ((OperExpr) this).removeVarFromExprTerm(remove_var);
            } else if ((this instanceof VarExpr) || (this instanceof DoubleExpr)) {
                OperExpr temp = new OperExpr(ArithOperation.PROD, Arrays.asList(this));
                ret = temp.removeVarFromExprTerm(remove_var);
            } else {
                System.out.println("removeVarFromExpr: Unsupported expression '" + this + "'");
                System.exit(1);
            }
            return ret;
        }

        // Assume expression is canonical, hence in sum of products form (could be a single term)
        public int determineHighestOrderOfVar(String var) {
            var = var.intern();
            if (this instanceof OperExpr && ((OperExpr) this)._type == ArithOperation.SUM) {
                return ((OperExpr) this)
                        .determineHighestOrderOfVarMultipleTerms(var);
            } else if (this instanceof OperExpr
                    && ((OperExpr) this)._type == ArithOperation.PROD) {
                return ((OperExpr) this).determineHighestOrderOfVarTerm(var);
            } else if ((this instanceof VarExpr)
                    || (this instanceof DoubleExpr)) {
                OperExpr temp = new OperExpr(ArithOperation.PROD, Arrays.asList(this));
                return temp.determineHighestOrderOfVarTerm(var);
            } else {
                System.out.println("removeVarFromExpr: Unsupported expression '" + this + "'");
                System.exit(1);
                return 0;
            }
        }
    }

    //Operation Arithmetic Expression
    public static class OperExpr extends ArithExpr {

        public ArithOperation _type = ArithOperation.UND;
        public ArrayList<ArithExpr> _terms = null;

        public OperExpr(ArithOperation type, ArithExpr t1, ArithExpr t2) {
            // this(type, Arrays.asList(new ArithExpr[] {t1, t2}));
            this(type, Arrays.asList(t1, t2));
        }

        public OperExpr(ArithOperation type, List<ArithExpr> terms) {
            _type = type;

            // Ensure subtraction and division are binary operators
            if ((_type == ArithOperation.MINUS || _type == ArithOperation.DIV) && terms.size() != 2) {
                _type = ArithOperation.ERROR;
                return;
            }

            _terms = new ArrayList<ArithExpr>(terms);
            if (_type == ArithOperation.SUM || _type == ArithOperation.PROD) {
//                try {
                Collections.sort(_terms);
//                } catch (IllegalArgumentException e) { //todo try/catch added by Hadi
//                    System.err.println("java.lang.IllegalArgumentException: Comparison method violates its general contract!");
                //todo Figure out the source of problem...
//                    for (ArithExpr a : _terms) {
//                        for (ArithExpr b : _terms) {
//                            if ((a.hashCode() == b.hashCode()) && (!a.equals(b) || a != b)) {
//                                System.out.println("1. a = " + a);
//                                System.out.println("1. b = " + b);
//                            }
//
//                            if ((a.equals(b) || b.equals(a) || a == b) && (a.hashCode() != b.hashCode())) {
//                                System.out.println("2. a = " + a);
//                                System.out.println("2. b = " + b);
//
//                            }
//                            for (ArithExpr c : _terms) {
//                                if (a.compareTo(b) < 0 && b.compareTo(c) < 0 && (a.compareTo(c) >= 0)) {
//                                    System.out.println("a = " + a);
//                                    System.out.println("b = " + b);
//                                    System.out.println("c = " + c);
//                                    System.out.println("a.hashCode() = " + a.hashCode());
//                                    System.out.println("b.hashCode() = " + b.hashCode());
//                                    System.out.println("c.hashCode() = " + c.hashCode());
//
//                                    System.out.println("a.compareTo(b) = " + a.compareTo(b));/
//                                    System.out.println("b.compareTo(c) = " + b.compareTo(c));
//                                    System.out.println("a.compareTo(c) = " + a.compareTo(c));
//                                }
//                            }
//                        }
//                    }
//                }
            }
        }

        public boolean equals(Object o) {
            if (o instanceof OperExpr) {
                OperExpr e = (OperExpr) o;
                return this._type == e._type && this._terms.equals(e._terms);
            } else
                return false;
        }

        // COMMENTED BY HADI
//    public int hashCode() {
//        return _terms.toString().hashCode() - _type.hashCode();
//    }
// ADDED BY HADI
        @Override
        public int hashCode() {
            return 31 * _type.hashCode() + _terms.hashCode();
        }

        public static ArithExpr Convert2OperExpr(FOPC.TFunction t) {
            // System.out.println("Convert2OperExpr: [" + t._nArity + "] "
            // + t.toFOLString());
            if (t._nArity == 0)
                return new VarExpr(t._sFunName);

            // The following is a bit ugly but easy to write & debug and
            // only called once when files are read (so inefficiency is OK)
            ArithExpr term1 = ArithExpr.Convert2ArithExpr(t.getBinding(0));
            ArithExpr term2 = ArithExpr.Convert2ArithExpr(t.getBinding(1));
            ArrayList<ArithExpr> terms = new ArrayList<ArithExpr>();
            ArithOperation type = ArithOperation.UND;
            if (t._sFunName.equals("f_add")) {
                type = ArithOperation.SUM;
            } else if (t._sFunName.equals("f_sub")) {
                type = ArithOperation.MINUS;
                if (term1 instanceof DoubleExpr && term2 instanceof DoubleExpr
                        && ((DoubleExpr) term1)._dConstVal == 0d)
                    return new DoubleExpr(-((DoubleExpr) term2)._dConstVal);
            } else if (t._sFunName.equals("f_mul")) {
                type = ArithOperation.PROD;
            } else if (t._sFunName.equals("f_div")) {
                type = ArithOperation.DIV;
            }
            if (type == ArithOperation.UND)
                return null;
            if ((type == ArithOperation.SUM || type == ArithOperation.PROD) && (term1 instanceof OperExpr)
                    && ((OperExpr) term1)._type == type) {
                terms.addAll(((OperExpr) term1)._terms);
                term1 = null;
            }
            if ((type == ArithOperation.SUM || type == ArithOperation.PROD) && (term2 instanceof OperExpr)
                    && ((OperExpr) term2)._type == type) {
                terms.addAll(((OperExpr) term2)._terms);
                term2 = null;
            }
            if (term1 != null)
                terms.add(term1);
            if (term2 != null)
                terms.add(term2);
            return new OperExpr(type, terms);
        }

        public String toString(boolean format) {
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < _terms.size(); i++) {
                if (i != 0)
                    sb.append(" " + _type + " ");
                sb.append(_terms.get(i).toString(format));
            }
            sb.append(")");
            return sb.toString();
        }

        public void collectVars(HashSet<String> vars) {
            for (ArithExpr e : _terms)
                e.collectVars(vars);
        }

        public ArithExpr substitute(HashMap<String, ArithExpr> subst) {
            ArrayList<ArithExpr> terms2 = new ArrayList<ArithExpr>();
            for (ArithExpr expr : _terms)
                terms2.add(expr.substitute(subst));
            OperExpr expr = new OperExpr(_type, terms2);
            if (expr._terms.size() == 1)
                return expr._terms.get(0);
            else
                return expr;
        }

        public ArithExpr round() {
            ArrayList<ArithExpr> terms2 = new ArrayList<ArithExpr>();
            for (ArithExpr expr : _terms)
                terms2.add(expr.round());
            OperExpr expr = new OperExpr(_type, terms2);
            if (expr._terms.size() == 1)
                return expr._terms.get(0);
            else
                return expr;
        }

        public Double evaluate(HashMap<String, Double> cont_assign) {
            Double accum = _terms.get(0).evaluate(cont_assign);
            for (int i = 1; i < _terms.size() && accum != null; i++) {
                Double term_eval = _terms.get(i).evaluate(cont_assign);
                if (term_eval == null)
                    accum = null;
                else
                    switch (_type) {
                        case SUM:
                            accum = accum + term_eval;
                            break;
                        case MINUS:
                            accum = accum - term_eval;
                            break;
                        case PROD:
                            accum = accum * term_eval;
                            break;
                        case DIV:
                            accum = accum / term_eval;
                            break;
                        default:
                            accum = null;
                    }
            }
            return accum;
        }

        @Override
        public Double evaluateRange(HashMap<String, Double> low_assign,
                                    HashMap<String, Double> high_assign, boolean use_low) {

            // Must use canonical nodes here... assumes products are binary
            // with the first term being a coefficient
            if (!XADD.USE_CANONICAL_NODES) {
                System.err.println("Must use canonical nodes if using evaluateRange");
                System.exit(1);
            }

            if (XADD.DEBUG_EVAL_RANGE)
                System.out.println("Evaluating " + (use_low ? "min" : "max")
                        + " range: " + this);

            Double accum = _terms.get(0).evaluateRange(low_assign, high_assign,
                    use_low);
            if (XADD.DEBUG_EVAL_RANGE)
                System.out.println("- Term eval [" + 0 + "] = " + _terms.get(0)
                        + " = " + accum + " -- " + use_low);

            boolean subterm_use_low = (_type == ArithOperation.MINUS || _type == ArithOperation.DIV || (_type == ArithOperation.PROD && accum < 0d)) ? !use_low
                    : use_low;

            for (int i = 1; i < _terms.size() && accum != null; i++) {

                Double term_eval = _terms.get(i).evaluateRange(low_assign,
                        high_assign, subterm_use_low);
                if (XADD.DEBUG_EVAL_RANGE)
                    System.out.println("- Term eval [" + i + "] = "
                            + _terms.get(i) + " = " + term_eval + " -- "
                            + subterm_use_low);
                if (term_eval == null)
                    accum = null;
                else
                    switch (_type) {
                        case SUM:
                            accum += term_eval;
                            break;
                        case MINUS:
                            accum -= term_eval;
                            break;
                        case PROD:
                            accum *= term_eval;
                            break;
                        case DIV:
                            accum /= term_eval;
                            break;
                        default:
                            accum = null;
                    }
                if (XADD.DEBUG_EVAL_RANGE)
                    System.out.println("- accum: " + accum);
            }
            if (XADD.DEBUG_EVAL_RANGE)
                System.out.println("* Result " + (use_low ? "min" : "max")
                        + " range: " + accum);
            return accum;
        }

        public boolean checkCanonical() {

            // This node is canonical if it is term canonical
            // or it is a sum of terms
            if (checkTermCanonical())
                return true;

            if (_type == ArithOperation.SUM) {

                //Error doesn't check for more than one term with the same variables!
                HashSet<ArithExpr> seenVars = new HashSet<ArithExpr>();

                // Ensure all subterms are canonical
                for (int i = 0; i < _terms.size(); i++) {

                    // First term can be a constant so long as more than one
                    // term
                    if (i == 0 && (_terms.get(0) instanceof DoubleExpr)) {
                        ((DoubleExpr) _terms.get(0)).round();
                        if (Math.abs(((DoubleExpr) _terms.get(0))._dConstVal) <= XADD.PRECISION)
                            return false;
                        else
                            continue;
                    }

                    if (!(_terms.get(i) instanceof OperExpr)
                            || !((OperExpr) _terms.get(i)).checkTermCanonical()) {
                        // System.out.println("-- not canonical because [" + i +
                        // "]" + _terms.get(i) + " : " +
                        // _terms.get(i).getClass());
                        return false;
                    }
                    
                    //Detect var
                    OperExpr term = (OperExpr) _terms.get(i);
                    ArrayList<ArithExpr> termlist = new ArrayList<ArithExpr>(term._terms); //make a copy to avoid changing decisions
                    if (!(termlist.get(0) instanceof DoubleExpr))
                        return false;
                    termlist.remove(0); //remove Double constant to obtain var name
                    OperExpr Var = new OperExpr(term._type, termlist);
                    if (!seenVars.add(Var)) return false; //repeated var
                }

                return true;
            } else {
                // System.out.println("-- not canonical because not SUM: " +
                // _aOpNames[_type]);
                return false;
            }
        }

        public boolean checkTermCanonical() {
            // This is term canonical if it is a product of a constant followed
            // by variables or a product with a delta function
            if (_type == ArithOperation.PROD) {

                if (_terms.get(_terms.size() - 1) instanceof DeltaFunExpr) // Delta comes last if it is present
                    return true;

                if (!(_terms.get(0) instanceof DoubleExpr))
                    return false;

                ((DoubleExpr) _terms.get(0)).round();
                if (Math.abs(((DoubleExpr) _terms.get(0))._dConstVal) <= XADD.PRECISION)
                    return false;

                for (int i = 1; i < _terms.size(); i++) {
                    if (!(_terms.get(i) instanceof VarExpr))
                        return false;
                }

                return true;
            } else
                return false;
        }

        // Canonicity for arithmetic expressions:
        //
        // 1. Expressions all zero on RHS of comparisons and restrict symbols:
        // a > b : a <= b and swap branches
        // a < b : a >= b and swap branches
        // a != b : a == b and swap branches
        // 2. Multiple layers of + / * collapsed: (X + Y) + Z -> X + Y + Z
        // 3. Distribute * over +: X * (A + B) -> X * A + X * B
        // 4. All subtraction: X - Y -> X + -Y
        // ??? 5. Division -> multiplication
        // ??? 6. Multiple multiplied divisions -> numerator and denominator
        // 7. Sorting of OperExpr terms with TreeSet?
        // 8. Make all products start with a single Double coefficient
        // 9. Compress / remove common polynomial terms
        // 10. Prevent singleton operator expressions
        public static int ALREADY_CANONICAL = 0;
        public static int NON_CANONICAL = 0;

        public Expr makeCanonical() {

            // A simple non-canonical case is OperExpr - 0, so catch this
            if (_type == ArithOperation.MINUS && _terms.get(1) instanceof DoubleExpr
                    && Math.abs(((DoubleExpr) _terms.get(1))._dConstVal) <= XADD.PRECISION) {
                return _terms.get(0).makeCanonical();
            }

            // If already canonical, no need to modify
            if (checkCanonical()) {
                // System.out.println("** Already canonical: " + this);
                ALREADY_CANONICAL++;
                return this;
            } else {
                // System.out.println("** Not canonical: " + this);
                NON_CANONICAL++;
            }

            ArithOperation new_type = _type;
            ArrayList<ArithExpr> new_terms = new ArrayList<ArithExpr>(_terms);

            // 4. All subtraction: X - Y -> X + -Y
            if (new_type == ArithOperation.MINUS) {
                ArithExpr term2 = new_terms.get(1);
                term2 = ArithExpr.op(term2, NEG_ONE, ArithOperation.PROD);
                new_terms.set(1, term2);
                new_type = ArithOperation.SUM;
            }

            // Recursively ensure all subterms in canonical form, and then
            // 2. Multiple layers of + / * collapsed: (X + Y) + Z -> X + Y + Z
            ArrayList<ArithExpr> reduced_terms = new ArrayList<ArithExpr>();
            for (ArithExpr e : new_terms) {
                e = (ArithExpr) e.makeCanonical();
                // If same type, add all subterms directly to reduced terms
                if ((e instanceof OperExpr) && ((OperExpr) e)._type == new_type
                        && (new_type == ArithOperation.SUM || new_type == ArithOperation.PROD))
                    reduced_terms.addAll(((OperExpr) e)._terms);
                else
                    reduced_terms.add(e);
            }
            new_terms = reduced_terms;
            // System.out.println(">> Flattened terms: " + reduced_terms);

            // 3. Distribute * over +: X * (A + B) -> X * A + X * B
            // X * (1/Y) * (W + Z) * (U + V)
            // Maintain sum list...
            // if division, multiply in 1/x
            boolean contains_delta = new_terms.get(new_terms.size() - 1) instanceof DeltaFunExpr; // Delta is always last
            //if (contains_delta) System.err.println(new_type + ": " + new_terms);

            // Don't simplify a product if it contains a delta
            if (new_type == ArithOperation.PROD && !contains_delta) {

                ArrayList<ArithExpr> sum_terms = new ArrayList<ArithExpr>();
                ArithExpr first_term = new_terms.get(0);
                if ((first_term instanceof OperExpr)
                        && ((OperExpr) first_term)._type == ArithOperation.SUM)
                    sum_terms.addAll(((OperExpr) first_term)._terms);
                else
                    sum_terms.add(first_term);

                for (int i = 1; i < new_terms.size(); i++) {
                    ArithExpr e = new_terms.get(i);
                    if ((e instanceof OperExpr) && ((OperExpr) e)._type == ArithOperation.SUM) {
                        // e2 : {A + B} * e3 : {C + D}
                        // System.out.println(">>>> Mult 1 " + e + " * " +
                        // sum_terms);
                        ArrayList<ArithExpr> new_sum_terms = new ArrayList<ArithExpr>();
                        for (ArithExpr e2 : sum_terms) {
                            for (ArithExpr e3 : ((OperExpr) e)._terms) {
                                // System.out.println(">>>> Multiplying " + e2 +
                                // " * " + e3);
                                new_sum_terms.add(ArithExpr.op(e2, e3, ArithOperation.PROD));
                            }
                        }
                        // System.out.println(">>>> Mult 1 Out " +
                        // new_sum_terms);
                        sum_terms = new_sum_terms;
                    } else {
                        // e2 : {A + B} * e
                        // System.out.println(">>>> Mult 2 " + e + " * " +
                        // sum_terms);
                        for (int j = 0; j < sum_terms.size(); j++) {
                            ArithExpr e2 = sum_terms.get(j);
                            sum_terms.set(j, new OperExpr(ArithOperation.PROD, e, e2));
                        }
                    }
                }

                // If sum_terms are singular no need to modify, otherwise
                if (sum_terms.size() > 1) {

                    new_type = ArithOperation.SUM;

                    // Again make canonical and collapse terms where possible
                    new_terms.clear();
                    for (ArithExpr e : sum_terms) {
                        e = (ArithExpr) e.makeCanonical();
                        // If same type, add all subterms directly to reduced
                        // terms
                        if ((e instanceof OperExpr)
                                && ((OperExpr) e)._type == ArithOperation.SUM)
                            new_terms.addAll(((OperExpr) e)._terms);
                        else
                            new_terms.add(e);
                    }
                }
            }

            // 9. Merge (and remove) all polynomial/function terms in a sum
            //    Product terms of delta may not be canonical because they were ignored for canonicalization earlier.
            if (new_type == ArithOperation.SUM) {
                ArrayList<ArithExpr> non_terms = new ArrayList<ArithExpr>();
                double const_sum = 0d;

                // Hash all terms to a coefficient
                HashMap<ArrayList<ArithExpr>, Double> term2coef = new HashMap<ArrayList<ArithExpr>, Double>();
                for (ArithExpr e : new_terms) {
                    if ((e instanceof OperExpr && ((OperExpr) e)._type == ArithOperation.PROD)
                            || (e instanceof VarExpr) || (e instanceof FunExpr)) {

                        // Determine the terms and coefficient
                        ArrayList<ArithExpr> index = new ArrayList<ArithExpr>();
                        DoubleExpr d = null;
                        if (e instanceof VarExpr || e instanceof FunExpr) {
                            index.add(e);
                            d = new DoubleExpr(1d);
                        } else {
                            OperExpr o = (OperExpr) e;
                            Expr first_term = o._terms.get(0);
                            if (first_term instanceof DoubleExpr)
                                d = (DoubleExpr) first_term;
                            else { // This should only occur when a product term contains a DeltaExpr and it was not simplified
                                if (!(o._terms.get(o._terms.size() - 1) instanceof DeltaFunExpr)) {
                                    System.err.println("Possible non-canonical term: " + o);
                                    new Exception().printStackTrace(System.err);
                                    System.exit(1);
                                }
                                index.add((ArithExpr) first_term); // It's not a constant
                                d = new DoubleExpr(1d);
                            }
                            for (int j = 1; j < o._terms.size(); j++)
                                index.add(o._terms.get(j));
                        }

                        // Hash to the correct coefficient
                        Double dval = null;
                        if ((dval = term2coef.get(index)) != null)
                            dval += d._dConstVal;
                        else
                            dval = d._dConstVal;
                        term2coef.put(index, dval);

                    } else if (e instanceof DoubleExpr) {
                        const_sum += ((DoubleExpr) e)._dConstVal;
                    } else
                        non_terms.add(e);
                }

                // Convert back to an ArrayList
                new_terms = non_terms;
                if ( Math.abs(const_sum) > XADD.PRECISION) // conts_sum != 0d <- Error prone double comparison
                    new_terms.add(0, new DoubleExpr(const_sum));

                for (Map.Entry<ArrayList<ArithExpr>, Double> t : term2coef
                        .entrySet()) {
                    double val = t.getValue();
                    if (val == 0d)
                        continue;

                    ArrayList<ArithExpr> term = t.getKey();
                    DoubleExpr dcoef = new DoubleExpr(val);
                    term.add(0, dcoef);
                    new_terms.add(new OperExpr(ArithOperation.PROD, term));
                }

                // An empty sum is zero
                if (new_terms.size() == 0)
                    return new DoubleExpr(0d);
            }

            // 8. Make all products start with a single Double coefficient
            if (new_type == ArithOperation.PROD) {
                double coef = 1d;
                ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
                for (ArithExpr e : new_terms) {
                    if (e instanceof DoubleExpr)
                        coef *= ((DoubleExpr) e)._dConstVal;
                    else
                        factors.add(e);
                }
                if (Math.abs(coef) > XADD.PRECISION) {
                    factors.add(0, new DoubleExpr(coef));
                    new_terms = factors; // Will be sorted on new OperExpr
                } else {
                    return new DoubleExpr(0d);
                }
            }
            // Handle division of two constants
            if (new_type == ArithOperation.DIV && new_terms.get(0) instanceof DoubleExpr
                    && new_terms.get(1) instanceof DoubleExpr) {
                return new DoubleExpr(
                        ((DoubleExpr) new_terms.get(0))._dConstVal
                                / ((DoubleExpr) new_terms.get(1))._dConstVal);
            }

            // 10. Prevent singleton operator expressions
            if (new_terms.size() == 1) {
                return new_terms.get(0);
            }

            // Ensure canonical order
            OperExpr canonical_expr = new OperExpr(new_type, new_terms);
            // System.out.println("- now canonical: " + canonical_expr);
            return canonical_expr;
        }

        public ArithExpr normalize() {
            double normConst = 0;
            ArithExpr normal = ONE;
            if (_terms.get(0).equals(ONE) || _terms.get(0).equals(NEG_ONE)) {
                //System.out.println("alreadyNormal "+ this);
                return this;
            }
            ArithOperation newType = _type;
            ArrayList<ArithExpr> newTerms = new ArrayList<ArithExpr>();
            if (_type != ArithOperation.SUM && _type != ArithOperation.PROD) {
                System.out.println("Uncanonical normalize!!, not SUM or PROD");
                //System.exit(1);
                return this;
            }
            ArithExpr t1 = _terms.get(0);
            if (t1 instanceof DoubleExpr) {
                normConst = ((DoubleExpr) t1)._dConstVal;
                if (normConst < 0) {
                    normal = NEG_ONE;
                    normConst = -normConst;
                }
            } else {
                ArithExpr t2 = ((OperExpr) t1)._terms.get(0);
                if (t2 instanceof DoubleExpr) {
                    normConst = ((DoubleExpr) t2)._dConstVal;
                    if (normConst < 0) {
                        normConst = -normConst;
                        normal = NEG_ONE;
                    }
                    ArrayList<ArithExpr> nterms = (ArrayList<ArithExpr>) ((OperExpr) t1)._terms.clone();
                    nterms.set(0, normal);
                    normal = new OperExpr(((OperExpr) t1)._type, nterms);
                } else System.out.println("not even t2 is Double: suspicious:" + normConst);
            }

            if (Math.abs(normConst - 1) <= XADD.PRECISION) {
                return this;
            }
            newTerms.add(normal);
            if (newType == ArithOperation.SUM) {
                for (int i = 1; i < _terms.size(); i++) {
                    if (_terms.get(i) instanceof DoubleExpr) {
                        System.err.println("two numbers on sum?");
                    }
                    if (_terms.get(i) instanceof VarExpr) {
                        System.err.println("lost var in normalize: skipping" + this);
                    }
                    if (_terms.get(i) instanceof OperExpr) {
                        OperExpr op1 = ((OperExpr) _terms.get(i)); //op1 must be PROD
                        ArrayList<ArithExpr> otherTerms = new ArrayList<ArithExpr>();
                        otherTerms.add(new DoubleExpr(((DoubleExpr) op1._terms.get(0))._dConstVal / normConst));
                        for (int j = 1; j < op1._terms.size(); j++) otherTerms.add(op1._terms.get(j));
                        newTerms.add(new OperExpr(ArithOperation.PROD, otherTerms));
                    }
                }
            } else {
                for (int j = 1; j < _terms.size(); j++) newTerms.add(_terms.get(j));
            }
            return new OperExpr(newType, newTerms);
        }

        @Override
        public String toString() {
            return toString(true);
        }

        // Must be a SUM of terms to get here
        public OperExpr differentiateMultipleTerms(String diff_var) {
            if (this._type != ArithOperation.SUM) {
                System.out.println("differentiateMultipleTerms: Expected SUM, got '" + this + "'");
                System.exit(1);
            }
            ArrayList<ArithExpr> differentiated_terms = new ArrayList<ArithExpr>();
            for (ArithExpr e : this._terms) {
                if (e instanceof OperExpr) {
                    differentiated_terms.add(((OperExpr) e).differentiateTerm(diff_var));
                } else if ((e instanceof VarExpr) || (e instanceof DoubleExpr)) {
                    OperExpr temp = new OperExpr(ArithOperation.PROD, Arrays.asList(e));
                    differentiated_terms.add(temp.differentiateTerm(diff_var));
                } else {
                    System.out.println("differentiateMultipleTerms: Unsupported expression '" + e + "'");
                    System.exit(1);
                }
            }
            return new OperExpr(ArithOperation.SUM, differentiated_terms);
        }

        // A single term (PROD)
        public ArithExpr differentiateTerm(String diff_var) {
            if (this._type != ArithOperation.PROD) {
                System.out.println("differentiateTerm: Expected PROD, got '" + this + "'");
                System.exit(1);
            }

            // Process all terms (optional double followed by vars)
            int derivative_var_count = 0;
            int last_var_added_at = -1;
            DoubleExpr d = new DoubleExpr(1d);
            ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
            for (ArithExpr e : this._terms) {
                if (e instanceof DoubleExpr) {
                    DoubleExpr d2 = (DoubleExpr) e;
                    d = new DoubleExpr(d._dConstVal * d2._dConstVal);
                } else if (e instanceof VarExpr) {
                    factors.add(e);
                    // Both interned so we can test direct equality
                    if (((VarExpr) e)._sVarName == diff_var) {
                        derivative_var_count++;
                        last_var_added_at = factors.size() - 1;
                    }
                } else {
                    System.out.println("differentiateTerm: Unsupported expression '" + e + "'");
                    System.exit(1);
                }
            }

            // Perform differentiation
            if (derivative_var_count == 0) {
                return ZERO; // Derivative of a constant is 0
            } else {
                // x*x*...*x = x^n
                // d/dx x^n = n*x^{n-1}
                factors.remove(last_var_added_at);
                d = new DoubleExpr(((double) derivative_var_count) * d._dConstVal);
                factors.add(0, d);

                return new OperExpr(ArithOperation.PROD, factors);
            }
        }

        // Must be a SUM of terms to get here
        public OperExpr integrateMultipleTerms(String integration_var) {
            if (this._type != ArithOperation.SUM) {
                System.out.println("integrateMultipleTerms: Expected SUM, got '" + this + "'");
                System.exit(1);
            }
            ArrayList<ArithExpr> integrated_terms = new ArrayList<ArithExpr>();
            for (ArithExpr e : this._terms) {
                if (e instanceof OperExpr) {
                    integrated_terms.add(((OperExpr) e).integrateTerm(integration_var));
                } else if ((e instanceof VarExpr) || (e instanceof DoubleExpr)) {
                    OperExpr temp = new OperExpr(ArithOperation.PROD, Arrays.asList(e));
                    integrated_terms.add(temp.integrateTerm(integration_var));
                } else {
                    System.out.println("integrateMultipleTerms: Unsupported expression '" + e + "'");
                    System.exit(1);
                }
            }
            return new OperExpr(ArithOperation.SUM, integrated_terms);
        }

        // A single term (PROD)
        public ArithExpr integrateTerm(String integration_var) {
            if (this._type != ArithOperation.PROD) {
                System.out.println("integrateTerm: Expected PROD, got '" + this + "'");
                System.exit(1);
            }

            // Process all terms (optional double followed by vars)
            int integration_var_count = 0;
            DoubleExpr d = new DoubleExpr(1d);
            ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
            for (ArithExpr e : this._terms) {
                // while (e instanceof OperExpr) {
                // OperExpr oe = (OperExpr) e;
                // e = e.op(oe._terms.get(0), oe._terms.get(1), oe._type);
                // }
                if (e instanceof DoubleExpr) {
                    DoubleExpr d2 = (DoubleExpr) e;
                    d = new DoubleExpr(d._dConstVal * d2._dConstVal);
                } else if (e instanceof VarExpr) {
                    factors.add(e);
                    // Both interned so we can test direct equality
                    if (((VarExpr) e)._sVarName == integration_var)
                        integration_var_count++;
                } else {
                    System.out.println("integrateTerm: Unsupported expression '" + e + "'");
                    System.exit(1);
                }
            }

            // Perform integration
            factors.add(new VarExpr(integration_var));
            d = new DoubleExpr(d._dConstVal / (double) (integration_var_count + 1));
            factors.add(0, d);

            return new OperExpr(ArithOperation.PROD, factors);
        }

        // Must be a SUM of terms to get here
        public CoefExprPair removeVarFromExprMultipleTerms(String remove_var) {
            if (this._type != ArithOperation.SUM) {
                System.out.println("removeVarFromExprMultipleTerms: Expected SUM, got '" + this + "'");
                System.exit(1);
            }
            double var_coef = 0d;
            ArrayList<ArithExpr> remaining_terms = new ArrayList<ArithExpr>();
            for (ArithExpr e : this._terms) {
                if (e instanceof OperExpr) {
                    CoefExprPair p = ((OperExpr) e).removeVarFromExprTerm(remove_var);
                    var_coef += p._coef;
                    remaining_terms.add(p._expr);
                } else if (e instanceof VarExpr) {
                    OperExpr temp = new OperExpr(ArithOperation.PROD, Arrays.asList(e));
                    CoefExprPair p = temp.removeVarFromExprTerm(remove_var);
                    var_coef += p._coef;
                    remaining_terms.add(p._expr);
                } else if (e instanceof DoubleExpr) {
                    remaining_terms.add(e);
                } else {
                    System.out.println("removeVarFromExprMultipleTerms: Unsupported expression '" + e + "'");
                    System.exit(1);
                }
            }
            return new CoefExprPair(new OperExpr(ArithOperation.SUM, remaining_terms), var_coef);
        }

        // A single term (PROD)
        public CoefExprPair removeVarFromExprTerm(String remove_var) {
            double var_coef = 0d;
            if (this._type != ArithOperation.PROD) {
                System.out.println("removeVarFromExprTerm: Expected PROD, got '" + this + "'");
                System.exit(1);
            }

            // Process all terms (optional double followed by vars)
            int remove_var_count = 0;
            double coef = 1d;
            ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
            for (ArithExpr e : this._terms) {
                if (e instanceof DoubleExpr) {
                    coef *= ((DoubleExpr) e)._dConstVal;
                } else if (e instanceof VarExpr) {
                    // Both interned so we can test direct equality
                    if (((VarExpr) e)._sVarName == remove_var)
                        remove_var_count++;
                    else
                        factors.add(e);
                } else {
                    System.out.println("removeVarFromExprTerm: Unsupported expression '" + e + "'");
                    System.exit(1);
                }
            }

            // Check for proper form
            if (remove_var_count > 0) {
                if (remove_var_count > 1 || factors.size() > 0) {
                    System.out.println("removeVarFromExprTerm: var '" + remove_var
                            + "' must appear linearly in expression '" + this + "'");
                    System.exit(1);
                }
                // If get here only coef*_integrationVar
                var_coef += coef;
                return new CoefExprPair(ZERO, var_coef); // Just add a zero term in this place
            } else {
                factors.add(0, new DoubleExpr(coef));
                return new CoefExprPair(new OperExpr(ArithOperation.PROD, factors), var_coef);
            }
        }

        // Must be a SUM of terms to get here
        public int determineHighestOrderOfVarMultipleTerms(String var) {
            if (this._type != ArithOperation.SUM) {
                System.out.println("determineHighestOrderOfVarMultipleTerms: Expected SUM, got '" + this + "'");
                System.exit(1);
            }
            int max_order = 0;
            for (ArithExpr e : this._terms) {
                if (e instanceof OperExpr) {
                    max_order = Math.max(max_order, ((OperExpr) e).determineHighestOrderOfVarTerm(var));
                } else if (e instanceof VarExpr) {
                    OperExpr temp = new OperExpr(ArithOperation.PROD, Arrays.asList(e));
                    max_order = Math.max(max_order, temp.determineHighestOrderOfVarTerm(var));
                } else if (!(e instanceof DoubleExpr)) {
                    System.out.println("determineHighestOrderOfVarMultipleTerms: Unsupported expression '" + e + "'");
                    System.exit(1);
                }
            }
            return max_order;
        }

        // A single term (PROD)
        public int determineHighestOrderOfVarTerm(String var) {
            if (this._type != ArithOperation.PROD) {
                System.out.println("determineHighestOrderOfVarTerm: Expected PROD, got '" + this + "'");
                System.exit(1);
            }

            // Process all terms (optional double followed by vars)
            int integration_var_count = 0;
            //Calculate the coefficient of the var - Not used
            //double coef = 1d;
            ArrayList<ArithExpr> factors = new ArrayList<ArithExpr>();
            for (ArithExpr e : this._terms) {
                if (e instanceof DoubleExpr) {
                    //Do Nothing, not finding the best coef
                    //coef *= ((DoubleExpr) e)._dConstVal;
                } else if (e instanceof VarExpr) {
                    // Both interned so we can test direct equality
                    if (((VarExpr) e)._sVarName == var)
                        integration_var_count++;
                    else
                        factors.add(e);
                } else {
                    System.out.println("determineHighestOrderOfVarTerm: Unsupported expression '" + e + "'");
                    System.exit(1);
                }
            }
            return integration_var_count;
        }
    }

    //Numerical Value Expression
    public static class DoubleExpr extends ArithExpr {
        public double _dConstVal = Double.NaN;

        public DoubleExpr(double val) {
            _dConstVal = val;
        }

        public String toString() {
            if (Double.isInfinite(_dConstVal) || Double.isNaN(_dConstVal))
                return Double.toString(_dConstVal);
            return _df.format(_dConstVal);
        }

        @Override
        public String toString(boolean format) {
            if (Double.isInfinite(_dConstVal) || Double.isNaN(_dConstVal))
                return Double.toString(_dConstVal);
            return _df_unformatted.format(_dConstVal);
        }


        //TODO This equality definition violates the rule that equal objects should have same hashCode. It is also "non-transitive"!
        // If XADD.PRECISION is non-zero, (there are cases where) this produces the bug:
        // "java.lang.IllegalArgumentException: Comparison method violates its general contract!" in class OperExpr where terms are sorted.
        // The solution seems to be: not using PRECISION in this method (using another method e.g. areSimilarEnough() for that purpose)
        public boolean equals(Object o) {
            if (o instanceof DoubleExpr) {
                DoubleExpr d = (DoubleExpr) o;
                if (((Double) this._dConstVal).equals(d._dConstVal)) return true;
                Double dif = this._dConstVal - d._dConstVal;
                if ((Double.isInfinite(dif) || Double.isNaN(dif))) return false;
                return Math.abs(dif) < XADD.PRECISION;
            } else
                return false;
        }

        public int hashCode() {
            return ((Double) _dConstVal).hashCode();
        }

        public ArithExpr substitute(HashMap<String, ArithExpr> subst) {
            return this;
        }

        public Double evaluate(HashMap<String, Double> cont_assign) {
            return _dConstVal;
        }

        @Override
        public Double evaluateRange(HashMap<String, Double> low_assign,
                                    HashMap<String, Double> high_assign, boolean use_low) {
            return _dConstVal;
        }

        public void collectVars(HashSet<String> vars) {
        }

        public Expr makeCanonical() {
            return this;
        }

        public ArithExpr round() {
            if (Double.isInfinite(_dConstVal) || Double.isNaN(_dConstVal) || this == ONE || this == ZERO || this == NEG_ONE)
                return this;
            //todo commented by hadi/scott
//            return new DoubleExpr((Math.round(_dConstVal * XADD.ROUND_PRECISION) * 1d) / XADD.ROUND_PRECISION);
            //todo added by hadi/scott:
            if (XADD.ROUND_PRECISION == null) return this;
            else {
                //todo: Warning by Hadi
//                System.err.println("Note that this kind of rounding produces lots of approximation errors...");
                return new DoubleExpr((Math.round(_dConstVal * XADD.ROUND_PRECISION) * 1d) / XADD.ROUND_PRECISION);
            }
        }

    }

    //Single Variable String Expression
    public static class VarExpr extends ArithExpr {
        public String _sVarName = null;
        public int _nHashCode = -1;

        public VarExpr(String name) {
            _sVarName = name.intern();
            _nHashCode = _sVarName.hashCode();
        }

        public String toString() {
            return _sVarName;
        }

        public int hashCode() {
            return _nHashCode;
        }

        public boolean equals(Object o) {
            if (o instanceof VarExpr) {
                VarExpr v = (VarExpr) o;
                return this._sVarName == v._sVarName;
            } else
                return false;
        }

        public ArithExpr substitute(HashMap<String, ArithExpr> subst) {
            ArithExpr v = subst.get(_sVarName);
            return v != null ? v : this;
        }

        public Double evaluate(HashMap<String, Double> cont_assign) {
            return cont_assign.get(_sVarName);
        }

        @Override
        public Double evaluateRange(HashMap<String, Double> low_assign,
                                    HashMap<String, Double> high_assign, boolean use_low) {
            return use_low ? low_assign.get(_sVarName) : high_assign
                    .get(_sVarName);
        }

        public void collectVars(HashSet<String> vars) {
            vars.add(_sVarName);
        }

        public Expr makeCanonical() {
            return new OperExpr(ArithOperation.PROD, new DoubleExpr(1d), this);
        }

        public ArithExpr normalize() {
            System.err.println("Shouldn't normalize single VarExpr");
            System.exit(1);
            return new OperExpr(ArithOperation.PROD, new DoubleExpr(1d), this);
        }

        @Override
        public String toString(boolean format) {
            return toString();
        }

        @Override
        public ArithExpr round() {
            return this;
        }
    }

    public static class CoefExprPair {
        public ArithExpr _expr;
        public double _coef;

        public CoefExprPair(ArithExpr expr, double coef) {
            _expr = expr;
            _coef = coef;
        }
    }

    //////////////////////////////////////////////////////////////////////////
    //                       Special-Purpose Functions
    //////////////////////////////////////////////////////////////////////////

    // Defined functions... we want these to be embeddable in OperExpr so
    // we need to make them ArithExpr's
    public abstract static class FunExpr extends ArithExpr {

        public FunExpr(ArrayList<ArithExpr> l) {
            _args = new ArrayList<ArithExpr>(l);
        }

        public String _funName = null;
        public ArrayList<ArithExpr> _args = new ArrayList<ArithExpr>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            boolean omit_parens = _args.size() == 1 && _args.get(0) instanceof OperExpr;
            for (ArithExpr e : _args)
                sb.append((sb.length() != 0 ? ", " : "") + e.toString());
            return _funName + (omit_parens ? "" : "(") + sb.toString() + (omit_parens ? "" : ")");
        }

        @Override
        public String toString(boolean format) {
            StringBuilder sb = new StringBuilder();
            for (ArithExpr e : _args)
                sb.append((sb.length() != 0 ? ", " : "") + e.toString(format));
            return _funName + "(" + sb.toString() + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this.getClass().equals(o.getClass())) {
                FunExpr f = (FunExpr) o;
                return _funName.equals(f._funName) && _args.equals(f._args);
            } else
                return false;
        }

        //      COMMENTED BY HADI
//        @Override
//        public int hashCode() {
//            return _funName.hashCode() + _args.hashCode();
//        }
        //ADDED BY HADI:
        @Override
        public int hashCode() {
            return 31 * _funName.hashCode() + _args.hashCode();
        }

        public final static Class ARRAYLIST_ARITH_EXPR_CLASS = new ArrayList<ArithExpr>().getClass();

        @Override
        public ArithExpr substitute(HashMap<String, ArithExpr> subst) {

            ArrayList<ArithExpr> new_args = new ArrayList<ArithExpr>();
            for (ArithExpr e : _args)
                new_args.add(e.substitute(subst));

            try {
                Constructor<? extends FunExpr> constructor = getClass().getDeclaredConstructor(ARRAYLIST_ARITH_EXPR_CLASS);
                return constructor.newInstance(new_args);
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace(System.err);
                System.exit(1);
                return null;
            }
        }

        @Override
        public void collectVars(HashSet<String> vars) {
            for (ArithExpr e : _args)
                e.collectVars(vars);
        }

        @Override
        // Assuming this should not modify existing structure
        public Expr makeCanonical() {

            ArrayList<ArithExpr> new_args = new ArrayList<ArithExpr>();
            for (ArithExpr e : _args)
                new_args.add((ArithExpr) e.makeCanonical());

            try {
                Constructor<? extends FunExpr> constructor = getClass().getDeclaredConstructor(ARRAYLIST_ARITH_EXPR_CLASS);
                return constructor.newInstance(new_args);
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace(System.err);
                System.exit(1);
                return null;
            }
        }

        @Override
        // Modifies existing structure... need to call before it is hashed
        public ArithExpr round() {

            ArrayList<ArithExpr> new_args = new ArrayList<ArithExpr>();
            for (ArithExpr e : _args)
                new_args.add(e.round());

            try {
                Constructor<? extends FunExpr> constructor = getClass().getDeclaredConstructor(ARRAYLIST_ARITH_EXPR_CLASS);
                return constructor.newInstance(new_args);
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace(System.err);
                System.exit(1);
                return null;
            }
        }
    }

    public static class DeltaFunExpr extends FunExpr {

        public final static String DELTA_NAME = "DELTA".intern();

        public DeltaFunExpr(ArithExpr e) {
            this(new ArrayList<ArithExpr>(Arrays.asList(e)));
        }

        public DeltaFunExpr(ArrayList<ArithExpr> l) {
            super(l);
            _funName = DELTA_NAME;
        }

        @Override
        // Might set to Double.POSITIVE_INFINITY, but we'll take a discrete intepretation and set it to 1
        public Double evaluate(HashMap<String, Double> cont_assign) {
            ArithExpr expr = _args.get(0);
            Double eval = expr.evaluate(cont_assign);
            if (eval == null)
                return null;
            else
                return Math.abs(eval) < XADD.PRECISION ? 1d : 0d;
        }

        @Override
        // Might set to Double.POSITIVE_INFINITY, but we'll take a discrete intepretation and set it to 1
        public Double evaluateRange(HashMap<String, Double> low_assign,
                                    HashMap<String, Double> high_assign, boolean use_low) {
            return 1d;
        }
    }
}