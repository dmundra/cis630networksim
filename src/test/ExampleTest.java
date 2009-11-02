package test;

import static org.testng.Assert.assertNotNull;
import org.testng.annotations.Test;

//This is an example Test which should pass. 
public class ExampleTest {
	
	@Test
	public void test1() {
		Object foobar = new Object();
		assertNotNull(foobar, "foobar is null");
	}
}
