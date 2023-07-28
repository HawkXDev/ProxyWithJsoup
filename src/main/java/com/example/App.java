package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;


public class App {

    public static void main(String[] args) {
        String url = "https://yandex.ru/internet";
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/115.0.0.0 Safari/537.36";

        try (InputStream input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);

            String proxyHost = prop.getProperty("proxyHost");
            int proxyPort = Integer.parseInt(prop.getProperty("proxyPort"));
            String proxyUser = prop.getProperty("proxyUser");
            String proxyPassword = prop.getProperty("proxyPassword");

            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                }
            });

            // Включаем поддержку аутентификации для туннелирования и прокси
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "false");
            System.setProperty("jdk.http.auth.proxying.disabledSchemes", "false");

            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));

            // Вызов метода для обработки с использованием Jsoup
            jsoupProcess(url, userAgent, proxyUser, proxyPassword, proxy);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void jsoupProcess(String url, String userAgent,
                                     String proxyUser, String proxyPassword, Proxy proxy) {
        try {
            Document doc = Jsoup.connect(url)
                    .proxy(proxy)
                    .header("Proxy-Authorization", getProxyAuthHeader(proxyUser, proxyPassword))
                    .userAgent(userAgent)
                    .get();

            // Получаем значения параметров
            String ipAddress = getParentSiblingTextOrNull(doc, "h3:contains(IPv4-адрес)");
            String ipv6Address = getParentSiblingTextOrNull(doc, "h3:contains(IPv6-адрес)");
            String browser = getParentSiblingTextOrNull(doc, "h3:contains(Браузер)");
            String screenResolution = getParentSiblingTextOrNull(doc, "h3:contains(Разрешение экрана)");
            String region = getParentSiblingSelectTextOrNull(doc,
                    "h3:contains(Регион)", ".location-renderer__value");

            // Выводим значения параметров
            System.out.println("ДАННЫЕ О ПОЛЬЗОВАТЕЛЕ");
            System.out.println("IPv4-адрес: " + ipAddress);
            System.out.println("IPv6-адрес: " + ipv6Address);
            System.out.println("Браузер: " + browser);
            System.out.println("Разрешение экрана: " + screenResolution);
            System.out.println("Регион: " + region);

            // Получаем техническую информацию
            System.out.println("\nТЕХНИЧЕСКАЯ ИНФОРМАЦИЯ");
            Element divTechInfo = doc.select(".list-info__content").first();

            if (Objects.nonNull(divTechInfo)) {
                Elements elements = divTechInfo.select(".list-info__item");

                // Выводим техническую информацию
                for (Element item : elements) {
                    String name = getTextOrNull(item, ".list-info__name");
                    String value = getTextOrNull(item, ".list-info__renderer");
                    System.out.println(name + " " + value);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getProxyAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
    }

    private static String getTextOrNull(Element element, String cssQuery) {
        String text = null;
        Element first = element.select(cssQuery).first();
        if (Objects.nonNull(first)) {
            text = first.text().trim();
        }
        return text;
    }

    private static String getParentSiblingSelectTextOrNull(Document doc, String cssQuery1, String cssQuery2) {
        String text = null;
        Element element = doc.select(cssQuery1).first();
        if (Objects.nonNull(element)) {
            Element parent = element.parent();
            if (Objects.nonNull(parent)) {
                Element select = parent.nextElementSibling();
                if (Objects.nonNull(select)) {
                    text = select.select(cssQuery2).text();
                }
            }
        }
        return text;
    }

    private static String getParentSiblingTextOrNull(Document doc, String cssQuery) {
        String text = null;
        Element element = doc.select(cssQuery).first();
        if (Objects.nonNull(element)) {
            Element parent = element.parent();
            if (Objects.nonNull(parent)) {
                Element sibling = parent.nextElementSibling();
                if (Objects.nonNull(sibling)) {
                    text = sibling.text().trim();
                }
            }
        }
        return text;
    }
}
