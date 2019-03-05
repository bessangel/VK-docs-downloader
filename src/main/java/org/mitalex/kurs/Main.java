package org.mitalex.kurs;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static Config config = new Config("config.properties");

    static int minWindNum = 3;

    public static void main(String[] args) throws InterruptedException, IOException {

		FirefoxOptions firefoxOptions = new FirefoxOptions();
		System.setProperty("webdriver.gecko.driver", config.get(Config.ConfigKeys.GECKO_DRIVER));
        String downloadDir = config.get(Config.ConfigKeys.DOWNLOAD_DIR);
        new File(downloadDir).mkdirs();

        FirefoxProfile profile = new FirefoxProfile( );
        profile.setPreference("browser.download.folderList", 2) ;
        profile.setPreference("browser.download.manager.showWhenStarting", false);
        profile.setPreference("browser.download.downloadDir", downloadDir) ;
        profile.setPreference("browser.helperApps.alwaysAsk.force", false);
        firefoxOptions.setProfile(profile);
        WebDriver driver = new FirefoxDriver(firefoxOptions);

        // open URL like driver.navigate().to("http://www.google.com");
        driver.get(config.get(Config.ConfigKeys.URL));


        String loginPageTitle = config.get(Config.ConfigKeys.LOGIN_PAGE_TITLE);
        if ( !"".equals(loginPageTitle) && driver.getTitle().toLowerCase().startsWith(loginPageTitle)) {
            // Find the text input element by its name
            String login = config.get(Config.ConfigKeys.LOGIN);
            String password = config.get(Config.ConfigKeys.PASSWORD);

            driver.findElement(By.cssSelector( config.get(Config.ConfigKeys.LOGIN_CSS_SELECTOR) )).sendKeys(login);
            driver.findElement(By.cssSelector( config.get( Config.ConfigKeys.PASSWORD_CSS_SELECTOR) )).sendKeys(password);
            driver.findElement(By.cssSelector( config.get(Config.ConfigKeys.LOGIN_BUTTON_CSS_SELECTOR))).submit();

            // Check the title of the page
            LOG.info("Page title is: " + driver.getTitle());

        }

        // Wait for the page to load, timeout after 10 seconds
        (new WebDriverWait(driver, 20)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return !d.getTitle().toLowerCase().startsWith( config.get(Config.ConfigKeys.LOGIN_PAGE_TITLE));
            }
        });

        // Wait page ajax loads
        driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);

        String mainWindowHandler = driver.getWindowHandle();

        List<WebElement> elems = driver.findElements( By.cssSelector( config.get(Config.ConfigKeys.CSS_SELECTOR)) );

        int limitDownloads = Integer.parseInt( config.get(Config.ConfigKeys.LIMIT_DOWNLOADS) );

        for(int i = 0; i < elems.size() && limitDownloads > 0; i++) {
            WebElement el = elems.get(i);
            String url = el.getAttribute( config.get(Config.ConfigKeys.CSS_SELECTOR_ATTRIBUTE_URL));
            String filename = el.getAttribute( config.get(Config.ConfigKeys.CSS_SELECTOR_ATTRIBUTE_FILENAME));
            // Качаем пока что только PDF
            if(filename.toLowerCase().endsWith(".pdf")){
                clickA(el, driver, filename, config.getDownloadDir(), mainWindowHandler);
                limitDownloads--;
            }

        }

        LOG.info("Page title is: " + driver.getTitle());

         driver.quit();
    }

    private static void downloadUrlWithCookie(WebDriver driver, String url, String filename, String downloadDir) {
        LOG.info(filename + "\t" + url);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            // set cookie
            Set<Cookie> cookies = driver.manage().getCookies();
            if (cookies != null) {
                StringBuilder cookiesString = new StringBuilder();
                for (Cookie cookie : cookies) {
                    cookiesString.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
                }
                connection.setRequestProperty("Cookie", cookiesString.toString());
            }
            // download
            InputStream ins = connection.getInputStream();
            BufferedInputStream bins = new BufferedInputStream(ins);
            FileOutputStream fout = new FileOutputStream(new File(downloadDir,filename));
            byte[] buf = new byte[8096];
            int bytesReaded ;
            while ( (bytesReaded = bins.read(buf)) != -1 ){
                fout.write(buf,0, bytesReaded);
            }
            bins.close();
            fout.close();

        } catch (IOException e) {
            LOG.error(e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }

    private static void clickA(WebElement el, WebDriver driver, String filename, String win, String mainWindowHandler) {
        el.click();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ArrayList<String> handles = new ArrayList<>(driver.getWindowHandles());

        for(int i=0; i<handles.size(); i++){
                String title = handles.get(i);
                if(!title.equals(mainWindowHandler)) {
                    driver.switchTo().window(title);
                    String url = driver.findElement(By.cssSelector("#iframe")).getAttribute("src");
                    downloadUrlWithCookie(driver, url, filename, config.getDownloadDir());
                    driver.close();
                    driver.switchTo().window(mainWindowHandler);
                }
        }


    }

}
