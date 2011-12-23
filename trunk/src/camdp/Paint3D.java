package camdp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import xadd.XADD;

public class Paint3D {
	public static double size3D = 20d;
	//for the 3 problems: no address for file because of running on server
	//public static String  NAME_FILE_3D="RoverNonLinear3D";
	//public static String  NAME_FILE_3D="refinement3D";
	//public static String  NAME_FILE_3D="2inventory3D";
	//public static String  NAME_FILE_3D="RoverNonLinear3D"; //turn rover=true;
	public static String  FILE_NAME;
	public final static boolean PRINTSCREENEVAL = true;
	CAMDP _camdp= null;
	public int counter = 0;


	public Paint3D(CAMDP camdp)
	{
		_camdp = camdp;
		FILE_NAME = _camdp.NAME_FILE_3D; 
		counter = 0;
	}


	/*
	 * Set the value of the other continous variables with the maxValue, without boolean assigments 
	 */
	
	public void create3DDataFile(Integer XDD, String xVar, String yVar) {
		try {
     		counter++;
            BufferedWriter out = new BufferedWriter(new FileWriter(FILE_NAME+counter+".txt"));
            
            HashMap<String,Boolean> bool_assign = new HashMap<String,Boolean>();
            	
            for (String var : _camdp._context._hsBooleanVars) {
       		   bool_assign.put(var, false);	
       		}
             //values in order for rover, refinement,inventory
            Double minX,minY,maxX,maxY;
            if (FILE_NAME.contains("rover"))
            {
            	minX = -40.0; maxX = 40.0; minY = -20.0; maxY=20.0;
            }
            else             
            //In case we want to plot between the 
             minX= _camdp._context._hmMinVal.get(xVar);
             maxX= _camdp._context._hmMaxVal.get(xVar);
             minY= _camdp._context._hmMinVal.get(yVar);
             maxY= _camdp._context._hmMaxVal.get(yVar);
             
             Double incX= (maxX-minX)/(size3D-1);
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
                 /*out.append(X.get(i).toString()+" ");
                 out.append(Y.get(i).toString()+" ");*/
                 for(int j=0;j<size3D;j++){
                	 
             
  		     		HashMap<String,Double> cont_assign = new HashMap<String,Double>();
  		     		
  		     		for (Map.Entry<String,Double> me : _camdp._context._hmMinVal.entrySet()) {
  		     			cont_assign.put(me.getKey(),  me.getValue());
  		     		}
  		     			     		
              		cont_assign.put(xVar,  X.get(j));
              		cont_assign.put(yVar,  Y.get(i));
              		
              		Double z=_camdp._context.evaluate(XDD, bool_assign, cont_assign);
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
