package com.cybozu.labs.langdetect;

/*
 * Copyright (C) 2010-2014 Cybozu Labs, 2016 Konstantin Gusarov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.cybozu.labs.langdetect.util.LangProfile;
import com.cybozu.labs.langdetect.util.NGram;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.kgusarov.textprocessing.langdetect.LangProfileDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

/**
 * <p>Language Detector Factory Class</p>
 * <p>This class manages an initialization and constructions of {@link Detector}.</p>
 * <p>When the language detection,
 * construct Detector instance via {@link DetectorFactory#create()}.
 * See also {@link Detector}'s sample code.</p>
 * <ul>
 *  <li>4x faster improvement based on Elmer Garduno's code. Thanks!</li>
 * </ul>
 *
 * @author Nakatani Shuyo
 * @author Konstantin Gusarov
 * @see Detector
 */
@SuppressWarnings("unchecked")
public class DetectorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DetectorFactory.class);

    final Map<String, double[]> languageProbabilityMap = Maps.newHashMap();
    final List<String> languages = Lists.newArrayList();

    /**
     * Create new {@code DetectorFactory}
     *
     * @throws LangDetectException      In case language profiles weren't read for some reason
     */
    public DetectorFactory() {
        List<String> resources = List.of(
                "sm/ar.json",
                "sm/bg.json",
                "sm/bn.json",
                "sm/ca.json",
                "sm/cs.json",
                "sm/da.json",
                "sm/de.json",
                "sm/el.json",
                "sm/en.json",
                "sm/es.json",
                "sm/et.json",
                "sm/fa.json",
                "sm/fi.json",
                "sm/fr.json",
                "sm/gu.json",
                "sm/he.json",
                "sm/hi.json",
                "sm/hr.json",
                "sm/hu.json",
                "sm/id.json",
                "sm/it.json",
                "sm/ja.json",
                "sm/ko.json",
                "sm/lt.json",
                "sm/lv.json",
                "sm/mk.json",
                "sm/ml.json",
                "sm/nl.json",
                "sm/no.json",
                "sm/pa.json",
                "sm/pl.json",
                "sm/pt.json",
                "sm/ro.json",
                "sm/ru.json",
                "sm/si.json",
                "sm/sq.json",
                "sm/sv.json",
                "sm/ta.json",
                "sm/te.json",
                "sm/th.json",
                "sm/tl.json",
                "sm/tr.json",
                "sm/uk.json",
                "sm/ur.json",
                "sm/vi.json",
                "sm/zh-cn.json",
                "sm/zh-tw.json");

        final int languageCount = resources.size();
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final ObjectMapper mapper = new ObjectMapper();

        for (int i = 0; i < languageCount; i++) {
            final String profile = resources.get(i);

            try (final InputStream is = cl.getResourceAsStream(profile)) {
                final LangProfileDocument lpd = mapper.readValue(is, LangProfileDocument.class);
                final LangProfile langProfile = new LangProfile(lpd);
                addProfile(langProfile, i, languageCount);
            } catch (final IOException e) {
                throw new LangDetectException(ErrorCode.FAILED_TO_INITIALIZE, "Failed to read language profile", e);
            }
        }
    }

    /**
     * Merge information from language profile instance into this factory
     *
     * @param profile                   Language profile to be merged
     * @param languageCount             Total amount of language profiles
     * @param index                     Index of language profile being added
     * @throws LangDetectException      In case language profile is already defined or contains invalid N-Grams
     */
    @VisibleForTesting
    void addProfile(final LangProfile profile, final int index, final int languageCount) {
        final String language = profile.getName();
        if (languages.contains(language)) {
            throw new LangDetectException(ErrorCode.DUPLICATE_LANGUAGE, language + " language profile is already defined");
        }

        languages.add(language);
        final Map<String, Integer> frequencies = profile.getFrequencies();

        for (final Map.Entry<String, Integer> entry : frequencies.entrySet()) {
            final String ngram = entry.getKey();

            if (!languageProbabilityMap.containsKey(ngram)) {
                languageProbabilityMap.put(ngram, new double[languageCount]);
            }

            final int length = ngram.length();
            final int[] nGramCount = profile.getNGramCount();
            if ((length >= 1) && (length <= NGram.MAX_NGRAM_LENGTH)) {
                final Double count = entry.getValue().doubleValue();
                final double probability = count / nGramCount[length - 1];

                languageProbabilityMap.get(ngram)[index] = probability;
            } else {
                LOGGER.warn("Invalid n-gram in language profile: {}", ngram);
            }
        }
    }

    /**
     * Construct Detector instance
     *
     * @return                              Detector instance
     * @throws LangDetectException          In case factory contains no language profiles
     */
    public Detector create() {
        return createDetector();
    }

    /**
     * Construct Detector instance with smoothing parameter
     *
     * @param alpha                     Smoothing parameter (default value = 0.5)
     * @return                          Detector instance
     * @throws LangDetectException      In case factory contains no language profiles
     */
    public Detector create(final double alpha) {
        final Detector detector = createDetector();
        detector.setAlpha(alpha);
        return detector;
    }

    private Detector createDetector() {
        if (languages.isEmpty()) {
            throw new LangDetectException(ErrorCode.PROFILE_NOT_LOADED, "No language profile classes found");
        }

        return new Detector(this);
    }

    public List<String> getLangList() {
        return unmodifiableList(languages);
    }
}
