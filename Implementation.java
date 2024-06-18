import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class regex {

    public static void main(String[] args) {
        String inputFilePath = "src/main/resources/PatientNotes.txt";
        String anonymizedFilePath = "AnonymizedPatientNotes.txt";
        String mappingFilePath = "MappingDocument.txt";

        try {
            String content = new String(Files.readAllBytes(Paths.get(inputFilePath)), StandardCharsets.UTF_8);
            StringBuilder anonymizedContent = new StringBuilder();
            List<String> mappingLines = new ArrayList<>();
            AtomicInteger patientCounter = new AtomicInteger(1);

            Pattern startPattern = Pattern.compile("(Mr\\.|Ms\\.|Mrs\\.)");
            Matcher startMatcher = startPattern.matcher(content);

            List<Integer> starts = new ArrayList<>();
            while (startMatcher.find()) {
                starts.add(startMatcher.start());
            }
            starts.add(content.length());

            for (int i = 0; i < starts.size() - 1; i++) {
                String patientRecord = content.substring(starts.get(i), starts.get(i + 1));
                String anonymizedRecord = anonymizePatientRecord(patientRecord, patientCounter.getAndIncrement(), mappingLines);
                anonymizedContent.append(anonymizedRecord).append("\n\n");
            }

            Files.write(Paths.get(anonymizedFilePath), anonymizedContent.toString().getBytes(StandardCharsets.UTF_8));
            Files.write(Paths.get(mappingFilePath), String.join("\n", mappingLines).getBytes(StandardCharsets.UTF_8));

            System.out.println("Process Successful.");
        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String anonymizePatientRecord(String record, int patientNumber, List<String> mappingLines) {
        String detailRegex = "(Mr\\.|Ms\\.|Mrs\\.|Dr\\.)\\s+[A-Z][a-z]+([ -][A-Z][a-z]+)?|\\b\\d{2}-year-old\\b|\\d+\\s+[A-Z][a-z]+\\s[A-Z][a-z]+(?:,\\s+[A-Za-z]+,\\s+[A-Z]{2})?";
        Pattern pattern = Pattern.compile(detailRegex);
        Matcher matcher = pattern.matcher(record);
        AtomicInteger detailCounter = new AtomicInteger(1);
        List<String> namesAnonymized = new ArrayList<>(); 
        String firstName = null; 

        while (matcher.find()) {
            String originalDetail = matcher.group();
            String replacementTag;
            
            if (originalDetail.matches("(Mr\\.|Ms\\.|Mrs\\.|Dr\\.)\\s+[A-Z][a-z]+([ -][A-Z][a-z]+)?")) {
                if (namesAnonymized.contains(originalDetail)) {
                    replacementTag = patientNumber + ".4";
                } else {
                    replacementTag = patientNumber + "." + detailCounter.getAndIncrement();
                    namesAnonymized.add(originalDetail); 
                    if (firstName == null) {
                        firstName = originalDetail.split(" ")[1]; 
                    }
                }
            } else {
                replacementTag = patientNumber + "." + detailCounter.getAndIncrement();
            }
            
            record = record.replaceFirst(Pattern.quote(originalDetail), replacementTag);
            
            mappingLines.add(replacementTag + " " + originalDetail);
        }
        
        if (firstName != null) {
            record = record.replaceAll("\\b" + firstName + "\\b", patientNumber + ".4");

            if (!mappingLines.contains(patientNumber + ".4 " + firstName)) {
                mappingLines.add(patientNumber + ".4 " + firstName);
            }
        }

        return record;
    }


}