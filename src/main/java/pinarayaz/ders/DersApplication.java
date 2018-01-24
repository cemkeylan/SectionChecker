package pinarayaz.ders;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application to check section availability from Offerings
 * Created by PINAR on 24.1.2018.
 */
public class DersApplication {
    private static final List<String> VALID_COURSES = Arrays.asList("CS 202", "CS 224");

    public static void main(String args[]) throws IOException, InterruptedException, TelegramApiRequestException {
        ApiContextInitializer.init();

        NotificationSender sender = new NotificationSender(args[0]);
        Map<String, Section> sectionMap = new HashMap<String, Section>();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        telegramBotsApi.registerBot(sender);

        System.out.println("10 sec pause for bot registrations");
        Thread.sleep(10000L);

        while (true) {

            Document doc = Jsoup.connect("https://stars.bilkent.edu.tr/homepage/ajax/plainOfferings.php?" +
                    "COURSE_CODE=CS&" +
                    "SEMESTER=20172&" +
                    "submit=List%20Selected%20Offerings&" +
                    "rndval=" + System.currentTimeMillis())
                    .get();

            for (Element element : doc.select("tbody").select("tr")) {
                Elements divisionElements = element.select("td");
                if (divisionElements.get(12).html().equalsIgnoreCase("unlimited")) {
                    continue;
                }
                String courseId = divisionElements.get(0).html();
                if (sectionMap.containsKey(courseId)) {
                    sectionMap.get(courseId).setQuota(Integer.valueOf(divisionElements.get(13).html()));
                } else {
                    sectionMap.put(courseId, new Section(courseId, Integer.valueOf(divisionElements.get(13).html())));
                }
            }

            for (Map.Entry<String, Section> entry : sectionMap.entrySet()) {
                for (String course : VALID_COURSES) {
                    Section s = entry.getValue();
                    if (s.getCourseId().startsWith(course)) {
                        System.out.println(s.toString());
                        if (s.getQuota() != 0 && s.getQuotaPrev() == 0) {
                            sender.sendNotification("quota for " + s.getCourseId() + " is now " + s.getQuota());
                        }
                        s.setQuotaPrev(s.getQuota());
                    }

                }

            }
            Thread.sleep(5000L);
        }
    }
}
