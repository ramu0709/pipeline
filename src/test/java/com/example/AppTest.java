package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class AppTest {

    @Test
    public void testGreet() {
        App app = new App();
        String result = app.greet("Ram");
        assertEquals("Hello, Ram!", result);
    }

    @Test
    public void testMainMethodOutput() {
        // Capture system output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        App.main(new String[]{});

        System.setOut(originalOut);
        assertTrue(outContent.toString().trim().contains("Hello, World"));
    }
}

