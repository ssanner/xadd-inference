package hgm.asve.cnsrv.approxator;

import xadd.XADD;

/**
 * Created by Hadi Afshar.
 * Date: 8/11/13
 * Time: 3:27 AM
 */
public interface Approximator {
    /**
     * When the approximator is built (to be passed to factory) the context is not often ready.
     * Therefore before using approximator
     *
     * @param context context should be passed to it via this (potentially heavy) method
     *                TODO calc context statically and make yourself free of setup and all consequences...
     */
    void setupWithContext(XADD context);  //todo maybe context should be passed as an input argument to the following methods

    XADD.XADDNode approximateXadd(XADD.XADDNode node);

    public static final Approximator DUMMY_APPROXIMATOR = new Approximator() {
        @Override
        public void setupWithContext(XADD context) {
        }

        @Override
        public XADD.XADDNode approximateXadd(XADD.XADDNode node) {
            return node;
        }
    };

}
