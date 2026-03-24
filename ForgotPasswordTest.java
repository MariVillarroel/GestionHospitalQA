import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
//aaaaaaaaaaaa
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ForgotPasswordTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private void setup() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("http://localhost:5173/admin-login");
        driver.manage().window().maximize();
    }

    private void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testForgotPasswordLink() {
        setup();
        try {
            // Buscar el link
            WebElement forgotPasswordLink = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.linkText("Forgot Password?")
                    )
            );

            // Verificar que existe
            assertTrue(forgotPasswordLink.isDisplayed(), "No se ve el link Forgot Password");

            // Hacer click
            forgotPasswordLink.click();

            // Esperar un poco
            Thread.sleep(2000);

            // Verificar que cambió algo (URL o pantalla)
            String currentUrl = driver.getCurrentUrl();
            System.out.println("URL actual: " + currentUrl);

            assertTrue(
                    currentUrl.contains("forgot") || currentUrl.contains("reset") || !currentUrl.contains("admin-login"),
                    "No cambió la pantalla después del click"
            );

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            teardown();
        }
    }
}
