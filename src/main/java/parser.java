import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.NoSuchElementException;

import java.io.BufferedWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.lang.Thread;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// 1. Скопировать ссылку на карточку.
// 2. Открыть новое окно по ссылке.
// 3. В новом окне по ссылке скопировать значения текста в тегах названия магазина и цены
// 4. Записать это к карточке товара.
// 5. Закрыть окно, открыть следующее.
// 6. Дойдя до конца списка ссылок на карточки, открыть новое окно по следующей ссылке на страницу.
// 7. Закрыть старое окно, повторить пункт 1.
// 8. Если не ссылки на следующую страницу, закрыть окно.

class ProductParseThread extends Thread {
    public final static ChromeOptions chromeOptions = new ChromeOptions().addArguments("--headless").addArguments("--log-level=3");

    private CountDownLatch latch;
    private String name;
    private String href;

    ProductParseThread(String productName, String productHref, CountDownLatch latch) {
        this.name = productName;
        this.href = productHref;
        this.latch = latch;
    }

    @Override
    public void run() {
        Product product = new Product(this.name);

        String link = this.href;

        WebDriver cardDriver = new ChromeDriver();
        // cardDriver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
        cardDriver.get(link);

        List<WebElement> shops = cardDriver.findElements(By.xpath("//td/a"));
        List<WebElement> prices = cardDriver.findElements(By.className("sellers-table__price-cell-text"));

        int priceId = 0;

        for (int j = 0; j < shops.size(); j++) {
            Deal deal = new Deal(shops.get(j).getText(),
                    Integer.parseInt(prices.get(priceId).getText().replaceAll(" ", "").replaceAll("₸", "")));
            product.addDeal(deal);
            priceId += 2;
        }

        parser.addProduct(product);
        shops.clear();
        prices.clear();
        cardDriver.quit();
        this.latch.countDown();
    }
}

class Deal {
    private String shop;
    private int price;

    public Deal(String shop, int price) {
        this.shop = shop;
        this.price = price;
    }

    public String getShop() {
        return this.shop;
    }

    public int getPrice() {
        return this.price;
    }
}

class Product {
    private String name;
    private ArrayList<Deal> deals = new ArrayList<>();

    public Product(String name) {
        this.name = name;
    }

    public void addDeal(Deal deal) {
        this.deals.add(deal);
    }

    public ArrayList<Deal> getDeals() {
        return this.deals;
    }

    public String getName() {
        return this.name;
    }
}

public class parser {



    public final static String[] categoryPaths = new String[] {
            "https://kaspi.kz/shop/c/disinfection%20germicidal%20lamps/",
            "https://kaspi.kz/shop/c/oxygen%20concentrators/"
    };

    public final static String categoryPath = "https://kaspi.kz/shop/c/oxygen%20concentrators/";

    public final static ChromeOptions chromeOptions = new ChromeOptions().addArguments("--headless").addArguments("--log-level=3");

    public static boolean nextFlag = false;

    public static ArrayList<Product> products = new ArrayList<>();

    public static void addProduct(Product product) {
        products.add(product);
    }

    public static void printProducts() {
        for (Product product : products) {
            System.out.println(product.getName());
            ArrayList<Deal> deals = product.getDeals();
            for (Deal deal : deals) {
                System.out.format("%-60.60s %-30.30s%n", deal.getShop(), deal.getPrice());
                System.out.println();
            }
            System.out.println("= = = = = = = = = = =");
        }
    }

    public static void setNextFlag(boolean flag) {
        nextFlag = flag;
    }

    public static void writeProducts() throws IOException {
        File exportFile = new File(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                + "-" + Calendar.getInstance().get(Calendar.MONTH)
                + "-" + new Random().nextInt(10001)
                + ".txt");
        FileWriter exportWriter = new FileWriter(exportFile);
        BufferedWriter writer = new BufferedWriter(exportWriter);
        for (Product product : products) {
            StringBuilder line = new StringBuilder(product.getName());
            ArrayList<Deal> deals = product.getDeals();
            for (Deal deal : deals) {
                line.append("\t").append(deal.getShop()).append("\t").append(deal.getPrice());
            }
            writer.write(line.toString());
            writer.write(System.lineSeparator());
        }
        writer.close();
    }



    public static void parseProducts(List<WebElement> goods) {
        CountDownLatch latch = new CountDownLatch(goods.size());
        ExecutorService executor = Executors.newFixedThreadPool(goods.size());

        for (int i = 0; i < goods.size(); i++) {
            executor.execute(new ProductParseThread(goods.get(i).getText(), goods.get(i).getAttribute("href"), latch));
        }
        executor.shutdown();
        try {
            latch.await();
        } catch(InterruptedException ex) {
            System.out.println(ex);
        }
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("webdriver.chrome.driver", "C:\\chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        driver.get(categoryPath);

        int page = 1;

        while (!nextFlag) {
            List<WebElement> products = driver.findElements(By.className("item-card__name-link"));

            List<WebElement> temp = new ArrayList<>();

            for (int i = 0; i < products.size(); i++) {
                temp.add(products.get(i));
                if (i % 5 == 0 && i != 0) {
                    parseProducts(temp);
                    temp.clear();
                }
            }

            parseProducts(temp);
            page++;
            driver.get(categoryPath + "?page=" + page);
            try {
                boolean flag = driver.findElement(By.className("_disabled")).getText().equals("Следующая →");
                List<WebElement> finalProducts = driver.findElements(By.className("item-card__name-link"));
                parseProducts(finalProducts);
                setNextFlag(flag);
            } catch(NoSuchElementException e) {
            }
        }
        driver.quit();

        writeProducts();
    }
}
