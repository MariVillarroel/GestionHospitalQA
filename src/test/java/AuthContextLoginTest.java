import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthContextLoginTest {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        String baseUrl = System.getProperty("baseUrl", "http://localhost:5173");
        driver.get(baseUrl + "/admin-login");
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testLoginStateWithPresetUser() {
        // 1. Verificar que estamos en login
        String initialUrl = driver.getCurrentUrl();
        assertTrue(initialUrl.contains("admin-login"));

        // 2. Llenar email
        WebElement emailInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//input[@type='email']")
                )
        );
        emailInput.clear();
        emailInput.sendKeys("admin.garcia@hospital.com");

        // 3. Llenar password
        WebElement passwordInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//input[@type='password']")
                )
        );
        passwordInput.clear();
        passwordInput.sendKeys("1234");

        // 4. Verificar que sí se llenaron
        assertFalse(emailInput.getAttribute("value").isEmpty(), "El email no se llenó");
        assertFalse(passwordInput.getAttribute("value").isEmpty(), "El password no se llenó");

        // 5. Click en Log in
        WebElement loginButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(),'Log in')]")
                )
        );
        loginButton.click();

        // 6. Esperar cambio real de estado
        wait.until(ExpectedConditions.not(
                ExpectedConditions.urlToBe(initialUrl)
        ));

        // 7. Verificar cambio de estado
        String currentUrl = driver.getCurrentUrl();
        System.out.println("Nueva URL: " + currentUrl);

        assertFalse(
                currentUrl.equals(initialUrl),
                "No cambió el estado de autenticación (sigue en login)"
        );
    }
}