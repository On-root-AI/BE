package com.OnRoot.onroot.global.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ExamCodeParser {

    public record ExamInfo(String jmCd, String jmfldnm) {}

    public Map<String, List<ExamInfo>> loadBySeriesName() {
        Map<String, List<ExamInfo>> result = new HashMap<>();
        try {
            ClassPathResource resource = new ClassPathResource("exam-codes.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(resource.getInputStream());

            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String jmCd = getText(item, "jmcd");
                String jmfldnm = getText(item, "jmfldnm");
                String seriesnm = getText(item, "seriesnm");
                if (!jmCd.isEmpty() && !jmfldnm.isEmpty() && !seriesnm.isEmpty()) {
                    result.computeIfAbsent(seriesnm, k -> new ArrayList<>())
                          .add(new ExamInfo(jmCd, jmfldnm));
                }
            }
            log.info("시험 종목 코드 로드: 계열 {}개, 총 {}개 종목",
                    result.size(), result.values().stream().mapToInt(List::size).sum());
        } catch (Exception e) {
            log.error("시험 종목 코드 로드 실패", e);
        }
        return result;
    }

    private String getText(Element el, String tag) {
        NodeList list = el.getElementsByTagName(tag);
        return list.getLength() > 0 ? list.item(0).getTextContent().trim() : "";
    }
}
