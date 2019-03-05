package org.mitalex.kurs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class Config {

    private final String configFilePath;

    public interface ConfigKeys {
        public static final String GECKO_DRIVER = "gecko_driver";

        String PROFILE_PATH = "profile_path";
        String DOWNLOAD_DIR = "download_dir";
        String URL = "url";
        String LOGIN = "login";
        String PASSWORD = "password";
        String LOGIN_PAGE_TITLE = "login_page_title";
        String LOGIN_CSS_SELECTOR = "login_css_selector";
        String PASSWORD_CSS_SELECTOR = "password_css_selector";
        String LOGIN_BUTTON_CSS_SELECTOR = "login_button_css_selector";
        String IS_DEBUG = "is_debug";
        String CSS_SELECTOR = "css_selector_element";
        String CSS_SELECTOR_ATTRIBUTE_URL = "css_selector_attribute_url";
        String CSS_SELECTOR_ATTRIBUTE_FILENAME = "css_selector_attribute_filename";
        String SECRET_FILE = "secret.key";
        String LIMIT_DOWNLOADS = "limit_downloads";
    }

    static final Logger LOG = LoggerFactory.getLogger(Config.class);

    private Properties config = new Properties();

    public Config(String configPath) {
        try {
            configFilePath = configPath;
            config.load(new FileInputStream(configPath));
            Enumeration<?> keys = config.propertyNames();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value = (String) config.get(key);
                value = cleanBadCharacters(value);
                config.setProperty(key, value);
            }
            doCrypt();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        LOG.info(config.toString());
        System.out.println("ok");
    }

    private void doCrypt() {
        String password = config.getProperty(ConfigKeys.PASSWORD);
        if (password.startsWith("{aes}")) {
            password = CryptoUtils.decrypt(password, new File(ConfigKeys.SECRET_FILE));
            config.setProperty(ConfigKeys.PASSWORD, password);
        } else {
            try {
                String encryptPassword = CryptoUtils.encrypt(password, new File(ConfigKeys.SECRET_FILE));
                List<String> lines = Files.readAllLines(Paths.get(configFilePath), StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String s = lines.get(i);
                    if (s.trim().matches(Config.ConfigKeys.PASSWORD + "\\s+\\=.*")) {
                        s = s.replaceAll("(.*)\\=(.*)", "$1={aes}" + encryptPassword);
                        lines.set(i, s);
                    }
                }
                Files.write(Paths.get(configFilePath), lines);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }

        }
    }

    /**
     * @param value
     * @return
     */
    private String cleanBadCharacters(String value) {
        if (value.matches(".*[\'\"].*")) {
            value = value.replaceAll("\"", "").replaceAll("\'", "");
        }
        return value;
    }

    public String get(String key) {
        return config.getProperty(key);
    }

    public String getDownloadDir() {
        return config.getProperty(ConfigKeys.DOWNLOAD_DIR);
    }
}
