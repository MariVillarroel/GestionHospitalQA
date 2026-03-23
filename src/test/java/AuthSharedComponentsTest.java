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

import jakarta.mail.*;
import jakarta.mail.Flags.Flag;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthSharedComponentsTest {
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
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        driver.get("http://localhost:5173");
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void verifyBackground(String context) {
        List<WebElement> backgrounds = driver.findElements(
                By.cssSelector("div[style*='background-image']")
        );

        assertTrue(!backgrounds.isEmpty(), "Background element not found for " + context);

        WebElement bg = backgrounds.get(0);
        String style = bg.getAttribute("style");

        assertTrue(style.contains("background-image"),
                "Background image not applied for " + context);

        assertTrue(style.contains("auth_bg_image.png"),
                "Background image path not correct for " + context + ". Actual style: " + style);
    }

    @Test
    public void blurredBackgroundConsistency() throws Exception {
        verifyBackground("Login");

        WebElement forgotPasswordLink = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//*[@id='root']/div/div[2]/div[2]/div[2]/form/div[3]/a")
                )
        );
        forgotPasswordLink.click();
        verifyBackground("Forgot Password");

        WebElement emailTextbox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("email"))
        );
        emailTextbox.sendKeys("qahospitales@gmail.com");

        WebElement submitEmail = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(),'Submit')]")
                )
        );
        submitEmail.click();
        verifyBackground("OTP Verification");

        WebElement otpcode = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[@id='root']/div/div[2]/div[2]/div[2]/form/div[2]/div/div[2]/input")
                )
        );

        String otp = GmailOtpReader.fetchOtpWithRetry();
        System.out.println("OTP obtenido: " + otp);
        otpcode.sendKeys(otp);

        WebElement verifyCodeBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[contains(text(),'Verify')]")
                )
        );
        verifyCodeBtn.click();

        verifyBackground("Set New Password");
    }
}

class GmailOtpReader {

    public static String fetchOtpWithRetry() throws Exception {
        int maxAttempts = 6;
        int waitMillis = 5000;

        for (int i = 0; i < maxAttempts; i++) {
            String otp = fetchLatestOtp();
            if (otp != null && !otp.isBlank()) {
                return otp;
            }
            Thread.sleep(waitMillis);
        }

        throw new RuntimeException("No se pudo obtener el OTP desde Gmail.");
    }

    private static String fetchLatestOtp() throws Exception {
        //String username = System.getenv("TEST_OTP_EMAIL");
        //String appPassword = System.getenv("TEST_OTP_PASSWORD");

        String username = "qahospitales@gmail.com";
        String appPassword = "jiulcsozjrkzubjt";

        if (username == null || appPassword == null) {
            throw new RuntimeException("Faltan TEST_GMAIL_USER o TEST_GMAIL_APP_PASSWORD.");
        }

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect("imap.gmail.com", username, appPassword);

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        Date fiveMinutesAgo = new Date(System.currentTimeMillis() - 5 * 60 * 1000);

        SearchTerm recentTerm = new ReceivedDateTerm(ComparisonTerm.GE, fiveMinutesAgo);
        SearchTerm unseenTerm = new FlagTerm(new Flags(Flag.SEEN), false);
        SearchTerm combined = new AndTerm(recentTerm, unseenTerm);

        Message[] messages = inbox.search(combined);

        if (messages.length == 0) {
            inbox.close(false);
            store.close();
            return null;
        }

        Message latest = Arrays.stream(messages)
                .max(Comparator.comparing(message -> {
                    try {
                        return message.getReceivedDate();
                    } catch (MessagingException e) {
                        return new Date(0);
                    }
                }))
                .orElse(null);

        if (latest == null) {
            inbox.close(false);
            store.close();
            return null;
        }

        String body = extractText(latest);
        latest.setFlag(Flag.SEEN, true);

        inbox.close(false);
        store.close();

        Pattern otpPattern = Pattern.compile("\\b(\\d{4,6})\\b");
        Matcher matcher = otpPattern.matcher(body);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new RuntimeException("No se encontró un OTP válido en el correo.");
    }

    private static String extractText(Part part) throws Exception {
        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            return content == null ? "" : content.toString();
        }

        if (part.isMimeType("text/html")) {
            Object content = part.getContent();
            return content == null ? "" : content.toString().replaceAll("<[^>]+>", " ");
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                result.append(extractText(bodyPart)).append(" ");
            }

            return result.toString();
        }

        return "";
    }
}