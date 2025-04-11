package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    @Test
    public void testGreet() {
        App app = new App();
        String result = app.greet("Ram");
        assertEquals("Hello, Ram!", result);
    }
}
