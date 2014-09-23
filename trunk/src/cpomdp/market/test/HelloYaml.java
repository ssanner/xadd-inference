package cpomdp.market.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class HelloYaml {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws FileNotFoundException {
		Yaml yaml = new Yaml();
	
		String yamlLocation = "./trunk/src/cpomdp/market/configuration.yaml";
		
//		System.out.println(yaml.dump(yaml.load(new FileInputStream(new File(yamlLocation)))));

		Map<String, Object> values = (Map<String, Object>) yaml.load(new FileInputStream(new File(yamlLocation)));

		System.out.println("domain " + values.get("domain").getClass().getName());
		System.out.println("number_iterations " + values.get("number_iterations").getClass().getName());
		System.out.println("discount_factor " + values.get("discount_factor").getClass().getName());
		
		String domain = (String)values.get("domain");
		Integer numIterations = (Integer) values.get("number_iterations");
		Double discountFactor = (Double) values.get("discount_factor");
		
		
		
	}
}