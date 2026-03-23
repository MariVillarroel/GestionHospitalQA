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

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginPageTest {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test
    public void testLoginScreenUI_AndQuickLogin() {
        driver.get("http://localhost:5173/login");

        WebElement title = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(), 'MEDDICAL ERP')]")));
        assertTrue(title.isDisplayed(), "MEDDICAL ERP title should be displayed");

        WebElement adminLoginButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[.//p[contains(text(), 'Admin')]]")));
        assertTrue(adminLoginButton.isDisplayed(), "Admin login button should be visible");

        adminLoginButton.click();

        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
        assertTrue(!driver.getCurrentUrl().contains("/login"), "User should be redirected after login");
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
