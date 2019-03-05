import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.mitalex.kurs.Config;
import org.mitalex.kurs.CryptoUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MainTest {

    @Test
    public void testJava8Streams() throws IOException {
        Random random = new Random();
        System.out.println(random.nextInt(100));
        List<Integer> l = IntStream.range(1,11).mapToObj(e->e).collect(Collectors.toList());
        System.out.println(l);
        l = Stream.generate(()->random.nextInt(100)).limit(15).collect(Collectors.toList());
        System.out.println("oh");
        System.out.println(l);
    }

    @Test
    public void testURLDownload() throws IOException {
        System.out.println("ok");
        HttpURLConnection conn = (HttpURLConnection) new URL("https://ya.ru").openConnection();
        InputStream ins = conn.getInputStream();
        BufferedInputStream bins = new BufferedInputStream(ins);
        FileOutputStream fout = new FileOutputStream("t.htm");
        int res = bins.read();
        while ( -1 != res){
            fout.write(res);
            res = bins.read();
        }
        bins.close();
        fout.close();
    }

    @Test
    public void  encryptConfig() throws IOException, GeneralSecurityException {
        String password = "123";
        String encryptPassword = CryptoUtils.encrypt(password, new File(Config.ConfigKeys.SECRET_FILE));

        List<String> lines = Files.readAllLines(Paths.get("config.properties"), StandardCharsets.UTF_8);
        for (int i =0; i < lines.size(); i++)
        {
            String s = lines.get(i);
            if( s.trim().matches(Config.ConfigKeys.PASSWORD+ "\\s+\\=.*") ){
                s = s.replaceAll("(.*)\\=(.*)", "$1={0aes}" + encryptPassword);
                lines.set(i,s);
            }
        }

        File testPropertiesFiles = new File("test.properties");
        Files.write( testPropertiesFiles.toPath(),lines);
        Properties testConfig = new Properties();
        testConfig.load( new FileInputStream(testPropertiesFiles) );
        // очищаем после теста
        testPropertiesFiles.delete();

        // тестируем результат
        String testEncryptedPassword = testConfig.getProperty(Config.ConfigKeys.PASSWORD);
        Assert.assertThat(testEncryptedPassword, CoreMatchers.startsWith("{aes}"));
        Assert.assertTrue("Ожидался шифрованый пароль, начинающийся на {aes}", testEncryptedPassword.startsWith("{aes}") );

    }
}


