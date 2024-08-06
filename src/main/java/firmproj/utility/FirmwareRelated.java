package firmproj.utility;

import soot.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
public class FirmwareRelated {
    private static final String FIRM_RELATED_MATCHER = "(?i).*(Firmware|downloadFIrmware|FirmwareUpdate|needOta|CheckOta|firmwareResponse|needUpdate|firware|fireware|getdeviceinfo|latestfirmware|checkUpdate|fw_update|checklatest|fwurl|swversion|checkIsOTAUpdateNeeded|otaUpdateRequired|fwinfo|newfwinfo|deviceOTA|deviceupdate|checknew|update|version|upgrade|getIP|domain|getServer|Region).*";
    private static final Pattern FIRM_RELATED_PATTERN = Pattern.compile(FIRM_RELATED_MATCHER);

    public static boolean isFirmRelated(String methodString){
        Matcher matcher = FIRM_RELATED_PATTERN.matcher(methodString);
        return matcher.find();
    }

    public static HashSet<String> matchFields(String input) {
        HashSet<String> fields = new HashSet<>();
        String regex = "<([a-zA-Z0-9._$]+):\\s+([a-zA-Z0-9._$]+)\\s+([a-zA-Z_][a-zA-Z0-9_]*)>";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            // 将匹配的结果添加到列表中
            fields.add(matcher.group());
        }

        return fields;  // 如果没有匹配到，返回空的列表
    }

}
