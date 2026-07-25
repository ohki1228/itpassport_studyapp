package com.itpassport.app.material;

import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * 起動時にclasspath上のExcel教材データをDBへ取り込む(要件: 起動時/バッチで自動読み込み)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialImportRunner implements ApplicationRunner {

    private static final String MATERIAL_RESOURCE = "classpath:data/study-materials.xlsx";

    private final MaterialImportService importService;
    private final ResourceLoader resourceLoader;

    @Override
    public void run(ApplicationArguments args) {
        Resource resource = resourceLoader.getResource(MATERIAL_RESOURCE);
        if (!resource.exists()) {
            log.warn("教材Excel({})が見つからないため、取込みをスキップします。", MATERIAL_RESOURCE);
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            ImportResult result = importService.importFrom(inputStream);
            log.info("教材データ取込み完了: genres={}, knowledge={}, questions={}, terms={}, errors={}",
                    result.genresImported(), result.knowledgeImported(), result.questionsImported(),
                    result.termsImported(), result.errors().size());
            result.errors().forEach(log::warn);
        } catch (IOException e) {
            log.error("教材Excelの読み込みに失敗しました。", e);
        }
    }
}
