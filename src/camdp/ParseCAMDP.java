package camdp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import xadd.XADD;
import xadd.ExprLib.ArithExpr;
import xadd.ExprLib.DoubleExpr;

public class ParseCAMDP {

    CAMDP _camdp = null;

    public final static ArithExpr ZERO = new DoubleExpr(0d);
    public final static ArithExpr ONE = new DoubleExpr(1d);
    ArrayList<String> CVars = new ArrayList<String>();
    ArrayList<String> BVars = new ArrayList<String>();
    ArrayList<String> NSCVars = new ArrayList<String>();
    ArrayList<String> NSBVars = new ArrayList<String>();
    ArrayList<String> NoiseVars = new ArrayList<String>();
    ArrayList<String> ICVars = new ArrayList<String>();
    ArrayList<String> IBVars = new ArrayList<String>();
    ArrayList<String> AVars = new ArrayList<String>();
    public HashMap<String, Double> _minCVal = new HashMap<String, Double>();
    public HashMap<String, Double> _maxCVal = new HashMap<String, Double>();
    ArrayList<Double> contParam = new ArrayList<Double>(2);
    //	ArrayList<Integer> constraints = new ArrayList<Integer>();
    BigDecimal discount;
    Integer iterations;
    boolean LINEARITY;
    double MAXREWARD;
    
    HashMap<String, Double> _initCVal = new HashMap<String, Double>();
    HashMap<String, Boolean> _initBVal = new HashMap<String, Boolean>();

    HashMap<String, CAction> _name2Action = new HashMap<String, CAction>();

    public ParseCAMDP(CAMDP camdp) {
        _camdp = camdp;
    }

    public void buildCAMDP(ArrayList input) {

        if (input == null) {
            System.out.println("Missing or empty input file!");
            System.exit(1);
        }

        Iterator i = input.iterator();
        Object o;

        // Set up variables
        o = i.next();
        if (!(o instanceof String) || !((String) o).equalsIgnoreCase("cvariables")) {
            exit("Missing cvariable declarations: " + o);
        }
        o = i.next();
        CVars = (ArrayList<String>) ((ArrayList) o).clone();
        o = i.next();
        if (!(o instanceof String) || !((String) o).equalsIgnoreCase("min-values")) {
            exit("Missing min-values declarations: " + o);
        }
        o = i.next();
        for (int index = 0; index < CVars.size(); index++) {
            String var = CVars.get(index);
            String val = ((ArrayList) o).get(index).toString();
            if (!val.trim().equalsIgnoreCase("NA")) try {
                double min_val = Double.parseDouble(val);
                _minCVal.put(var, min_val);
                _minCVal.put(var + "'", min_val); // Bounds apply to primed vars as well
            } catch (NumberFormatException nfe) {
                exit("\nIllegal min-value: " + var + " = " + val + " @ index " + index);
            }
        }
        o = i.next();
        if (!(o instanceof String) || !((String) o).equalsIgnoreCase("max-values")) {
            exit("Missing max-values declarations: " + o);
        }
        o = i.next();
        for (int index = 0; index < CVars.size(); index++) {
            String var = CVars.get(index);
            String val = ((ArrayList) o).get(index).toString();
            if (!val.trim().equalsIgnoreCase("NA")) try {
                double max_val = Double.parseDouble(val);
                _maxCVal.put(var, max_val);
                _maxCVal.put(var + "'", max_val); // Bounds apply to primed vars as well
            } catch (NumberFormatException nfe) {
                exit("\nIllegal max-value: " + var + " = " + val + " @ index " + index);
            }
        }
        o = i.next();
        if (!(o instanceof String) || !((String) o).equalsIgnoreCase("bvariables")) {
            exit("Missing bvariable declarations: " + o);
        }
        o = i.next();
        //System.out.println(o);

        // Ensure boolean decisions are entered at the top of the XADD ordering
        BVars = (ArrayList<String>) ((ArrayList) o).clone();
        for (String var : BVars) {
            _camdp._context.getVarIndex(_camdp._context.new BoolDec(var), true);
            _camdp._context.getVarIndex(_camdp._context.new BoolDec(var + "'"), true);
        }

        // Noise vars -- defined after state variables if present
        o = i.next();
        if ((o instanceof String) && ((String) o).equalsIgnoreCase("nvariables")) {
            o = i.next();
            NoiseVars = (ArrayList<String>) ((ArrayList) o).clone();
            o = i.next();
        } else {
            System.out.println("Missing nvariable declarations after state vars... assuming only discrete noise on continuous transitions.");
            NoiseVars = new ArrayList<String>();
        }

        if (!(o instanceof String)) {
            exit("Expected identifier {icvariables,ibvariables} but got " + o);
        } else if (((String) o).equalsIgnoreCase("ivariables")) {
            // This is for backwards compatibility when ivariables was a required declaration... if used, ivariables must be empty
            o = i.next();
            if (!((ArrayList) o).isEmpty())
                exit("Cannot handle non-empty ivariable declarations " + o + ": must use ibvariables or icvariables to indicate type.");
            System.err.println("WARNING: use of ivariables declaration has now been replaced with ibvariables and icvariables, ignoring since empty.");
        } else {

            // Expect icvariables, min-values, max-values, ibvariables
            if (!(o instanceof String) || !((String) o).equalsIgnoreCase("icvariables")) {
                exit("Missing icvariable declarations: " + o);
            }
            o = i.next();
            ICVars = (ArrayList<String>) ((ArrayList) o).clone();
            o = i.next();
            if (!(o instanceof String) || !((String) o).equalsIgnoreCase("min-values")) {
                exit("Missing min-values declarations: " + o);
            }
            o = i.next();
            for (int index = 0; index < ICVars.size(); index++) {
                String var = ICVars.get(index);
                String val = ((ArrayList) o).get(index).toString();
                if (!val.trim().equalsIgnoreCase("NA")) try {
                    double min_val = Double.parseDouble(val);
                    _minCVal.put(var, min_val);
                } catch (NumberFormatException nfe) {
                    exit("\nIllegal min-value: " + var + " = " + val + " @ index " + index);
                }
            }
            o = i.next();
            if (!(o instanceof String) || !((String) o).equalsIgnoreCase("max-values")) {
                exit("Missing max-values declarations: " + o);
            }
            o = i.next();
            for (int index = 0; index < ICVars.size(); index++) {
                String var = ICVars.get(index);
                String val = ((ArrayList) o).get(index).toString();
                if (!val.trim().equalsIgnoreCase("NA")) try {
                    double max_val = Double.parseDouble(val);
                    _maxCVal.put(var, max_val);
                } catch (NumberFormatException nfe) {
                    exit("\nIllegal max-value: " + var + " = " + val + " @ index " + index);
                }
            }
            o = i.next();
            if (!(o instanceof String) || !((String) o).equalsIgnoreCase("ibvariables")) {
                exit("Missing ibvariable declarations: " + o);
            }
            o = i.next();
            //System.out.println(o);

            // Ensure boolean decisions are entered at the top of the XADD ordering
            IBVars = (ArrayList<String>) ((ArrayList) o).clone();
            for (String var : IBVars)
                _camdp._context.getVarIndex(_camdp._context.new BoolDec(var), true);
        }

        // avariable declarations are optional to allow parsing of non-continuously parameterized action cmdps
        o = i.next();
        Object push_back = null;
        if (!(o instanceof String) || !((String) o).equalsIgnoreCase("avariables")) {
            System.out.println("Missing avariable declarations before " + o + "... assuming there are no continuously parameterized actions.");
            AVars = new ArrayList<String>();
            push_back = o;
        } else {
            o = i.next();
            AVars = (ArrayList<String>) ((ArrayList) o).clone();
        }

        // Record all continuous vars (except next state)
        ArrayList<String> contVars = new ArrayList<String>();
        contVars.addAll(CVars);
        contVars.addAll(ICVars);
        contVars.addAll(AVars);

        // Setup next state variables
        NSBVars.clear();
        NSCVars.clear();
        for (String s : BVars)
            NSBVars.add(s + "'");
        for (String s : CVars)
            NSCVars.add(s + "'");

        // Set up actions
        while (true) {
            o = push_back == null ? i.next() : push_back; // Could have a saved object if avariable declaration was missing
            push_back = null; // Whether or not it was used, clear it
            if (!(o instanceof String)
                    || !((String) o).equalsIgnoreCase("action")) {
                break;
            }

            // o == "action" + continuous action
            String aname = (String) i.next();
            o = i.next();

            // o is either empty, "()", or "( lb <= var <= ub ... )", e.g.
            // [-1000000, <, =, a, <, =, 1000000]
            // []
            // k'
            if (o instanceof ArrayList) {
                // Non-empty parameter list
                parseActionParam((ArrayList) o);
                o = i.next(); // Advance to next token
            } // otherwise there was no parameter list and o contains beginning of CPF

            // This handles intermediate and next-state var CPFs
            HashMap<String, ArrayList> cpt_map = new HashMap<String, ArrayList>();
            while (!((String) o).equalsIgnoreCase("reward") && !((String) o).equalsIgnoreCase("noise")) {
                cpt_map.put((String) o, (ArrayList) i.next());
                o = i.next();
            }

            // Handle noise
            HashMap<String, ArrayList> noise_map = new HashMap<String, ArrayList>();
            if ((o instanceof String) && ((String) o).equalsIgnoreCase("noise")) {
                while (!((String) o).equalsIgnoreCase("reward")) {
                    if (((String) o).equalsIgnoreCase("noise"))
                        o = i.next();
                    if (!(o instanceof String)) {
                        exit("Expected noise variable header or 'reward' but got: " + o);
                    }
                    noise_map.put((String) o, (ArrayList) i.next());
                    o = i.next();
                }
            } else if (NoiseVars.size() > 0) {
                exit("Expected noise declaration for noise vars: " + NoiseVars);
            }

            // Set up reward
            if (!(o instanceof String) || !((String) o).equalsIgnoreCase("reward")) {
                exit("Missing reward declaration for action: " + aname + " " + o);
            }

            // new parser format : has + for summing rewards
            int reward_dd = _camdp._context.buildCanonicalXADD((ArrayList) i.next());
            o = i.next();
            while (!((String) o).equalsIgnoreCase("endaction")) {
                if (((String) o).equalsIgnoreCase("+")) {
                    int reward_summand = _camdp._context.buildCanonicalXADD((ArrayList) i.next());
                    reward_dd = _camdp._context.applyInt(reward_dd, reward_summand, _camdp._context.SUM);
                } else
                    exit("No endaction, expected a '+' in " + aname);
                o = i.next();
            }

            _name2Action.put(aname, new CAction(_camdp, aname, contParam,
                    cpt_map, reward_dd, CVars, BVars, ICVars, IBVars,
                    AVars, NSCVars, NSBVars, noise_map, NoiseVars));

        } // endaction

        // Check for constraints declaration (can be multiple)
//		constraints =new ArrayList<Integer>();
        while (true) {
            if (!(o instanceof String) || !((String) o).equalsIgnoreCase("constraint")) {
                break;
            }

            // Constraints exist!
            exit("Constraints are no longer accepted in input files.\nConstraints should be applied directly to the reward to yield -Infinity for illegal states.");

//			o=i.next(); // get dd
//			ArrayList next_constraint = (ArrayList) o;
//			int next_constraint_dd = _camdp._context.buildCanonicalXADD(next_constraint);
//			constraints.add(next_constraint_dd);
//			
//			o = i.next(); // get endconstraint
//			o = i.next(); // get constraint or discount
        }

        // Read discount and tolerance
        if (!(o instanceof String)
                || !((String) o).equalsIgnoreCase("discount")) {
            System.out.println("Missing discount declaration: " + o);
            System.exit(1);
        }
        discount = ((BigDecimal) i.next());
        
        // Initial State declarations are optional
        o = i.next();
        push_back = null;
        if (!(o instanceof String) || !((String) o).equalsIgnoreCase("initialState")) {
            System.out.println("Missing initial State declaration before " + o + "... assuming complete solution is intended.");
            push_back = o;
        } 
        else {
            o = i.next();
            for (int index = 0; index < CVars.size(); index++) {
                String var = CVars.get(index);
                String val = ((ArrayList) o).get(index).toString();
                if (!val.trim().equalsIgnoreCase("NA")) try {
                    double ini_cval = Double.parseDouble(val);
                    _initCVal.put(var, ini_cval);
                } catch (NumberFormatException nfe) {
                    exit("\nIllegal initial-cvalue: " + var + " = " + val + " @ index " + index);
                }
            }
            o = i.next(); //
            for (int index = 0; index < BVars.size(); index++) {
                String var = BVars.get(index);
                String val = ((ArrayList) o).get(index).toString();
                if (val.trim().equalsIgnoreCase("true")) _initBVal.put(var, true);
                else if (val.trim().equalsIgnoreCase("false")) _initBVal.put(var, false);
                else exit("\nIllegal initial-bvalue: " + var + " = " + val + " @ index " + index);
            }
        }        
        o = push_back == null ? i.next() : push_back; // Could have a saved object if init values declaration was missing
        if (!(o instanceof String)
                || !((String) o).equalsIgnoreCase("iterations")) {
            System.out.println("Missing iterations declaration: " + o);
            System.exit(1);
        }
        iterations = (new Integer((String) i.next()));
        if (i.hasNext()){
        	o=i.next(); 
        	if (!(o instanceof String)
                || !( ((String) o).equalsIgnoreCase("LINEAR") || ((String) o).equalsIgnoreCase("NONLINEAR")) ) {
            System.err.println("Missing linearity declaration: " + o);
            LINEARITY = true;    
        	}
        	else{
        		LINEARITY = ((String)o).equalsIgnoreCase("LINEAR");
        	}
        }
        else{
            System.out.println("Missing linearity declaration, assumes linear.");
            LINEARITY = true;    
        }
        if (i.hasNext()){
        	o=i.next(); 
        	if (!(o instanceof String)
                || !((String) o).equalsIgnoreCase("MAXREWARD") || !i.hasNext()) {
        		 System.out.println("Missing Max Imediate Reward declaration, assumes 0.");
        		 MAXREWARD = 0.0;    
        	}
        	else{
        		o=i.next();
        		MAXREWARD = Double.parseDouble(o.toString());
        	}
        }
        else{
            System.out.println("Missing Max Imediate Reward declaration, assumes 0.");
            MAXREWARD = 0.0;    
        }
    }

    public HashMap<String, Double> get_minCVal() {
        return _minCVal;
    }

    public void set_minCVal(HashMap<String, Double> _minCVal) {
        this._minCVal = _minCVal;
    }

    public HashMap<String, Double> get_maxCVal() {
        return _maxCVal;
    }

    public void set_maxCVal(HashMap<String, Double> _maxCVal) {
        this._maxCVal = _maxCVal;
    }

    public HashMap<String, Double> get_initCVal() {
        return _initCVal;
    }
    public void set_initCVal(HashMap<String, Double> initCVal) {
        this._initCVal = initCVal;
    }

    public HashMap<String, Boolean> get_initBVal() {
        return _initBVal;
    }
    
    public void set_initBVal(HashMap<String, Boolean> _initBVal) {
        this._initBVal = _initBVal;
    }
    
    private void parseActionParam(ArrayList<String> params) {
        // All actions required to have bounds, e.g.
        // [0, <, =, a1, <, =, 200, ^, 0, <, =, a2, <, =, 200]
        // [-1000000, <, =, a, <, =, 1000000]
        for (int var_index = 3; var_index < params.size(); var_index += 8) {
            String var_name = params.get(var_index);
            int avar_index = AVars.indexOf(var_name);
            if (avar_index < 0)
                exit("Expected an action-variable, but got: '" + var_name + "'");
            contParam.add(avar_index * 2, new Double(params.get(var_index - 3)) /* LB */);
            contParam.add((avar_index * 2) + 1, new Double(params.get(var_index + 3)) /* UB */);
        }
    }

    public void exit(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    public XADD get_context() {
        return _camdp._context;
    }

    public void set_context(XADD _context) {
        this._camdp._context = _context;
    }

    public ArrayList<String> getCVars() {
        return CVars;
    }

    public ArrayList<String> getBVars() {
        return BVars;
    }

    public ArrayList<String> getAVars() {
        return AVars;
    }

    public ArrayList<String> getICVars() {
        return ICVars;
    }

    public ArrayList<String> getIBVars() {
        return IBVars;
    }

    public ArrayList<String> getNSCVars() {
        return NSCVars;
    }

    public ArrayList<String> getNSBVars() {
        return NSBVars;
    }

    public ArrayList<String> getNoiseVars() {
        return NoiseVars;
    }

//	public ArrayList<Integer> getConstraints() {
//		return constraints;
//	}

    public BigDecimal getDiscount() {
        return discount;
    }

    public Integer getIterations() {
        return iterations;
    }

    public HashMap<String, CAction> getHashmap() {
        return _name2Action;
    }
    
}
