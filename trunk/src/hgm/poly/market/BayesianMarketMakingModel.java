package hgm.poly.market;

import hgm.poly.*;
import hgm.poly.bayesian.BayesianModel;

import java.util.Arrays;

/**
 * Created by Hadi Afshar.
 * Date: 19/12/13
 * Time: 8:36 PM
 *
 *
 *
 *  p(Buy|v*, a, b) =   IF[v* > a] : B1             p(SELL|v*, a, b) =   IF[v* > b] : S1        v* ~  U(v - epsilon , v + epsilon)
 *                      IF[v* < a] : B3                                  IF[v* < b] : S3
 *
 */
public class BayesianMarketMakingModel extends BayesianModel<TradingResponse> {

    private double starVarEpsilon;
    public static final double B3 = 0.0;//0.1; // (0.1)
    public static final double B1 = 0.8; // (0.8)
    public static final double S3 = 0.8; // (0.8)
    public static final double S1 = 0.0;//0.1; // (0.1)

    public BayesianMarketMakingModel(MarketMakingDatabase db/*, double epsilon*/) {
        super(db);
        starVarEpsilon = db.getStarVarEpsilon();

        if (starVarEpsilon < 0.0) throw new RuntimeException("epsilon should be positive.");
    }

    @Override
    // Pr(q_{ab} | v_i) where v_i is the value rv. of commodity type i.
    protected PiecewiseExpression computeLikelihoodGivenValueVector(TradingResponse response) {

        String v_i = vars[response.getCommodityTypeId()];
        double a = response.getAskPrice();
        double b = response.getBidPrice();

        switch (response.getTradersResponse()) {
            case BUY:
                return makeBuyLikelihood(v_i, a);
            case SELL:
                return makeSellLikelihood(v_i, b);
            case NO_DEAL:
                return makeNoDealLikelihood(v_i, a, b);
            default:
                throw new RuntimeException("unknown response!");
        }
    }

    private PiecewiseExpression makeBuyLikelihood(String v_i, double a) {
        /**
         * pr(Q = BUY | v_i) =  if v_i > a + epsilon:               B1              (e.g. 0.8)
         *                      if a - epsilon < v_i < a + epsilon: {(X2-X1).v_i + epsilon(X2+X1) +(X1-X2)a}/{2.epsilon}         (e.g. 7*(v_i - a)/(20 * epsilon) + 9/20)
         *                      if v_i < a - epsilon:               B3              (e.g. 0.1)
         */
        Polynomial bConstraint1 = factory.makePolynomial(v_i + "^(1) + " + (-a - starVarEpsilon)); //v > a + epsilon
        Polynomial bPolynomial1 = factory.makePolynomial(Double.toString(B1));
        ConstrainedExpression bStatement1 = new ConstrainedExpression(bPolynomial1, Arrays.asList(bConstraint1));

        Polynomial bConstraint21 = factory.makePolynomial(v_i + "^(1) + " + (-a + starVarEpsilon)); //a - epsilon < v
        Polynomial bConstraint22 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (a + starVarEpsilon)); //v < a + epsilon
        Polynomial bPolynomial2 = factory.makePolynomial(((B1 - B3)/(2.0 * starVarEpsilon)) + "* " + v_i + "^(1) + " + ((starVarEpsilon*(B1 + B3) + (B3 - B1)*a)/ (2.0 * starVarEpsilon))); // 7(v-a)/(20*epsilon) + 9/20
        ConstrainedExpression bStatement2 = new ConstrainedExpression(bPolynomial2, Arrays.asList(bConstraint21, bConstraint22));

        Polynomial bConstraint3 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (a - starVarEpsilon)); //v < a - epsilon
        Polynomial bPolynomial3 = factory.makePolynomial(Double.toString(B3));
        ConstrainedExpression bStatement3 = new ConstrainedExpression(bPolynomial3, Arrays.asList(bConstraint3));

        return new PiecewiseExpression(bStatement1, bStatement2, bStatement3);
    }

    private PiecewiseExpression makeSellLikelihood(String v_i, double b) {
        /**
         * pr(Q = SELL | v_i) = if v_i > b + epsilon:               S1      (e.g. 0.1)
         *                      if b - epsilon < v_i < b + epsilon: {(Y2-Y1).v_i + epsilon(Y2+Y1) +(Y1-Y2)b}/{2.epsilon}                        (e.g. -7*(v_i - b)/(20 * epsilon) + 9/20)
         *                      if v_i < b - epsilon:               S3      (e.g. 0.8)
         */
        Polynomial sConstraint1 = factory.makePolynomial(v_i + "^(1) + " + (-b - starVarEpsilon)); //v > b + epsilon
        Polynomial sPolynomial1 = factory.makePolynomial(Double.toString(S1));
        ConstrainedExpression sStatement1 = new ConstrainedExpression(sPolynomial1, Arrays.asList(sConstraint1));

        Polynomial sConstraint21 = factory.makePolynomial(v_i + "^(1) + " + (-b + starVarEpsilon)); //b - epsilon < v
        Polynomial sConstraint22 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (b + starVarEpsilon)); //v < b + epsilon
        Polynomial sPolynomial2 = factory.makePolynomial(((S1 - S3)/(2.0 * starVarEpsilon)) + "* " + v_i + "^(1) + " + ((starVarEpsilon*(S1 + S3) + (S3 - S1)*b)/(2.0*starVarEpsilon))); // -7(v-b)/(20*epsilon) + 9/20
        ConstrainedExpression sStatement2 = new ConstrainedExpression(sPolynomial2, Arrays.asList(sConstraint21, sConstraint22));

        Polynomial sConstraint3 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (b - starVarEpsilon)); //v < b - epsilon
        Polynomial sPolynomial3 = factory.makePolynomial(Double.toString(S3));
        ConstrainedExpression sStatement3 = new ConstrainedExpression(sPolynomial3, Arrays.asList(sConstraint3));

        return new PiecewiseExpression(sStatement1, sStatement2, sStatement3);
    }

    private PiecewiseExpression makeNoDealLikelihood(String v_i, double a, double b) {
        //note: b<a

        /**
         * pr(Q = BUY | v_i) =  if v_i > a + epsilon:               0.8                                 [B1]
         *                      if a - epsilon < v_i < a + epsilon: 7*(v_i - a)/(20 * epsilon) + 9/20   [B2]
         *                      if v_i < a - epsilon:               0.1                                 [B3]
         */
        Polynomial bConstraint1 = factory.makePolynomial(v_i + "^(1) + " + (-a - starVarEpsilon)); //v > a + epsilon
//        Polynomial bPolynomial1 = factory.makePolynomial(Double.toString(B1));

        Polynomial bConstraint21 = factory.makePolynomial(v_i + "^(1) + " + (-a + starVarEpsilon)); //a - epsilon < v
        Polynomial bConstraint22 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (a + starVarEpsilon)); //v < a + epsilon
        Polynomial bPolynomial2 = factory.makePolynomial(((B1 - B3)/(2.0 * starVarEpsilon)) + "* " + v_i + "^(1) + " + ((starVarEpsilon*(B1 + B3) + (B3 - B1)*a)/ (2.0 * starVarEpsilon))); // 7(v-a)/(20*epsilon) + 9/20

        Polynomial bConstraint3 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (a - starVarEpsilon)); //v < a - epsilon
        Polynomial bPolynomial3 = factory.makePolynomial(Double.toString(B3));
        ///////////////////////////////////////////////////////

        /**
         * pr(Q = SELL | v_i) =  if v_i > b + epsilon:               0.1                                [S1]
         *                      if b - epsilon < v_i < b + epsilon: -7*(v_i - b)/(20 * epsilon) + 9/20  [S2]
         *                      if v_i < b - epsilon:               0.8                                 [S3]
         */
        Polynomial sConstraint1 = factory.makePolynomial(v_i + "^(1) + " + (-b - starVarEpsilon)); //v > b + epsilon
        Polynomial sPolynomial1 = factory.makePolynomial(Double.toString(S1));

        Polynomial sConstraint21 = factory.makePolynomial(v_i + "^(1) + " + (-b + starVarEpsilon)); //b - epsilon < v
        Polynomial sConstraint22 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (b + starVarEpsilon)); //v < b + epsilon
        Polynomial sPolynomial2 = factory.makePolynomial(((S1 - S3)/(2.0 * starVarEpsilon)) + "* " + v_i + "^(1) + " + ((starVarEpsilon*(S1 + S3) + (S3 - S1)*b)/(2.0*starVarEpsilon))); // -7(v-b)/(20*epsilon) + 9/20

        Polynomial sConstraint3 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (b - starVarEpsilon)); //v < b - epsilon

        ///////////////////////////////////////////////////////////////////

        /**
         * pr(Q = NoDeal | v_i) =  [S1], [B1]:      1-[{S1}]-[{B1}]
         *                      if b - epsilon < v_i < b + epsilon: -7*(v_i - b)/(20 * epsilon) + 9/20  [S2]
         *                      if v_i < b - epsilon:               0.8                                 [S3]
         */


        // if S1, B1 : 1-f(B1)-f(S1)
        ConstrainedExpression nStatement1 = new ConstrainedExpression(factory.makePolynomial(Double.toString(1.0 - B1 - S1)), Arrays.asList(sConstraint1, bConstraint1));

        // if S1, B2: 1 - f(S1)-f(B2)
        Polynomial nPolynomial2 = sPolynomial1.clone();//1-S1-B2
        nPolynomial2.addToThis(bPolynomial2);
        nPolynomial2.addToThis(factory.makePolynomial("-1.0"));
        nPolynomial2.multiplyScalarInThis(-1.0);
        ConstrainedExpression nStatement2 = new ConstrainedExpression(nPolynomial2, Arrays.asList(sConstraint1, bConstraint21, bConstraint22));


        // if S2, B2 : 1-f(S2)-f(B2)
        Polynomial nPolynomial3 = sPolynomial2.clone();//1-S2-B2
        nPolynomial3.addToThis(bPolynomial2);
        nPolynomial3.addToThis(factory.makePolynomial("-1.0"));
        nPolynomial3.multiplyScalarInThis(-1.0);
        ConstrainedExpression nStatement3 = new ConstrainedExpression(nPolynomial3, Arrays.asList(sConstraint21, sConstraint22, bConstraint21, bConstraint22));

        // if S2, B3 : 1-f(S2)-f(B3)
        Polynomial nPolynomial4 = sPolynomial2.clone();//1-S2-B3
        nPolynomial4.addToThis(bPolynomial3);
        nPolynomial4.addToThis(factory.makePolynomial("-1.0"));
        nPolynomial4.multiplyScalarInThis(-1.0);
        ConstrainedExpression nStatement4 = new ConstrainedExpression(nPolynomial4, Arrays.asList(sConstraint21, sConstraint22, bConstraint3));

        // if S3, B3 : 1-f(S3)-f(B3)
        ConstrainedExpression nStatement5 = new ConstrainedExpression(factory.makePolynomial(Double.toString(1.0 - S3 - B3)), Arrays.asList(sConstraint3, bConstraint3));

        //if S1, B3: 1-f(S1)-f(B3)
        ConstrainedExpression nStatement6 = new ConstrainedExpression(factory.makePolynomial(Double.toString(1.0 - S1 - B3)), Arrays.asList(sConstraint1, bConstraint3));

        return new PiecewiseExpression(nStatement1, nStatement2, nStatement3, nStatement4, nStatement5, nStatement6);

    }
}

/*
public class BayesianMarketMakingModel extends BayesianModel<TradingResponse> {

    private double starVarEpsilon;

    public BayesianMarketMakingModel(MarketMakingDatabase db*/
/*, double epsilon*//*
) {
        super(db);
        starVarEpsilon = db.getStarVarEpsilon();

        if (starVarEpsilon < 0.0) throw new RuntimeException("epsilon should be positive.");
    }

    @Override
    // Pr(q_{ab} | v_i) where v_i is the value rv. of commodity type i.
    protected PiecewisePolynomial computeLikelihoodGivenValueVector(TradingResponse response) {

        String v_i = vars[response.getCommodityTypeId()];//valueVectorName + "_" + response.getCommodityTypeId();
        double a = response.getAskPrice();
        double b = response.getBidPrice();

        switch (response.getTradersResponse()) {
            case BUY:
                return makeBuyLikelihood(v_i, a);
            case SELL:
                return makeSellLikelihood(v_i, b);
            case NO_DEAL:
                return makeNoDealLikelihood(v_i, a, b);
            default:
                throw new RuntimeException("unknown response!");
        }
    }

    private PiecewisePolynomial makeSellLikelihood(String v_i, double b) {
        */
/**
         * pr(Q = SELL | v_i) =  if v_i > b + epsilon:               0.1
         *                      if b - epsilon < v_i < b + epsilon: -7*(v_i - b)/(20 * epsilon) + 9/20
         *                      if v_i < b - epsilon:               0.8
         *//*

        Polynomial sConstraint1 = factory.makePolynomial(v_i + "^(1) + " + (-b - starVarEpsilon)); //v > b + epsilon
        Polynomial sPolynomial1 = factory.makePolynomial("0.1");
        ConstrainedPolynomial sStatement1 = new ConstrainedPolynomial(sPolynomial1, Arrays.asList(sConstraint1));
//                FunctionVisualizer.visualize(sStatement1, -20, 20, 0.1, "");

        double d = -7.0 / (20.0 * starVarEpsilon);
        Polynomial sConstraint21 = factory.makePolynomial(v_i + "^(1) + " + (-b + starVarEpsilon)); //b - epsilon < v
        Polynomial sConstraint22 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (b + starVarEpsilon)); //v < b + epsilon
        Polynomial sPolynomial2 = factory.makePolynomial(d + "* " + v_i + "^(1) + " + ((9.0 / 20.0) - d * b)); // -7(v-b)/(20*epsilon) + 9/20
        ConstrainedPolynomial sStatement2 = new ConstrainedPolynomial(sPolynomial2, Arrays.asList(sConstraint21, sConstraint22));

        Polynomial sConstraint3 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (b - starVarEpsilon)); //v < b - epsilon
        Polynomial sPolynomial3 = factory.makePolynomial("0.8");
        ConstrainedPolynomial sStatement3 = new ConstrainedPolynomial(sPolynomial3, Arrays.asList(sConstraint3));

        return new PiecewisePolynomial(sStatement1, sStatement2, sStatement3);
    }

    private PiecewisePolynomial makeBuyLikelihood(String v_i, double a) {
        */
/**
         * pr(Q = BUY | v_i) =  if v_i > a + epsilon:               0.8
         *                      if a - epsilon < v_i < a + epsilon: 7*(v_i - a)/(20 * epsilon) + 9/20
         *                      if v_i < a - epsilon:               0.1
         *//*

        Polynomial bConstraint1 = factory.makePolynomial(v_i + "^(1) + " + (-a - starVarEpsilon)); //v > a + epsilon
        Polynomial bPolynomial1 = factory.makePolynomial("0.8");
        ConstrainedPolynomial bStatement1 = new ConstrainedPolynomial(bPolynomial1, Arrays.asList(bConstraint1));

        double c = 7.0 / (20.0 * starVarEpsilon);
        Polynomial bConstraint21 = factory.makePolynomial(v_i + "^(1) + " + (-a + starVarEpsilon)); //a - epsilon < v
        Polynomial bConstraint22 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (a + starVarEpsilon)); //v < a + epsilon
        Polynomial bPolynomial2 = factory.makePolynomial(c + "* " + v_i + "^(1) + " + ((9.0 / 20.0) - c * a)); // 7(v-a)/(20*epsilon) + 9/20
        ConstrainedPolynomial bStatement2 = new ConstrainedPolynomial(bPolynomial2, Arrays.asList(bConstraint21, bConstraint22));

        Polynomial bConstraint3 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (a - starVarEpsilon)); //v < a - epsilon
        Polynomial bPolynomial3 = factory.makePolynomial("0.1");
        ConstrainedPolynomial bStatement3 = new ConstrainedPolynomial(bPolynomial3, Arrays.asList(bConstraint3));

        return new PiecewisePolynomial(bStatement1, bStatement2, bStatement3);
    }

    private PiecewisePolynomial makeNoDealLikelihood(String v_i, double a, double b) {
        //note: b<a

        */
/**
         * pr(Q = BUY | v_i) =  if v_i > a + epsilon:               0.8                                 [B1]
         *                      if a - epsilon < v_i < a + epsilon: 7*(v_i - a)/(20 * epsilon) + 9/20   [B2]
         *                      if v_i < a - epsilon:               0.1                                 [B3]
         *//*

        Polynomial bConstraint1 = factory.makePolynomial(v_i + "^(1) + " + (-a - starVarEpsilon)); //v > a + epsilon
//        Polynomial bPolynomial1 = factory.makePolynomial("0.8");

        double c = 7.0 / (20.0 * starVarEpsilon);
        Polynomial bConstraint21 = factory.makePolynomial(v_i + "^(1) + " + (-a + starVarEpsilon)); //a - epsilon < v
        Polynomial bConstraint22 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (a + starVarEpsilon)); //v < a + epsilon
        Polynomial bPolynomial2 = factory.makePolynomial(c + "* " + v_i + "^(1) + " + ((9.0 / 20.0) - c * a)); // 7(v-a)/(20*epsilon) + 9/20

        Polynomial bConstraint3 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (a - starVarEpsilon)); //v < a - epsilon
        Polynomial bPolynomial3 = factory.makePolynomial("0.1");
        ///////////////////////////////////////////////////////

        */
/**
         * pr(Q = SELL | v_i) =  if v_i > b + epsilon:               0.1                                [S1]
         *                      if b - epsilon < v_i < b + epsilon: -7*(v_i - b)/(20 * epsilon) + 9/20  [S2]
         *                      if v_i < b - epsilon:               0.8                                 [S3]
         *//*

        Polynomial sConstraint1 = factory.makePolynomial(v_i + "^(1) + " + (-b - starVarEpsilon)); //v > b + epsilon
        Polynomial sPolynomial1 = factory.makePolynomial("0.1");

        double d = -7.0 / (20.0 * starVarEpsilon);
        Polynomial sConstraint21 = factory.makePolynomial(v_i + "^(1) + " + (-b + starVarEpsilon)); //b - epsilon < v
        Polynomial sConstraint22 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (b + starVarEpsilon)); //v < b + epsilon
        Polynomial sPolynomial2 = factory.makePolynomial(d + "* " + v_i + "^(1) + " + ((9.0 / 20.0) - d * b)); // -7(v-b)/(20*epsilon) + 9/20

        Polynomial sConstraint3 = factory.makePolynomial("-1 * " + v_i + "^(1) + " + (b - starVarEpsilon)); //v < b - epsilon

        ///////////////////////////////////////////////////////////////////

        */
/**
         * pr(Q = NoDeal | v_i) =  [S1], [B1]:      1-[{S1}]-[{B1}]
         *                      if b - epsilon < v_i < b + epsilon: -7*(v_i - b)/(20 * epsilon) + 9/20  [S2]
         *                      if v_i < b - epsilon:               0.8                                 [S3]
         *//*



        // if S1, B1 : 1-f(B1)-f(S1)
        ConstrainedPolynomial nStatement1 = new ConstrainedPolynomial(factory.makePolynomial("0.1"), Arrays.asList(sConstraint1, bConstraint1));

        // if S1, B2: 1 - f(S1)-f(B2)
        Polynomial nPolynomial2 = sPolynomial1.clone();//1-S1-B2
        nPolynomial2.addToThis(bPolynomial2);
        nPolynomial2.addToThis(factory.makePolynomial("-1.0"));
        nPolynomial2.multiplyScalarInThis(-1.0);
        ConstrainedPolynomial nStatement2 = new ConstrainedPolynomial(nPolynomial2, Arrays.asList(sConstraint1, bConstraint21, bConstraint22));


        // if S2, B2 : 1-f(S2)-f(B2)
        Polynomial nPolynomial3 = sPolynomial2.clone();//1-S2-B2
        nPolynomial3.addToThis(bPolynomial2);
        nPolynomial3.addToThis(factory.makePolynomial("-1.0"));
        nPolynomial3.multiplyScalarInThis(-1.0);
        ConstrainedPolynomial nStatement3 = new ConstrainedPolynomial(nPolynomial3, Arrays.asList(sConstraint21, sConstraint22, bConstraint21, bConstraint22));

        // if S2, B3 : 1-f(S2)-f(B3)
        Polynomial nPolynomial4 = sPolynomial2.clone();//1-S2-B3
        nPolynomial4.addToThis(bPolynomial3);
        nPolynomial4.addToThis(factory.makePolynomial("-1.0"));
        nPolynomial4.multiplyScalarInThis(-1.0);
        ConstrainedPolynomial nStatement4 = new ConstrainedPolynomial(nPolynomial4, Arrays.asList(sConstraint21, sConstraint22, bConstraint3));

        // if S3, B3 : 1-f(S3)-f(B3)
        ConstrainedPolynomial nStatement5 = new ConstrainedPolynomial(factory.makePolynomial("0.1"), Arrays.asList(sConstraint3, bConstraint3));

        //if S1, B3: 1-f(S1)-f(B3)
        ConstrainedPolynomial nStatement6 = new ConstrainedPolynomial(factory.makePolynomial("0.8"), Arrays.asList(sConstraint1, bConstraint3));

        return new PiecewisePolynomial(nStatement1, nStatement2, nStatement3, nStatement4, nStatement5, nStatement6);

    }
}
*/
