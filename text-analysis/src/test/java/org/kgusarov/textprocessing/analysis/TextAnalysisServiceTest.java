package org.kgusarov.textprocessing.analysis;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TextAnalysisServiceTest {
    private static final String TEXT_TO_ANALYSE = "" +
            "@k\u200Cgusarov: \u200CТ73 на ул.тербатас 73 - очень уютное место где легко совмещаются высокий сервис и вкусная еда. " +
            "Обслуж\u200Cивание на\u200C 5 звезд, официанты очень доброжелательны и всегда с уважением обращаются к гостю, " +
            "свободно демонстрируя знания блюд и напитков. Еду жд\u200Cать долго не приходиться, буквально " +
            "через 10-15 мин.прин\u200Cосят горячее, красиво украшеное и неимоверно вкусное блюдо. Домашний омлет " +
            "фритатта, который можно вариировать по желанию, наполненный разными овощами,мясом, сыром дает " +
            "чуство сытости на весь день. Сытное блюдо дополняет только что испеченный дессерт чиз кейк " +
            "inte\u200Crnational с ягодками граната и клубничкой,как заключение приятного время провидения в " +
            "изысканном и в то же время уютно-домашнем ресторане в центре риге. Всем советую посетить!!!\n" +
            "\nhttp://www.lursoft.lv/address/riga-terbatas-iela-73-lv-1001" +
            "\ngoogle.com" +
            //"\ngoogle.lv" +
            //"\nwhatever.lv" +
            //"\nwhatever.lt" +
            "\n$also $some $cash" +
            "\n#Рига #Центр #Ch\u200Ceesecake #Омлет ";

    private static final List<String> TRANSLITERATED_SUBSTRINGS = Lists.newArrayList("kgusarov", "min", "international",
            "riga", "cheesecake");

    @Test
    public void testAnalyse() throws Exception {
        final TransliterationService ts = TransliterationServiceFactory.create();
        final TermExtractionService tes = new TermExtractionService();
        final TextCleanupService tcs = new TextCleanupService();

        final TextAnalysisService service = new TextAnalysisService(tes, tcs, ts);
        final AnalysedText analysedText = service.analyse(TEXT_TO_ANALYSE, true);

        final List<AnalysedEntity> cashtags = analysedText.getCashtags();
        final List<AnalysedEntity> hashtags = analysedText.getHashtags();
        final List<AnalysedEntity> mentions = analysedText.getMentions();
        final List<AnalysedEntity> urls = analysedText.getUrls();

        final List<String> oUrls = toOriginal(urls);
        final List<String> tUrls = toTransliterated(urls);

        assertThat(oUrls, containsInAnyOrder("http://www.lursoft.lv/address/riga-terbatas-iela-73-lv-1001",
                /*"google.lv",*/ "google.com"/*, "whatever.lv", "whatever.lt"*/));
        assertThat(tUrls, iterableWithSize(2));
        tUrls.forEach(Assert::assertNull);

        final List<String> oCashtags = toOriginal(cashtags);
        final List<String> tCashtags = toTransliterated(cashtags);

        final List<String> oHashtags = toOriginal(hashtags);
        final List<String> tHashtags = toTransliterated(hashtags);

        final List<String> oMentions = toOriginal(mentions);
        final List<String> tMentions = toTransliterated(mentions);

        assertThat(oCashtags, containsInAnyOrder("also", "some", "cash"));
        assertThat(tCashtags, containsInAnyOrder("also", "some", "cash"));

        assertThat(oHashtags, containsInAnyOrder("рига", "центр", "cheesecake", "омлет"));
        assertThat(tHashtags, containsInAnyOrder("riga", "centr", "cheesecake", "omlet"));

        assertThat(oMentions, containsInAnyOrder("kgusarov"));
        assertThat(tMentions, containsInAnyOrder("kgusarov"));

        final List<String> originalTerms = analysedText.getOriginalTerms();
        final List<String> transliteratedTerms = analysedText.getTransliteratedTerms();

        assertThat(originalTerms, hasItems("т73", "сервис", "international", "тербатас"));
        assertThat(transliteratedTerms, hasItems("t73", "servis", "international", "terbatas"));

        final String originalText = analysedText.getOriginalText();
        final String transliteratedText = analysedText.getTransliteratedText();

        final String clean = tcs.removeDirectionAndInvisibleChars(TEXT_TO_ANALYSE);
        assertEquals(clean, originalText);

        assertThat(transliteratedText, stringContainsInOrder(TRANSLITERATED_SUBSTRINGS));

        ts.shutdown();
    }

    private List<String> toOriginal(final List<AnalysedEntity> analysed) {
        return analysed.stream()
                .map(AnalysedEntity::getOriginalValue)
                .collect(Collectors.toList());
    }

    private List<String> toTransliterated(final List<AnalysedEntity> analysed) {
        return analysed.stream()
                .map(AnalysedEntity::getTransliteratedValue)
                .collect(Collectors.toList());
    }
}