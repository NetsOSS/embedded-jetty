package eu.nets.utils.jetty.embedded;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import java.util.HashMap;

/**
 * Log4j-appender som gjør at loggingen kan tilpasses Nimbus ved:
 * - hindring av meldingsstormer (cooldown og max occurrences)
 * - formatering av stack trace og multilinje meldinger til en linje
 * <p/>
 * Baserer seg på den innebygde DailyRollingFileAppender.
 * <p/>
 * <p/>
 * <pre>
 * #Eksempel på log4j.properties
 * log4j.appender.DAILY_APPENDER=no.bbs.shared.server.log4j.NimbusFriendlyFileAppender
 * log4j.appender.DAILY_APPENDER.File=logs/jettylog.log
 * log4j.appender.DAILY_APPENDER.DatePattern='.'yyyy-MM-dd
 *
 * # Pattern som brukes for ting som ikke skal fanges opp av Nimbus
 * log4j.appender.DAILY_APPENDER.DefaultPattern=%d %p [%c : %t] - %m%n
 *
 * # Pattern som inneholder en tag som Nimbus sniffer etter
 * log4j.appender.DAILY_APPENDER.NimbusPattern=%d %p [%c : %t] ::NIMBUS:: %m%n
 *
 * # Maks meldinger av samme kategori fra samme tråd som kan tagges for Nimbus
 * log4j.appender.DAILY_APPENDER.MaxOccurences=10
 *
 * # Tid (i sekunder) der det ikke kan tagges flere meldinger av samme type til Nimbus, når max occurrences er oppnådd.
 * log4j.appender.DAILY_APPENDER.MessageCooldown=600
 *
 * # Minimum meldingsnivå for at Nimbustagging skal utføres.
 * log4j.appender.DAILY_APPENDER.NimbusMinLevel=INFO
 *
 * # Angir om man skal bruke tråd+level som identifikator for "like" meldinger, eller bare level.
 * # Kjekk for applikasjoner som oppretter nye tråder med random navn.
 * log4j.appender.separateOnThread=true
 * </pre>
 *
 * @author extjry
 */
public class NimbusFriendlyFileAppender extends DailyRollingFileAppender {

    public static final String REVISION_ID = "$Revision$";

    protected String nimbusMinLevel = "WARN";
    protected int maxOccurences = 10;
    protected int messageCooldown = 120;
    protected boolean separateOnThread = true;

    protected String defaultPattern = "%d %p [%c : %t] - %m%n";
    protected String nimbusPattern = "%d %p [%c : %t] ::NIMBUS:: %m%n";

    private HashMap<String, Integer> messageCount = new HashMap<String, Integer>();
    private HashMap<String, Long> lastMessage = new HashMap<String, Long>();

    private NimbusFriendlyPatternLayout friendlyLayout;

    @Override
    public void append(LoggingEvent event) {

        if (event.getMessage() == null && event.getThrowableStrRep() == null) {
            return;
        }

        if (friendlyLayout == null) {
            friendlyLayout =
                    new NimbusFriendlyPatternLayout(defaultPattern, nimbusPattern);
            super.setLayout(friendlyLayout);
        }

        Level nimbusMin = Level.toLevel(nimbusMinLevel);
        Level eventLevel = event.getLevel();

        if (eventLevel.toInt() >= nimbusMin.toInt()) {

            String messageKey = (separateOnThread ? event.getThreadName() : "") + eventLevel;
            Integer currentCount = messageCount.containsKey(messageKey)
                    ? messageCount.get(messageKey) : 1;

            Long lastIncoming = lastMessage.containsKey(messageKey)
                    ? lastMessage.get(messageKey) : 0L;


            if (!isWithinCooldown(lastIncoming)
                    || (isWithinCooldown(lastIncoming) && currentCount < maxOccurences)) {

                if (isWithinCooldown(lastIncoming) && currentCount < maxOccurences) {
                    messageCount.put(messageKey, currentCount + 1);
                } else if (!isWithinCooldown(lastIncoming)) {
                    lastMessage.put(messageKey, System.currentTimeMillis());
                    messageCount.put(messageKey, 1);
                }

                friendlyLayout.setTagForNimbus(true);
            }
        }

        super.append(event);
        friendlyLayout.setTagForNimbus(false);
    }


    @Override
    public boolean requiresLayout() {
        return false;
    }


    protected boolean isWithinCooldown(long last) {
        return (System.currentTimeMillis() - last) < (messageCooldown * 1000);
    }


    public void setMaxOccurences(int maxOccurences) {
        this.maxOccurences = maxOccurences;
    }

    public void setMessageCooldown(int messageCooldown) {
        this.messageCooldown = messageCooldown;
    }

    public void setNimbusMinLevel(String nimbusMinLevel) {
        this.nimbusMinLevel = nimbusMinLevel;
    }

    public void setDefaultPattern(String defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public void setNimbusPattern(String nimbusPattern) {
        this.nimbusPattern = nimbusPattern;
    }

    public void setSeparateOnThread(boolean separateOnThread) {
        this.separateOnThread = separateOnThread;
    }
}


/**
 * @author extjry
 */
class NimbusFriendlyPatternLayout extends PatternLayout {

    private boolean tagForNimbus = false;
    private String defaultPattern;
    private String nimbusPattern;

    public NimbusFriendlyPatternLayout(String defaultPattern, String nimbusPattern) {
        super(defaultPattern);

        this.defaultPattern = defaultPattern;
        this.nimbusPattern = nimbusPattern;
    }

    @Override
    public synchronized String format(LoggingEvent event) {

        if (tagForNimbus) {
            super.setConversionPattern(nimbusPattern);
            String nimbusMessage = super.format(event);
            String finalMessage = "";

            final String SEP = System.getProperty("line.separator", "\n");
            int lastNewLine = nimbusMessage.lastIndexOf(SEP);
            if ((lastNewLine != -1 && lastNewLine != nimbusMessage.indexOf(SEP)) || event.getThrowableStrRep() != null) {

                super.setConversionPattern(defaultPattern);
                String defaultMessage = super.format(event);

                if (lastNewLine != -1 && lastNewLine != nimbusMessage.indexOf(SEP)) {
                    String[] lines = nimbusMessage.split(SEP);
                    for (int i = 0; i < lines.length && i <= 10; i++) {
                        finalMessage += (i != 0 ? " [" + i + "] " : "") + lines[i];
                    }
                }

                if (event.getThrowableStrRep() != null) {
                    String[] trace = event.getThrowableStrRep();
                    String stackTrace = "";
                    if (trace.length > 0) {

                        stackTrace += "Stack Trace: " + prepareForRegexp(trace[0]);
                    }

                    if (trace.length > 1) {
                        stackTrace += prepareForRegexp(trace[1]);
                    }

                    String cleanNimbusMessage = nimbusMessage != null ? nimbusMessage.replaceAll(SEP, ", ") : "";

                    finalMessage += cleanNimbusMessage + stackTrace;
                }

                finalMessage += SEP + defaultMessage;
            } else {
                finalMessage = nimbusMessage;
            }

            return finalMessage;
        }

        super.setConversionPattern(defaultPattern);
        return super.format(event);
    }

    private String prepareForRegexp(String str) {
        return str != null ? str.replaceAll("$", "") : "";
    }


    public void setTagForNimbus(boolean tagForNimbus) {
        this.tagForNimbus = tagForNimbus;
    }
}
