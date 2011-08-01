package camdp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import xadd.XADD;

public class Paint3D {
	public static double size3D = 10d;
	public static String  NAME_FILE_3D="./src/cmdp/ex/data/File3D";
	public static boolean rover;
	public final static boolean PRINTSCREENEVAL = false;
	CAMDP camdp = null;
	XADD xadd= null;


	public Paint3D(CAMDP camdp2, XADD context)
	{
		camdp = camdp2;
		xadd = context;
	}


	/*
	 * Set the value of the other continous variables with the maxValue, without boolean assigments 
	 */
	
	public void create3DDataFile(Integer XDD, String xVar, String yVar) {
		try {
     		
            BufferedWriter out = new BufferedWriter(new FileWriter(NAME_FILE_3D));
            
            HashMap<String,Boolean> bool_assign = new HashMap<String,Boolean>();
            	
            for (String var : camdp.get_alBVars()) {
       		   bool_assign.put(var, false);	
       		}
            if (rover){
            	bool_assign.put("p1", true);
            }
            else{//knapsack
            	
            }
             
             Double minX= xadd._hmMinVal.get(xVar);
             Double maxX= xadd._hmMaxVal.get(xVar);
             Double incX= (maxX-minX)/(size3D-1);
             
             Double minY= xadd._hmMinVal.get(yVar);
             Double maxY= xadd._hmMaxVal.get(yVar);
             Double incY= (maxY-minY)/(size3D-1);
             
             
            ArrayList<Double> X = new ArrayList<Double>();
            ArrayList<Double> Y = new ArrayList<Double>();
            

             Double xval=minX;
             Double yval=minY;
             for(int i=0;i<size3D;i++){
            	 X.add(xval);
            	 Y.add(yval);
            	 xval=xval+incX;
            	 yval=yval+incY;
             }
             System.out.println(">> Evaluations");
             for(int i=0;i<size3D;i++){
                 out.append(X.get(i).toString()+" ");
                 out.append(Y.get(i).toString()+" ");
                 for(int j=0;j<size3D;j++){
                	 
             
  		     		HashMap<String,Double> cont_assign = new HashMap<String,Double>();
  		     		
  		     		for (Map.Entry<String,Double> me : xadd._hmMinVal.entrySet()) {
  		     			cont_assign.put(me.getKey(),  me.getValue());
  		     		}
  		     			     		
              		cont_assign.put(xVar,  X.get(j));
              		cont_assign.put(yVar,  Y.get(i));
              		
              		Double z=xadd.evaluate(XDD, bool_assign, cont_assign);
              		if (PRINTSCREENEVAL){
             		System.out.println("Eval: [" + bool_assign + "], [" + cont_assign + "]"
             						   + ": " + z);
              		}

             		out.append(z.toString()+" ");
                   /*
             		cont_assign.put(xVar,  200.0d/3.0d);
              		cont_assign.put(yVar,  100.0d/3.0d);
              		z=_context.evaluate(XDD, bool_assign, cont_assign);
             		System.out.println("Eval: [" + bool_assign + "], [" + cont_assign + "]"
             						   + ": " + z);		

             		out.append(z.toString()+" ");
              		*/
              		
             		
                 }
                 out.newLine();
             }
            //out.append(System.getProperty("line.separator"));
             out.close();
             
             
             
         } catch (IOException e) {
         	System.out.println("Problem with the creation 3D file");
         	System.exit(0);
         }
	}
}
