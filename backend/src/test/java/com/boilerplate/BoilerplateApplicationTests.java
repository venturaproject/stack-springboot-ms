package com.boilerplate;

import com.boilerplate.support.PostgresTestcontainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestcontainer.class)
class BoilerplateApplicationTests {

    @Test
    void contextLoads() {}
}
