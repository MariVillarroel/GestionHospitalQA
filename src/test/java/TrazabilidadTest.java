package com.hospital.qa;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrazabilidadTest {

    @Test
    public void pruebaConexionJira() {
        // Configuración para que corra en el servidor de GitHub (Headless) lol
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);

        try {
            // 1. Ir a una página estable
            driver.get("https://www.google.com");

            // 2. Obtener el título
            String title = driver.getTitle();
            System.out.println("El título de la página es: " + title);

            // 3. LA PRUEBA DE TRAZABILIDAD:
            // Para que salga VERDE (PASSED): usa "Google"
            // Para que salga ROJO (FAILED): cambia "Google" por "Facebook"
            assertTrue(title.contains("Google"), "El título no coincide, la trazabilidad falló.");
            
        } finally {
            driver.quit();
        }
    }
}