package firmproj.utility;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
public class FirmwareRelated {
    private static final String FIRM_RELATED_MATCHER = "(?i).*(Firmware|downloadFIrmware|FirmwareUpdate|needOta|CheckOta|firmwareResponse|needUpdate|firware|fireware|getdeviceinfo|latestfirmware|checkUpdate|fw_update|checklatest|fwurl|swversion|checkIsOTAUpdateNeeded|otaUpdateRequired|fwinfo|newfwinfo|deviceOTA|deviceupdate|checknewversion).*";
    private static final Pattern FIRM_RELATED_PATTERN = Pattern.compile(FIRM_RELATED_MATCHER);

    public static boolean isFirmRelated(String methodString){
        Matcher matcher = FIRM_RELATED_PATTERN.matcher(methodString);
        return matcher.find();
    }
}
