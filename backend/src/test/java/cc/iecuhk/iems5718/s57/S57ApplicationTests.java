package cc.iecuhk.iems5718.s57;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = { "shop.bootstrap.enabled=false"})
class S57ApplicationTests extends MySqlTestSupport {

	@Test
	void contextLoads() {
	}

}
