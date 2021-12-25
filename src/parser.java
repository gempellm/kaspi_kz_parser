import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.Date;
import java.util.List;



import java.io.IOException;

public class parser {
    public static void main(String[] args) throws IOException {
        Date date = java.util.Calendar.getInstance().getTime();
        System.out.println(date);

        System.setProperty("webdriver.chrome.driver", "C:\\chromedriver.exe");

        WebDriver driver = new ChromeDriver();

        driver.get("https://kaspi.kz/shop/c/medical%20equipment/");

        System.out.println(driver.getTitle());

        List<WebElement> goods = driver.findElements(By.className("item-card__name-link"));
        List<WebElement> prices = driver.findElements(By.className("item-card__debet"));

        for (int i = 0; i < goods.size(); i++) {
            System.out.println(goods.get(i).getText()
                    + " " + prices.get(i).findElement(By.className("item-card__prices-price")).getText());
        }

        driver.quit();


    }
}
